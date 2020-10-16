package com.soybeany.cache.v2.strategy;

import com.soybeany.cache.v2.model.DataPack;

/**
 * @author Soybeany
 * @date 2020/1/19
 */
public abstract class BaseMemCacheStrategy<Param, Data> extends BaseCacheStrategy<Param, Data> {

    protected static class DataHolder<Data> {
        final Data data; // 数据
        final Exception exception; // 相关的异常
        final boolean isNorm; // 是否为正常数据
        final long expiry; // 超时

        private final long mCreateStamp; // 创建时的时间戳

        public static <Data> DataHolder<Data> get(DataPack<Data> data, long expiryInMills) {
            return new DataHolder<Data>(data, null, true, expiryInMills);
        }

        public static <Data> DataHolder<Data> get(Exception exception, long expiryInMills) {
            return new DataHolder<Data>(null, exception, false, expiryInMills);
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
         * 判断此数据是否已经失效
         */
        public boolean isExpired() {
            return System.currentTimeMillis() - mCreateStamp > expiry;
        }
    }

}
