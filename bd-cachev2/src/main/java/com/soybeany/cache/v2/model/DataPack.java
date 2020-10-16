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
     * 数据来源
     */
    public final DataFrom from;

    /**
     * 该数据剩余的有效时间
     */
    public final long expiryInMills;

    public static <Data> DataPack<Data> newCacheDataPack(Data data, long expiryInMills) {
        return new DataPack<Data>(data, DataFrom.CACHE, expiryInMills);
    }

    public static <Data> DataPack<Data> newSourceDataPack(Data data) {
        return new DataPack<Data>(data, DataFrom.SOURCE, Long.MAX_VALUE);
    }

    private DataPack(Data data, DataFrom from, long expiryInMills) {
        this.data = data;
        this.from = from;
        this.expiryInMills = expiryInMills;
    }
}
