package com.soybeany.cache.v2.contract;

import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataPack;

/**
 * 可用于日志输出
 *
 * @author Soybeany
 * @date 2020/10/19
 */
public interface ILogger<Param, Data> {

    /**
     * 获取数据时的回调
     */
    void onGetData(DataContext<Param> context, DataPack<Data> pack);

    /**
     * 缓存数据时的回调
     */
    void onCacheData(DataContext<Param> context, DataPack<Data> pack);

    /**
     * 移除缓存时的回调
     */
    void onRemoveCache(DataContext<Param> context, int... storageIndexes);

    /**
     * 移除陈旧缓存时的回调
     *
     * @param removeLevel 移除至的缓存等级，0为未移除，1为一级缓存，以此类推
     */
    void onRemoveOldCache(DataContext<Param> context, int removeLevel);


    /**
     * 清除缓存时的回调
     */
    void onClearCache(String dataDesc, int... storageIndexes);

}
