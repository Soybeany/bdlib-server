package com.soybeany.cache.v2.strategy;

/**
 * @author Soybeany
 * @date 2020/1/19
 */
public abstract class BaseMemCacheStrategy<Param, Data> extends BaseCacheStrategy<Param, Data> {

    protected static class DataHolder<Data> {
        final Data data; // 数据
        final long stamp; // 创建时的时间戳
        final Exception exception; // 相关的异常
        final boolean isNorm; // 是否为正常数据

        public static <Data> DataHolder<Data> get(Data data) {
            return new DataHolder<Data>(data, null, true);
        }

        public static <Data> DataHolder<Data> get(Exception exception) {
            return new DataHolder<Data>(null, exception, false);
        }

        public DataHolder(Data data, Exception exception, boolean isNorm) {
            this(data, System.currentTimeMillis(), exception, isNorm);
        }

        public DataHolder(Data data, long stamp, Exception exception, boolean isNorm) {
            this.data = data;
            this.stamp = stamp;
            this.exception = exception;
            this.isNorm = isNorm;
        }

    }

}
