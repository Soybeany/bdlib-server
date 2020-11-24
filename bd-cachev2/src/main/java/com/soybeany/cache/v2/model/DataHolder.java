package com.soybeany.cache.v2.model;

import com.soybeany.cache.v2.exception.DataException;

/**
 * @author Soybeany
 * @date 2020/11/19
 */
public class DataHolder<Data> {

    public final boolean norm; // 是否为正常数据
    public final Data data; // 数据
    public final Exception exception; // 相关的异常
    public final long expiredTime; // 失效的时间戳

    public static <Data> DataHolder<Data> get(DataPack<Data> dataPack, long expiryMillis) {
        return new DataHolder<>(true, dataPack.data, null, Math.min(dataPack.expiryMillis, expiryMillis));
    }

    public static <Data> DataHolder<Data> get(Exception exception, long expiryMillis) {
        Exception e = exception;
        long expiry = expiryMillis;
        if (exception instanceof DataException) {
            e = ((DataException) exception).getOriginException();
            expiry = Math.min(((DataException) exception).expiryMillis, expiryMillis);
        }
        return new DataHolder<>(false, null, e, expiry);
    }

    public DataHolder(boolean norm, Data data, Exception exception, long expiryMillis) {
        this.norm = norm;
        this.data = data;
        this.exception = exception;
        this.expiredTime = currentTimeMillis() + expiryMillis;
    }

    public DataPack<Data> toDataPack(Object provider) throws DataException {
        long expiryMillis = getRemainingValidTimeInMillis();
        if (!norm) {
            throw new DataException(provider, exception, expiryMillis);
        }
        return new DataPack<>(provider, data, expiryMillis);
    }

    public boolean isExpired() {
        return getRemainingValidTimeInMillis() <= 0;
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    private long getRemainingValidTimeInMillis() {
        return expiredTime - currentTimeMillis();
    }
}
