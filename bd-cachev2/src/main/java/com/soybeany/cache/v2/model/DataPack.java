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
    private long mLeftValidTime;

    public static <Data> DataPack<Data> newCacheDataPack(Data data) {
        return new DataPack<Data>(data, DataFrom.CACHE);
    }

    public static <Data> DataPack<Data> newSourceDataPack(Data data) {
        return new DataPack<Data>(data, DataFrom.SOURCE);
    }

    private DataPack(Data data, DataFrom from) {
        this.data = data;
        this.from = from;
    }

}
