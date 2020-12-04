package com.soybeany.cache.v2.strategy;


import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.model.CacheEntity;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataPack;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * 使用LRU策略的本地内存缓存
 *
 * @author Soybeany
 * @date 2020/1/19
 */
public class LruMemCacheStrategy<Param, Data> extends StdCacheStrategy<Param, Data> {

    private final LruDataAccessor<CacheEntity<Data>> mDataAccessor = new LruDataAccessor<>();

    @Override
    public String desc() {
        return "LRU";
    }

    @Override
    public void removeCache(DataContext<Param> context, String key) {
        mDataAccessor.removeData(key);
    }

    @Override
    public void clearCache(String dataDesc) {
        mDataAccessor.clear();
    }

    @Override
    public DataPack<Data> onGetCache(DataContext<Param> context, String key) throws NoCacheException {
        return getCache(key);
    }

    @Override
    public void onCacheData(DataContext<Param> context, String key, DataPack<Data> data) {
        cacheData(key, data);
    }

    /**
     * 设置用于存放数据的队列容量
     */
    public LruMemCacheStrategy<Param, Data> capacity(int size) {
        mDataAccessor.capacity = size;
        return this;
    }

    /**
     * 主动获取缓存
     */
    protected DataPack<Data> getCache(String key) throws NoCacheException {
        return innerGetCache(key);
    }

    /**
     * 主动缓存数据
     */
    protected void cacheData(String key, DataPack<Data> data) {
        CacheEntity<Data> cacheEntity = CacheEntity.fromDataPack(data, System.currentTimeMillis(), mExpiry, mFastFailExpiry);
        mDataAccessor.putData(key, cacheEntity);
    }

    // ********************内部方法********************

    private DataPack<Data> innerGetCache(String key) throws NoCacheException {
        CacheEntity<Data> cacheEntity = mDataAccessor.get(key);
        // 若缓存中没有数据，则直接抛出无数据异常
        if (null == cacheEntity) {
            throw new NoCacheException();
        }
        long currentTimeMillis = System.currentTimeMillis();
        // 若缓存中的数据过期，则移除数据后抛出无数据异常
        if (cacheEntity.isExpired(currentTimeMillis)) {
            mDataAccessor.removeData(key);
            throw new NoCacheException();
        }
        // 数据依旧有效，则移到队列末尾
        mDataAccessor.moveDataToLast(key);
        // 返回数据
        return CacheEntity.toDataPack(cacheEntity, this, currentTimeMillis);
    }

    // ********************内部类********************

    private static class LruDataAccessor<Data> {
        private final Map<String, Data> mCacheMap = new HashMap<>();
        private final LinkedList<String> mOrderList = new LinkedList<>(); // 用于记录访问顺序

        int capacity = 100;

        Data get(String key) {
            return mCacheMap.get(key);
        }

        synchronized void moveDataToLast(String key) {
            mOrderList.remove(key);
            mOrderList.offer(key);
        }

        synchronized void removeData(String key) {
            mOrderList.remove(key);
            mCacheMap.remove(key);
        }

        synchronized void putData(String key, Data data) {
            // 若存储的数据已达到设置的上限，先移除末位数据
            if (mOrderList.size() >= capacity && !mOrderList.isEmpty()) {
                removeData(mOrderList.getFirst());
            }
            mOrderList.offer(key);
            mCacheMap.put(key, data);
        }

        synchronized void clear() {
            mOrderList.clear();
            mCacheMap.clear();
        }
    }
}
