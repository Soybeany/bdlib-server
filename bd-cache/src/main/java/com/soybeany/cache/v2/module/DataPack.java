package com.soybeany.cache.v2.module;


import com.soybeany.cache.v2.core.ICacheService;
import com.soybeany.cache.v2.core.IDataProvider;
import com.soybeany.cache.v2.core.IDatasource;

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
     * 数据来源说明
     */
    public final String dataFromDesc;

    /**
     * 此数据是否能让上级缓存服务缓存
     */
    public final boolean canCache;

    public static <Data> DataPack<Data> newCacheDataPack(Data data, IDataProvider provider, boolean canCache) {
        if (!(provider instanceof ICacheService)) {
            throw new RuntimeException("数据提供者没有实现“ICacheService”接口");
        }
        return new DataPack<>(data, DataFrom.CACHE, provider, canCache);
    }

    public static <Data> DataPack<Data> newSourceDataPack(Data data, IDataProvider provider) {
        if (!(provider instanceof IDatasource)) {
            throw new RuntimeException("数据提供者没有实现“IDatasource“接口");
        }
        return new DataPack<>(data, DataFrom.SOURCE, provider, true);
    }

    private DataPack(Data data, DataFrom from, IDataProvider provider, boolean canCache) {
        this.data = data;
        this.from = from;
        dataFromDesc = provider.getDesc();
        this.canCache = canCache;
    }

}
