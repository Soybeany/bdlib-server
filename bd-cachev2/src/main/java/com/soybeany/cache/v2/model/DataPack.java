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
     * 数据的提供者，即数据最近一次的提供者
     */
    public final Object provider;

    /**
     * 该数据剩余的有效时间
     */
    public final long expiryInMills;

    public static <Param, Data> DataPack<Data> newCacheDataPack(ICacheStrategy<Param, Data> provider, Data data, long expiryInMills) {
        return new DataPack<Data>(provider, data, DataFrom.CACHE, expiryInMills);
    }

    public static <Param, Data> DataPack<Data> newSourceDataPack(IDatasource<Param, Data> provider, Data data) {
        return new DataPack<Data>(provider, data, DataFrom.SOURCE, Long.MAX_VALUE);
    }

    private DataPack(Object provider, Data data, DataFrom from, long expiryInMills) {
        this.provider = provider;
        this.data = data;
        this.from = from;
        this.expiryInMills = expiryInMills;
    }
}