package com.soybeany.cache.v2.strategy;


import com.soybeany.cache.v2.contract.ICacheStrategy;

/**
 * @author Soybeany
 * @date 2020/1/20
 */
public abstract class BaseCacheStrategy<Param, Data> implements ICacheStrategy<Param, Data> {

    protected long mExpiry = Long.MAX_VALUE; // 永不超时
    protected long mFastFailExpiry = 60000; // 1分钟;

    @Override
    public ICacheStrategy<Param, Data> expiry(long millis) {
        mExpiry = millis;
        return this;
    }

    @Override
    public ICacheStrategy<Param, Data> fastFailExpiry(long millis) {
        mFastFailExpiry = millis;
        return this;
    }

    @Override
    public int order() {
        return ORDER_DEFAULT;
    }

    @Override
    public boolean supportDoubleCheck() {
        return false;
    }

    /**
     * 判断指定时间戳是否已经过期
     */
    protected boolean isStampExpired(long stamp, long reference) {
        return System.currentTimeMillis() - stamp > reference;
    }
}
