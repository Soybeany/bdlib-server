package com.soybeany.cache.v2.storage;

import com.soybeany.cache.v2.contract.ICacheStorage;
import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.model.CacheEntity;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataPack;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * @author Soybeany
 * @date 2022/2/8
 */
@Accessors(fluent = true, chain = true)
public abstract class StdStorageBuilder<Param, Data> {

    /**
     * 正常数据的生存时间，用于一般场景
     */
    @Setter
    protected int pTtl = Integer.MAX_VALUE;

    /**
     * 异常的生存时间，用于防缓存穿透等场景
     */
    @Setter
    protected int pTtlErr = 60000;

    public ICacheStorage<Param, Data> build() {
        // 预处理时间
        handleTtl();
        // 构建
        return onBuild();
    }

    private void handleTtl() {
        pTtl = Math.max(pTtl, 1);
        pTtlErr = Math.max(pTtlErr, 1);
        if (pTtlErr > pTtl) {
            pTtlErr = pTtl;
        }
    }

    protected abstract ICacheStorage<Param, Data> onBuild();

    // ***********************内部类****************************

    @RequiredArgsConstructor
    public static abstract class StdStorage<Param, Data> implements ICacheStorage<Param, Data> {

        private final int pTtl;
        private final int pTtlErr;

        @Override
        public DataPack<Data> onGetCache(DataContext<Param> context, String key) throws NoCacheException {
            CacheEntity<Data> cacheEntity = onLoadCacheEntity(context, key);
            long curTimestamp = onGetCurTimestamp();
            // 若缓存中的数据过期，则移除数据后抛出无数据异常
            if (cacheEntity.isExpired(curTimestamp)) {
                onRemoveCache(context, key);
                throw new NoCacheException();
            }
            // 返回数据
            return CacheEntity.toDataPack(cacheEntity, this, curTimestamp);
        }

        @Override
        public DataPack<Data> onCacheData(DataContext<Param> context, String key, DataPack<Data> data) {
            CacheEntity<Data> cacheEntity = CacheEntity.fromDataPack(data, System.currentTimeMillis(), pTtl, pTtlErr);
            CacheEntity<Data> newCacheEntity = onSaveCacheEntity(context, key, cacheEntity);
            if (newCacheEntity == cacheEntity) {
                return data;
            }
            return CacheEntity.toDataPack(newCacheEntity, this, onGetCurTimestamp());
        }

        protected abstract CacheEntity<Data> onLoadCacheEntity(DataContext<Param> context, String key) throws NoCacheException;

        protected abstract CacheEntity<Data> onSaveCacheEntity(DataContext<Param> context, String key, CacheEntity<Data> entity);

        protected abstract long onGetCurTimestamp();

    }

}
