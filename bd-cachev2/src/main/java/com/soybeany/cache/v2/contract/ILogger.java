package com.soybeany.cache.v2.contract;

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
    void onGetData(String dataDesc, String paramDesc, Param param, DataPack<Data> pack);

    /**
     * 缓存数据时的回调
     */
    void onCacheData(String dataDesc, String paramDesc, Param param, DataPack<Data> pack);

    /**
     * 缓存异常时的回调
     */
    void onCacheException(String dataDesc, String paramDesc, Param param, Exception e);

    /**
     * 移除缓存时的回调
     */
    void onRemoveCache(String dataDesc, String paramDesc, Param param);

    /**
     * 清除缓存时的回调
     */
    void onClearCache(String dataDesc);

}
