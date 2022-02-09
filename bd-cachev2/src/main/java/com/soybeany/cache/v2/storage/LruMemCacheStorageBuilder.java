package com.soybeany.cache.v2.storage;

import com.soybeany.cache.v2.contract.ICacheStorage;
import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.model.CacheEntity;
import com.soybeany.cache.v2.model.DataContext;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Soybeany
 * @date 2022/2/8
 */
@Accessors(fluent = true, chain = true)
public class LruMemCacheStorageBuilder<Param, Data> extends StdStorageBuilder<Param, Data> {

    /**
     * 设置用于存放数据的队列容量
     */
    @Setter
    protected int capacity = 100;

    @Override
    protected ICacheStorage<Param, Data> onBuild() {
        return new Storage<>(pTtl, pTtlErr, enableRenewExpiredCache, capacity);
    }

    // ***********************内部类****************************

    public static class Storage<Param, Data> extends StdStorage<Param, Data> {

        private final LruMap<String, CacheEntity<Data>> mLruMap;

        public Storage(int pTtl, int pTtlErr, boolean enableRenewExpiredCache, int capacity) {
            super(pTtl, pTtlErr, enableRenewExpiredCache);
            mLruMap = new LruMap<>(capacity);
        }

        @Override
        public String desc() {
            return "LRU";
        }

        @Override
        protected synchronized CacheEntity<Data> onLoadCacheEntity(DataContext<Param> context, String key) throws NoCacheException {
            if (!mLruMap.containsKey(key)) {
                throw new NoCacheException();
            }
            return mLruMap.get(key);
        }

        @Override
        protected synchronized CacheEntity<Data> onSaveCacheEntity(DataContext<Param> context, String key, CacheEntity<Data> entity) {
            mLruMap.put(key, entity);
            return entity;
        }

        @Override
        protected long onGetCurTimestamp() {
            return System.currentTimeMillis();
        }

        @Override
        public synchronized void onRemoveCache(DataContext<Param> context, String key) {
            mLruMap.remove(key);
        }

        @Override
        public synchronized void onClearCache() {
            mLruMap.clear();
        }

        @Override
        public int cacheSize() {
            return mLruMap.size();
        }
    }

    private static class LruMap<K, V> extends LinkedHashMap<K, V> {
        private final int capacity;

        public LruMap(int capacity) {
            super(0, 0.75f, true);
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > capacity;
        }
    }

}
