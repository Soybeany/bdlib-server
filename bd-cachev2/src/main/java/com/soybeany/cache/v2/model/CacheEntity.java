package com.soybeany.cache.v2.model;

/**
 * 缓存存储器内部使用的实体
 * <br>Created by Soybeany on 2020/11/25.
 */
public class CacheEntity<Data> {

    /**
     * 目标数据缓存
     */
    public final DataCore<Data> dataCore;

    /**
     * 该数据失效的时间戳(时间点)
     */
    public final long expiredTimestamp;

    public static <Data> CacheEntity<Data> fromDataPack(DataPack<Data> dataPack, long curTimestamp, int maxNormalExpiryMillis, int maxAbnormalExpiryMillis) {
        int maxExpiryMillis = dataPack.dataCore.norm ? maxNormalExpiryMillis : maxAbnormalExpiryMillis;
        long expiryMillis = Math.min(dataPack.expiryMillis, maxExpiryMillis);
        return new CacheEntity<>(dataPack.dataCore, curTimestamp + expiryMillis);
    }

    public static <Data> DataPack<Data> toDataPack(CacheEntity<Data> entity, Object provider, long curTimestamp) {
        return new DataPack<>(entity.dataCore, provider, (int) (entity.expiredTimestamp - curTimestamp));
    }

    private CacheEntity(DataCore<Data> dataCore, long expiredTimestamp) {
        this.dataCore = dataCore;
        this.expiredTimestamp = expiredTimestamp;
    }

    public boolean isExpired(long curTimestamp) {
        return curTimestamp > expiredTimestamp;
    }

}
