package com.soybeany.cache.v2.core;

import com.soybeany.cache.v2.model.DataPack;

/**
 * 一些高级用法的封装
 *
 * @author Soybeany
 * @date 2021/6/10
 */
public class DataManagerUtils {

    /**
     * 根据指定的至少有效时间，直接获取/刷新数据包
     */
    public static <Param, Data> DataPack<Data> getOrRefreshDataPack(DataManager<Param, Data> manager, Param param, int validMillisAtLease) {
        DataPack<Data> dataPack = manager.getDataPack(param);
        // 若数据的有效期满足指定要求，则直接返回
        if (dataPack.remainValidMillis >= validMillisAtLease) {
            return dataPack;
        }
        // 否则清除缓存，重新获取
        manager.removeCache(param);
        return manager.getDataPack(param);
    }

}
