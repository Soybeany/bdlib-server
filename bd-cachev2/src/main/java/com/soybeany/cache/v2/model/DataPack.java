package com.soybeany.cache.v2.model;


import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.contract.IDatasource;

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
     * 数据来源
     */
    public final DataFrom from;

    /**
     * 数据的生产者
     */
    public final Object producer;

    /**
     * 该数据剩余的有效时间
     */
    public final long expiryInMills;

    public static <Param, Data> DataPack<Data> newCacheDataPack(ICacheStrategy<Param, Data> producer, Data data, long expiryInMills) {
        return new DataPack<Data>(producer, data, DataFrom.CACHE, expiryInMills);
    }

    public static <Param, Data> DataPack<Data> newSourceDataPack(IDatasource<Param, Data> producer, Data data) {
        return new DataPack<Data>(producer, data, DataFrom.SOURCE, Long.MAX_VALUE);
    }

    private DataPack(Object producer, Data data, DataFrom from, long expiryInMills) {
        this.producer = producer;
        this.data = data;
        this.from = from;
        this.expiryInMills = expiryInMills;
    }
}
