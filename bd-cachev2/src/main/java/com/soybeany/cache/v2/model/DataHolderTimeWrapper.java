package com.soybeany.cache.v2.model;

/**
 * @author Soybeany
 * @date 2020/11/19
 */
public class DataHolderTimeWrapper<Data> {

    public final DataHolder<Data> target; // 目标dataHolder
    public final long createStamp = currentTimeMillis(); // 创建时的时间戳

    private final long expiryMillis; // 超时时间，静态

    public static <Data> DataHolderTimeWrapper<Data> get(DataPack<Data> dataPack, long expiryMillis) {
        Data data = null;
        if (null != dataPack) {
            data = dataPack.data;
            expiryMillis = Math.min(dataPack.expiryMillis, expiryMillis);
        }
        return new DataHolderTimeWrapper<>(DataHolder.get(data), expiryMillis);
    }

    public static <Data> DataHolderTimeWrapper<Data> get(Exception exception, long expiryMillis) {
        return new DataHolderTimeWrapper<>(DataHolder.get(exception), expiryMillis);
    }

    public static boolean isExpired(long remainingValidTime) {
        return remainingValidTime <= 0;
    }

    private static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public DataHolderTimeWrapper(DataHolder<Data> target, long expiryMillis) {
        this.target = target;
        this.expiryMillis = expiryMillis;
    }

    public long getRemainingValidTimeInMillis() {
        long expiredTime = createStamp + expiryMillis;
        return expiredTime - currentTimeMillis();
    }

    public boolean isExpired() {
        return isExpired(getRemainingValidTimeInMillis());
    }
}
