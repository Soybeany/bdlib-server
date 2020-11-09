package com.soybeany.cache.v2.strategy;


import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.model.DataHolder;
import com.soybeany.cache.v2.model.DataPack;

/**
 * @author Soybeany
 * @date 2020/1/20
 */
public abstract class StdCacheStrategy<Param, Data> implements ICacheStrategy<Param, Data> {

    protected long mExpiry = Long.MAX_VALUE; // 永不超时
    protected long mFastFailExpiry = 60000; // 1分钟;

    @Override
    public int order() {
        return ORDER_DEFAULT;
    }

    @Override
    public boolean supportDoubleCheck() {
        return false;
    }

    @Override
    public DataPack<Data> onHandleException(Param param, String key, DataException e) throws DataException {
        onCacheException(param, key, e.getOriginException());
        throw e;
    }

    /**
     * 数据失效的超时，用于一般场景
     *
     * @param millis 失效时间(毫秒)
     * @return 自身，方便链式调用
     */
    public StdCacheStrategy<Param, Data> expiry(long millis) {
        mExpiry = millis;
        return this;
    }

    /**
     * 快速失败的超时，用于防缓存穿透等场景
     *
     * @param millis 失效时间(毫秒)
     * @return 自身，方便链式调用
     */
    public StdCacheStrategy<Param, Data> fastFailExpiry(long millis) {
        mFastFailExpiry = millis;
        return this;
    }

    /**
     * 缓存异常时的回调
     */
    protected abstract void onCacheException(Param param, String key, Exception e);


    protected static class TimeWrapper<Data> {

        public final DataHolder<Data> target; // 目标dataHolder
        public final long createStamp; // 创建时的时间戳

        public static <Data> TimeWrapper<Data> get(DataPack<Data> data, long expiryMillis, long createStamp) {
            return new TimeWrapper<Data>(DataHolder.get(data, expiryMillis), createStamp);
        }

        public static <Data> TimeWrapper<Data> get(Exception exception, long expiryMillis, long createStamp) {
            return new TimeWrapper<Data>(DataHolder.<Data>get(exception, expiryMillis), createStamp);
        }

        public static boolean isExpired(long remainingValidTime) {
            return remainingValidTime <= 0;
        }

        public static long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        public TimeWrapper(DataHolder<Data> target, long createStamp) {
            this.target = target;
            this.createStamp = createStamp;
        }

        public long getRemainingValidTimeInMills(long curTimeInMills) {
            long expiredTime = createStamp + target.getExpiryMillis();
            return expiredTime - curTimeInMills;
        }
    }
}
