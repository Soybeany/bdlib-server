package com.soybeany.cache.v2.service;


import com.soybeany.cache.v2.core.ICacheService;

/**
 * @author Soybeany
 * @date 2020/1/20
 */
public abstract class BaseCacheService<Param, Data> implements ICacheService<Param, Data> {

    protected long mExpiry = Long.MAX_VALUE; // 永不超时
    protected long mNoDataExpiry = 60000; // 1分钟;

    @Override
    public ICacheService<Param, Data> expiry(long millis) {
        mExpiry = millis;
        return this;
    }

    @Override
    public ICacheService<Param, Data> noDataExpiry(long millis) {
        mNoDataExpiry = millis;
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

    protected static class DataHolder<Data> {
        final Data data; // 数据
        final long stamp; // 创建时的时间戳
        final boolean hasData; // 是否有数据

        public static <Data> DataHolder<Data> get(Data data) {
            return new DataHolder<>(data, true);
        }

        public static <Data> DataHolder<Data> noData() {
            return new DataHolder<>(null, false);
        }

        public DataHolder(Data data, long stamp, boolean hasData) {
            this.data = data;
            this.stamp = stamp;
            this.hasData = hasData;
        }

        private DataHolder(Data data, boolean hasData) {
            this(data, System.currentTimeMillis(), hasData);
        }
    }
}
