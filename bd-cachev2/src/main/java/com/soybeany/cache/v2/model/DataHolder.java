package com.soybeany.cache.v2.model;

import com.soybeany.cache.v2.exception.DataException;

/**
 * @author Soybeany
 * @date 2020/11/19
 */
public class DataHolder<Data> {

    public final boolean norm; // 是否为正常数据
    public final Object producer; // 数据/异常的生产者
    public final Data data; // 数据
    public final Exception exception; // 相关的异常
    public final long createStamp = currentTimeMillis(); // 创建时的时间戳

    private final long expiryMillis; // 超时时间，静态

    public static <Data> DataHolder<Data> get(DataPack<Data> dataPack, Long expiryMillis) {
        long expiry = dataPack.expiryMillis;
        if (null != expiryMillis && expiryMillis < dataPack.expiryMillis) {
            expiry = expiryMillis;
        }
        return new DataHolder<>(true, dataPack.producer, dataPack.data, null, expiry);
    }

    public static <Data> DataHolder<Data> get(Object producer, Exception exception, Long expiryMillis) {
        return new DataHolder<>(false, producer, null, exception, expiryMillis);
    }

    public DataHolder(boolean norm, Object producer, Data data, Exception exception, Long expiryMillis) {
        this.norm = norm;
        this.producer = producer;
        this.data = data;
        this.exception = exception;
        this.expiryMillis = null != expiryMillis ? expiryMillis : 0;
    }

    public DataPack<Data> toDataPack(Object provider) throws DataException {
        if (!norm) {
            throw new DataException(provider, exception);
        }
        return new DataPack<>(producer, provider, data, getRemainingValidTimeInMillis());
    }

    public boolean isExpired() {
        return getRemainingValidTimeInMillis() <= 0;
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    private long getRemainingValidTimeInMillis() {
        long expiredTime = createStamp + expiryMillis;
        return expiredTime - currentTimeMillis();
    }
}
