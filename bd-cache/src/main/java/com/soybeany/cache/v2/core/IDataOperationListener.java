package com.soybeany.cache.v2.core;


import com.soybeany.cache.v2.module.DataGetMode;
import com.soybeany.cache.v2.module.DataPack;

/**
 * 数据操作监听器
 *
 * @author Soybeany
 * @date 2020/2/3
 */
public interface IDataOperationListener<Param, Data> {

    /**
     * 使用服务时的回调
     */
    void onApplyService(String desc, ICacheService<Param, Data> service);

    /**
     * 获取数据时的回调，若在过程中抛出了异常，则不会触发此回调
     *
     * @param mode 获取数据的方式
     */
    void onGetData(String desc, DataGetMode mode, Param param, DataPack<Data> pack);

    /**
     * 更新缓存时的回调，若在过程中抛出了异常，则不会触发此回调
     */
    void onUpdateCache(String desc, Param param, Data data);

    /**
     * 移除缓存时的回调，若在过程中抛出了异常，则不会触发此回调
     */
    void onRemoveCache(String desc, Param param);

    /**
     * 清除缓存时的回调，若在过程中抛出了异常，则不会触发此回调
     */
    void onClearCache(String desc);

    /**
     * 无数据时触发此回调
     */
    void onNoData(String desc, Param param);

    /**
     * 遇到未预料的异常时，触发此回调
     */
    void onHandleUnexpectException(String desc, Exception exception);
}
