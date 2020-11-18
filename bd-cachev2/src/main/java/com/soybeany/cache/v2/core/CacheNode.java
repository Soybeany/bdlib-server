package com.soybeany.cache.v2.core;

import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.contract.IKeyConverter;
import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.exception.NoDataSourceException;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataFrom;
import com.soybeany.cache.v2.model.DataPack;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 链式设计中的节点，若需访问下一节点，需加锁
 *
 * @author Soybeany
 * @date 2020/1/20
 */
class CacheNode<Param, Data> {

    private final Map<String, Lock> mKeyMap = new WeakHashMap<>();

    private final ICacheStrategy<Param, Data> mCurStrategy;
    private final IKeyConverter<Param> mConverter;

    private CacheNode<Param, Data> mNextNode;

    private final Map<Lock, DataPack<Data>> mTmpDataPackMap = new ConcurrentHashMap<>();

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

    /**
     * 获取数据并自动缓存
     */
    DataPack<Data> getDataPackAndAutoCache(DataContext<Param> context, final IDatasource<Param, Data> datasource) throws DataException {
        return getDataFromCurNode(context, mConverter.getKey(context.param), (context2, key) -> getDataFromTmpMapOrNextNode(key,
                () -> getDataFromNextNode(context2, key, datasource)
        ));
    }

    /**
     * 仅仅获取缓存
     */
    DataPack<Data> getCache(DataContext<Param> context) throws DataException {
        return getDataFromCurNode(context, mConverter.getKey(context.param), (context2, key) -> getDataFromTmpMapOrNextNode(key,
                () -> getCacheFromNextNode(context2, key)
        ));
    }

    void cacheData(DataContext<Param> context, final DataPack<Data> data) {
        traverse(context, (key, node) -> node.mCurStrategy.onCacheData(context, key, data));
    }

    void cacheException(DataContext<Param> context, Exception e) {
        final DataException exception = new DataException(DataFrom.SOURCE, e);
        traverse(context, (key, node) -> {
            try {
                node.mCurStrategy.onHandleException(context, key, exception);
            } catch (DataException dataException) {
                // 不作处理
            }
        });
    }

    void removeCache(DataContext<Param> context) {
        traverse(context, (key, node) -> node.mCurStrategy.removeCache(context, key));
    }

    void clearCache(String dataDesc) {
        traverse(new DataContext<>(dataDesc, null, null), (key, node) -> node.mCurStrategy.clearCache(dataDesc));
    }

    // ****************************************内部方法****************************************

    private void traverse(DataContext<Param> context, ICallback2<Param, Data> callback) {
        CacheNode<Param, Data> node = this;
        while (null != node) {
            String key = node.mConverter.getKey(context.param);
            synchronized (node.getDefaultLock()) {
                callback.onInvoke(key, node);
            }
            node = node.mNextNode;
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private DataPack<Data> getDataFromTmpMapOrNextNode(String key, ICallback3<Data> callback) throws DataException {
        // 加锁，避免并发时数据重复获取
        Lock lock = getLock(key);
        lock.countOfGet.incrementAndGet();
        synchronized (lock) {
            try {
                DataPack<Data> dataPack = mTmpDataPackMap.get(lock);
                // 若临时数据缓存没有时，再访问下一节点，以免并发时多次访问下一级节点(double check机制)
                if (null == dataPack) {
                    dataPack = callback.onGetDataFromNextNode();
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

    private DataPack<Data> getCacheFromNextNode(DataContext<Param> context, String key) throws DataException {
        // 若已无下一节点，抛出异常
        if (null == mNextNode) {
            throw new DataException(DataFrom.CACHE, new NoCacheException());
        }
        // 否则从下一节点获取缓存
        DataPack<Data> pack = mNextNode.getCache(context);
        synchronized (getDefaultLock()) {
            mCurStrategy.onCacheData(context, key, pack);
        }
        return pack;
    }

    /**
     * 从下一节点获取数据
     */
    private DataPack<Data> getDataFromNextNode(DataContext<Param> context, String key, IDatasource<Param, Data> datasource) throws DataException {
        try {
            DataPack<Data> pack;
            // 若没有下一节点，则从数据源获取
            if (null == mNextNode) {
                pack = getDataDirectly(context.param, datasource);
            }
            // 否则从下一节点获取缓存
            else {
                pack = mNextNode.getDataPackAndAutoCache(context, datasource);
            }
            synchronized (getDefaultLock()) {
                mCurStrategy.onCacheData(context, key, pack);
            }
            return pack;
        } catch (DataException e) {
            synchronized (getDefaultLock()) {
                return mCurStrategy.onHandleException(context, key, e);
            }
        }
    }

    /**
     * 从本地缓存服务获取数据
     */
    private DataPack<Data> getDataFromCurNode(DataContext<Param> context, String key, ICallback1<Param, Data> listener) throws DataException {
        try {
            return mCurStrategy.onGetCache(context, key);
        } catch (ICacheStrategy.NoCacheException e) {
            return listener.onNoCache(context, key);
        }
    }

    private Object getDefaultLock() {
        return mCurStrategy;
    }

    private Lock getLock(String key) {
        Lock lock = mKeyMap.get(key);
        if (null != lock) {
            return lock;
        }
        synchronized (mKeyMap) {
            lock = mKeyMap.get(key);
            if (null == lock) {
                mKeyMap.put(key, lock = new Lock());
            }
        }
        return lock;
    }

    // ****************************************内部类****************************************

    private interface ICallback1<Param, Data> {
        DataPack<Data> onNoCache(DataContext<Param> context, String key) throws DataException;
    }

    private interface ICallback2<Param, Data> {
        void onInvoke(String key, CacheNode<Param, Data> node);
    }

    private interface ICallback3<Data> {
        DataPack<Data> onGetDataFromNextNode() throws DataException;
    }

    private static class Lock {
        final AtomicInteger countOfGet = new AtomicInteger();

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

}
