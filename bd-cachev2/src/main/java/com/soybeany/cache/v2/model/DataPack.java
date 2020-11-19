package com.soybeany.cache.v2.model;


/**
 * 数据包
 *
 * @author Soybeany
 * @date 2020/7/15
 */
public class DataPack<Data> {

    /**
     * 数据具体值
     */
    public final Data data;

    /**
     * 数据的生产者，即数据第一次的提供者
     */
    public final Object producer;

    /**
     * 数据的提供者，即数据最近一次的提供者
     */
    public final Object provider;

    /**
     * 该数据剩余的有效时间
     */
    public final long expiryMillis;

    public static <Data> DataPack<Data> newSourceDataPack(Object producer, Object provider, Data data) {
        return new DataPack<>(producer, provider, data, Long.MAX_VALUE);
    }

    public DataPack(Object producer, Object provider, Data data, long expiryMillis) {
        this.producer = producer;
        this.provider = provider;
        this.data = data;
        this.expiryMillis = expiryMillis;
    }
}
