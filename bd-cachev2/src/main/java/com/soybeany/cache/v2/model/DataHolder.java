package com.soybeany.cache.v2.model;

/**
 * <br>Created by Soybeany on 2020/10/16.
 */
public class DataHolder<Data> {

    public final boolean isNorm; // 是否为正常数据

    public final Data data; // 数据
    public final Exception exception; // 相关的异常
    public final long expiry; // 超时

    private final long mCreateStamp; // 创建时的时间戳

    public static <Data> DataHolder<Data> get(DataPack<Data> data, long expiryInMills) {
        return new DataHolder<Data>(data, null, true, expiryInMills);
    }

    public static <Data> DataHolder<Data> get(Exception exception, long expiryInMills) {
        return new DataHolder<Data>(null, exception, false, expiryInMills);
    }

    public static boolean isExpired(long leftValidTime) {
        return leftValidTime < 0;
    }

    public DataHolder(DataPack<Data> data, Exception exception, boolean isNorm, long expiryInMills) {
        this.exception = exception;
        this.isNorm = isNorm;

        if (null != data) {
            this.data = data.data;
            this.expiry = Math.min(data.expiryInMills, expiryInMills);
        } else {
            this.data = null;
            this.expiry = expiryInMills;
        }
        this.mCreateStamp = System.currentTimeMillis();
    }

    /**
     * 剩余的有效时间
     */
    public long getLeftValidTime() {
        return expiry - (System.currentTimeMillis() - mCreateStamp);
    }

    /**
     * 判断此数据是否已经失效
     */
    public boolean isExpired() {
        return isExpired(getLeftValidTime());
    }
}
