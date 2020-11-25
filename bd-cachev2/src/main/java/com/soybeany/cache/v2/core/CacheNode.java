package com.soybeany.cache.v2.core;

import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.contract.IKeyConverter;
import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.exception.NoDataSourceException;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataCore;
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

    public static <Param, Data> DataPack<Data> getDataDirectly(Object invoker, Param param, IDatasource<Param, Data> datasource) {
        // 没有指定数据源
        if (null == datasource) {
            return new DataPack<>(DataCore.get(new NoDataSourceException()), invoker, Integer.MAX_VALUE);
        }
        // 正常执行
        try {
            return new DataPack<>(DataCore.get(datasource.onGetData(param)), datasource, Integer.MAX_VALUE);
        } catch (Exception e) {
            return new DataPack<>(DataCore.get(e), datasource, Integer.MAX_VALUE);
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
    DataPack<Data> getDataPackAndAutoCache(DataContext<Param> context, final IDatasource<Param, Data> datasource) {
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

    void cacheData(DataContext<Param> context, DataPack<Data> pack) {
        traverse(context.param, (key, node) -> node.mCurStrategy.onCacheData(context, key, pack));
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
    private DataPack<Data> getDataFromPenetratorProtector(String key, ICallback1<Data> callback) {
        // 加锁，避免并发时数据重复获取
        Lock lock = getLock(key);
        lock.countOfGet.incrementAndGet();
        synchronized (lock) {
            try {
                DataPack<Data> tmpPack = mPenetratorProtector.get(lock);
                // 若临时数据缓存有数据，则不再访问下一节点，以免并发时多次访问下一级节点(double check机制)
                if (null != tmpPack) {
                    return tmpPack;
                }
                DataPack<Data> dataPack = callback.onNoCache();
                tmpPack = new DataPack<>(dataPack.dataCore, mCurStrategy, dataPack.expiryMillis);
                mPenetratorProtector.put(lock, tmpPack);
                // 返回结果
                return dataPack;
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
    private DataPack<Data> getDataFromNextNode(DataContext<Param> context, String key, IDatasource<Param, Data> datasource) {
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
    }

    /**
     * 从本地缓存服务获取数据
     */
    private DataPack<Data> getDataFromCurNode(DataContext<Param> context, String key, ICallback1<Data> callback) {
        try {
            return mCurStrategy.onGetCache(context, key);
        } catch (NoCacheException e) {
            return callback.onNoCache();
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
        DataPack<Data> onNoCache();
    }

    private interface ICallback2<Param, Data> {
        void onInvoke(String key, CacheNode<Param, Data> node);
    }

    private static class PenetratorProtector<Data> {
        private final Map<Lock, DataPack<Data>> mTmpMap = new ConcurrentHashMap<>();

        DataPack<Data> get(Lock lock) {
            return mTmpMap.get(lock);
        }

        void put(Lock lock, DataPack<Data> dataPack) {
            mTmpMap.put(lock, dataPack);
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
