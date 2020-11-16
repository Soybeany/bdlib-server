package com.soybeany.cache.v2.core;

import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.contract.IKeyConverter;
import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.exception.NoDataSourceException;
import com.soybeany.cache.v2.model.DataFrom;
import com.soybeany.cache.v2.model.DataPack;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Soybeany
 * @date 2020/1/20
 */
class CacheNode<Param, Data> {

    private final Map<String, Lock> mKeyMap = new WeakHashMap<String, Lock>();

    private final ICacheStrategy<Param, Data> mCurStrategy;
    private final IKeyConverter<Param> mConverter;

    private CacheNode<Param, Data> mNextNode;

    private final Map<Lock, DataPack<Data>> mTmpDataPackMap = new ConcurrentHashMap<Lock, DataPack<Data>>();

    static <Param, Data> DataPack<Data> getDataDirectly(Param param, IDatasource<Param, Data> datasource) throws DataException {
        try {
            if (null == datasource) {
                throw new NoDataSourceException();
            }
            return DataPack.newSourceDataPack(datasource, datasource.onGetData(param));
        } catch (Exception e) {
            throw new DataException(DataFrom.SOURCE, e);
        }
    }

    public CacheNode(ICacheStrategy<Param, Data> curStrategy, IKeyConverter<Param> converter) {
        if (null == curStrategy) {
            throw new RuntimeException("CacheService不能为null");
        }
        if (null == converter) {
            throw new RuntimeException("KeyConverter不能为null");
        }
        mCurStrategy = curStrategy;
        mConverter = converter;
    }

    ICacheStrategy<Param, Data> getStrategy() {
        return mCurStrategy;
    }

    /**
     * 设置下一个节点
     *
     * @param node 下一个节点
     */
    void setNextNode(CacheNode<Param, Data> node) {
        mNextNode = node;
    }

    DataPack<Data> getCache(Param param, final IDatasource<Param, Data> datasource) throws DataException {
        return getDataFromCurNode(param, mConverter.getKey(param), new ICallback1<Param, Data>() {
            @Override
            public DataPack<Data> onNoCache(Param param2, String key) throws DataException {
                return getDataFromTmpMapOrNextNode(datasource, param2, key);
            }
        });
    }

    void cacheData(final Param param, final DataPack<Data> data) {
        traverse(param, new ICallback2<Param, Data>() {
            @Override
            public void onInvoke(String key, CacheNode<Param, Data> node) {
                node.mCurStrategy.onCacheData(param, key, data);
            }
        });
    }

    void cacheException(final Param param, final Exception e) {
        traverse(param, new ICallback2<Param, Data>() {
            @Override
            public void onInvoke(String key, CacheNode<Param, Data> node) {
                node.mCurStrategy.onCacheException(param, key, e);
            }
        });
    }

    void removeCache(final Param param) {
        traverse(param, new ICallback2<Param, Data>() {
            @Override
            public void onInvoke(String key, CacheNode<Param, Data> node) {
                node.mCurStrategy.removeCache(param, key);
            }
        });
    }

    void clearCache() {
        traverse(null, new ICallback2<Param, Data>() {
            @Override
            public void onInvoke(String key, CacheNode<Param, Data> node) {
                node.mCurStrategy.clearCache();
            }
        });
    }

    // ****************************************内部方法****************************************

    private void traverse(Param param, ICallback2<Param, Data> callback) {
        CacheNode<Param, Data> node = this;
        while (null != node) {
            String key = node.mConverter.getKey(param);
            synchronized (getLock(key)) {
                callback.onInvoke(key, node);
            }
            node = node.mNextNode;
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private DataPack<Data> getDataFromTmpMapOrNextNode(IDatasource<Param, Data> datasource, Param param, String key) throws DataException {
        // 加锁，避免并发时数据重复获取
        Lock lock = getLock(key);
        lock.countOfGet.incrementAndGet();
        synchronized (lock) {
            try {
                DataPack<Data> dataPack = mTmpDataPackMap.get(lock);
                // 若临时数据缓存没有时，再访问下一节点，以免并发时多次访问下一级节点
                if (null == dataPack) {
                    dataPack = getDataFromNextNode(param, key, datasource);
                    mTmpDataPackMap.put(lock, DataPack.newTempCacheDataPack(dataPack));
                }
                return dataPack;
            } finally {
                int count = lock.countOfGet.decrementAndGet();
                if (0 == count) {
                    mTmpDataPackMap.remove(lock);
                }
            }
        }
    }

    /**
     * 从下一节点获取缓存
     */
    private DataPack<Data> getDataFromNextNode(Param param, String key, IDatasource<Param, Data> datasource) throws DataException {
        try {
            DataPack<Data> pack;
            // 若没有下一节点，则从数据源获取
            if (null == mNextNode) {
                pack = getDataDirectly(param, datasource);
            }
            // 否则从下一节点获取缓存
            else {
                pack = mNextNode.getCache(param, datasource);
            }
            mCurStrategy.onCacheData(param, key, pack);
            return pack;
        } catch (DataException e) {
            mCurStrategy.onCacheException(param, key, e.getOriginException());
            throw e;
        }
    }

    /**
     * 从本地缓存服务获取数据
     */
    private DataPack<Data> getDataFromCurNode(Param param, String key, ICallback1<Param, Data> listener) throws DataException {
        try {
            return mCurStrategy.onGetCache(param, key);
        } catch (ICacheStrategy.NoCacheException e) {
            return listener.onNoCache(param, key);
        }
    }

    private Lock getLock(String key) {
        Lock lock = mKeyMap.get(key);
        if (null == lock) {
            synchronized (this) {
                lock = mKeyMap.get(key);
                if (null == lock) {
                    mKeyMap.put(key, lock = new Lock());
                }
            }
        }
        return lock;
    }

    // ****************************************内部类****************************************

    private interface ICallback1<Param, Data> {
        DataPack<Data> onNoCache(Param param, String key) throws DataException;
    }

    private interface ICallback2<Param, Data> {
        void onInvoke(String key, CacheNode<Param, Data> node);
    }

    private static class Lock {
        final AtomicInteger countOfGet = new AtomicInteger();
    }

}
