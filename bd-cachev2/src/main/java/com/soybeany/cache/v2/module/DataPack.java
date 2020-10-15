package com.soybeany.cache.v2.module;


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
     * 此数据是否能让上级缓存服务缓存
     */
    public final boolean canCache;

    public static <Data> DataPack<Data> newCacheDataPack(Data data, boolean canCache) {
        return new DataPack<Data>(data, DataFrom.CACHE, canCache);
    }

    public static <Data> DataPack<Data> newSourceDataPack(Data data) {
        return new DataPack<Data>(data, DataFrom.SOURCE, true);
    }

    private DataPack(Data data, DataFrom from, boolean canCache) {
        this.data = data;
        this.from = from;
        this.canCache = canCache;
    }

}
