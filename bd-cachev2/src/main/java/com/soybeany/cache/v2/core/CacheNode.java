package com.soybeany.cache.v2.core;

import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.contract.IKeyConverter;
import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.exception.NoDataSourceException;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataHolder;
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

    private CacheNode<Param, Data> mNextNode;

    private final PenetratorProtector<Data> mPenetratorProtector = new PenetratorProtector<>();

    public static <Param, Data> DataPack<Data> getDataDirectly(Object invoker, Param param, IDatasource<Param, Data> datasource) throws DataException {
        if (null == datasource) {
            throw new DataException(invoker, new NoDataSourceException());
        }
        try {
            return DataPack.newSourceDataPack(datasource, datasource.onGetData(param));
        } catch (Exception e) {
            throw new DataException(datasource, e);
        }
    }

    public CacheNode(ICacheStrategy<Param, Data> curStrategy) {
        if (null == curStrategy) {
            throw new RuntimeException("CacheService不能为null");
        }
        mCurStrategy = curStrategy;
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
        String key = getConverter().getKey(context.param);
        return getDataFromCurNode(context, key, () -> getDataFromPenetratorProtector(key, () -> {
            // 如果支持再次查询，则再次查询
            if (mCurStrategy.supportGetCacheBeforeAccessNextStrategy()) {
                try {
                    return mCurStrategy.onGetCacheBeforeAccessNextStrategy(context, key);
                } catch (NoCacheException ignore) {
                }
            }
            // 查询下一节点
            return getDataFromNextNode(context, key, datasource);
        }));
    }

    void cacheData(DataContext<Param> context, final DataPack<Data> data) {
        traverse(context.param, (key, node) -> node.mCurStrategy.onCacheData(context, key, data));
    }

    void cacheException(DataContext<Param> context, Exception e) {
        final DataException exception = new DataException(mCurStrategy, e);
        traverse(context.param, (key, node) -> {
            try {
                node.mCurStrategy.onHandleException(context, key, exception);
            } catch (DataException dataException) {
                // 不作处理
            }
        });
    }

    void removeCache(DataContext<Param> context) {
        traverse(context.param, (key, node) -> node.mCurStrategy.removeCache(context, key));
    }

    void clearCache(String dataDesc) {
        traverse(null, (key, node) -> node.mCurStrategy.clearCache(dataDesc));
    }

    // ****************************************内部方法****************************************

    IKeyConverter<Param> getConverter() {
        return mCurStrategy.getConverter();
    }

    private void traverse(Param param, ICallback2<Param, Data> callback) {
        CacheNode<Param, Data> node = this;
        while (null != node) {
            String key = node.getConverter().getKey(param);
            callback.onInvoke(key, node);
            node = node.mNextNode;
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private DataPack<Data> getDataFromPenetratorProtector(String key, ICallback1<Data> callback) throws DataException {
        // 加锁，避免并发时数据重复获取
        Lock lock = getLock(key);
        lock.countOfGet.incrementAndGet();
        synchronized (lock) {
            try {
                DataHolder<Data> holder = mPenetratorProtector.get(lock);
                // 若临时数据缓存有数据，则不再访问下一节点，以免并发时多次访问下一级节点(double check机制)
                if (null != holder) {
                    return holder.toDataPack(mCurStrategy);
                }
                Object provider;
                try {
                    DataPack<Data> dataPack = callback.onNoCache();
                    provider = dataPack.provider;
                    holder = DataHolder.get(dataPack, null);
                } catch (DataException e) {
                    provider = e.provider;
                    holder = DataHolder.get(e.getOriginException(), null);
                }
                mPenetratorProtector.put(lock, holder);
                // 返回结果
                return holder.toDataPack(provider);
            } finally {
                int count = lock.countOfGet.decrementAndGet();
                if (0 == count) {
                    mPenetratorProtector.remove(lock);
                }
            }
        }
    }

    /**
     * 从下一节点获取数据
     */
    private DataPack<Data> getDataFromNextNode(DataContext<Param> context, String key, IDatasource<Param, Data> datasource) throws DataException {
        try {
            DataPack<Data> pack;
            // 若没有下一节点，则从数据源获取
            if (null == mNextNode) {
                pack = getDataDirectly(mCurStrategy, context.param, datasource);
            }
            // 否则从下一节点获取缓存
            else {
                pack = mNextNode.getDataPackAndAutoCache(context, datasource);
            }
            mCurStrategy.onCacheData(context, key, pack);
            return pack;
        } catch (DataException e) {
            return mCurStrategy.onHandleException(context, key, e);
        }
    }

    /**
     * 从本地缓存服务获取数据
     */
    private DataPack<Data> getDataFromCurNode(DataContext<Param> context, String key, ICallback1<Data> listener) throws DataException {
        try {
            return mCurStrategy.onGetCache(context, key);
        } catch (NoCacheException e) {
            return listener.onNoCache();
        }
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

    private interface ICallback1<Data> {
        DataPack<Data> onNoCache() throws DataException;
    }

    private interface ICallback2<Param, Data> {
        void onInvoke(String key, CacheNode<Param, Data> node);
    }

    private static class PenetratorProtector<Data> {
        private final Map<Lock, DataHolder<Data>> mTmpMap = new ConcurrentHashMap<>();

        DataHolder<Data> get(Lock lock) {
            return mTmpMap.get(lock);
        }

        void put(Lock lock, DataHolder<Data> dataHolder) {
            mTmpMap.put(lock, dataHolder);
        }

        void remove(Lock lock) {
            mTmpMap.remove(lock);
        }
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
