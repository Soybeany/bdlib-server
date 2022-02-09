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
        private boolean enableRenewExpiredCache;

        @Override
        public DataPack<Data> onGetCache(DataContext<Param> context, String key) throws NoCacheException {
            CacheEntity<Data> cacheEntity = onLoadCacheEntity(context, key);
            long curTimestamp = onGetCurTimestamp();
            // 若缓存中的数据过期，则移除数据后抛出无数据异常
            if (cacheEntity.isExpired(curTimestamp)) {
                if (!enableRenewExpiredCache) {
                    onRemoveCache(context, key);
                }
                throw new NoCacheException();
            }
            // 返回数据
            return CacheEntity.toDataPack(cacheEntity, this, curTimestamp);
        }

        @Override
        public DataPack<Data> onCacheData(DataContext<Param> context, String key, DataPack<Data> dataPack) {
            // 若不支持缓存刷新，则不作额外处理
            if (dataPack.norm() || !enableRenewExpiredCache) {
                return simpleCacheData(context, key, dataPack);
            }
            try {
                CacheEntity<Data> cacheEntity = onLoadCacheEntity(context, key);
                // 若缓存依旧可用，则直接使用
                long curTimestamp = onGetCurTimestamp();
                if (!cacheEntity.isExpired(curTimestamp)) {
                    return CacheEntity.toDataPack(cacheEntity, this, curTimestamp);
                }
                // 不是正常数据，则当缓存不存在处理
                if (!cacheEntity.dataCore.norm) {
                    throw new NoCacheException();
                }
                // 重新持久化一个使用新过期时间的info
                CacheEntity<Data> newCacheEntity = new CacheEntity<>(cacheEntity.dataCore, curTimestamp + pTtlErr);
                onSaveCacheEntity(context, key, newCacheEntity);
                return CacheEntity.toDataPack(newCacheEntity, this, curTimestamp);
            } catch (NoCacheException e) {
                // 没有本地缓存，按常规处理
                return simpleCacheData(context, key, dataPack);
            }
        }

        @Override
        public void enableRenewExpiredCache(boolean enable) {
            enableRenewExpiredCache = enable;
        }

        private DataPack<Data> simpleCacheData(DataContext<Param> context, String key, DataPack<Data> data) {
            CacheEntity<Data> cacheEntity = CacheEntity.fromDataPack(data, onGetCurTimestamp(), pTtl, pTtlErr);
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
