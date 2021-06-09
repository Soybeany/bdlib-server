package com.soybeany.cache.v2.storage;


import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.model.CacheEntity;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataPack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 使用LRU存储器的本地内存缓存
 *
 * @author Soybeany
 * @date 2020/1/19
 */
public class LruMemCacheStorage<Param, Data> extends StdCacheStorage<Param, Data> {

    private final LruMap<String, CacheEntity<Data>> mLruMap = new LruMap<>();
    private final Map<String, CacheEntity<Data>> mCacheHolder = Collections.synchronizedMap(mLruMap);

    @Override
    public String desc() {
        return "LRU";
    }

    @Override
    public void removeCache(DataContext<Param> context, String key) {
        removeCache(key);
    }

    @Override
    public void clearCache(String dataDesc) {
        mCacheHolder.clear();
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
    public LruMemCacheStorage<Param, Data> capacity(int size) {
        mLruMap.capacity = size;
        return this;
    }

    /**
     * 当前缓存的数据条数
     */
    public int size() {
        return mCacheHolder.size();
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
        onCacheData(key, cacheEntity);
    }

    /**
     * 主动移除缓存
     */
    protected void removeCache(String key) {
        mCacheHolder.remove(key);
    }

    /**
     * 缓存数据时的回调
     */
    protected void onCacheData(String key, CacheEntity<Data> cacheEntity) {
        mCacheHolder.put(key, cacheEntity);
    }

    // ********************内部方法********************

    private DataPack<Data> innerGetCache(String key) throws NoCacheException {
        CacheEntity<Data> cacheEntity = mCacheHolder.get(key);
        // 若缓存中没有数据，则直接抛出无数据异常
        if (null == cacheEntity) {
            throw new NoCacheException();
        }
        long currentTimeMillis = System.currentTimeMillis();
        // 若缓存中的数据过期，则移除数据后抛出无数据异常
        if (cacheEntity.isExpired(currentTimeMillis)) {
            mCacheHolder.remove(key);
            throw new NoCacheException();
        }
        // 返回数据
        return CacheEntity.toDataPack(cacheEntity, this, currentTimeMillis);
    }

    // ********************内部类********************

    private static class LruMap<K, V> extends LinkedHashMap<K, V> {
        int capacity = 100;

        public LruMap() {
            super(0, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }
}
