package com.soybeany.cache.v2.contract;


import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.model.DataPack;

/**
 * 缓存策略
 *
 * @author Soybeany
 * @date 2020/1/19
 */
public interface ICacheStrategy<Param, Data> {

    /**
     * 默认的优先级
     */
    int ORDER_DEFAULT = 0;

    // ********************配置类********************

    /**
     * 指定优先级
     *
     * @return 优先级值，值越大，越先被使用
     */
    int order();

    /**
     * 该策略的描述
     *
     * @return 具体的描述文本
     */
    String desc();

    // ********************回调类********************

    /**
     * 获取缓存时的回调
     *
     * @param param 参数
     * @param key   键
     * @return 数据
     */
    DataPack<Data> onGetCache(Param param, String key) throws DataException, NoCacheException;

    /**
     * 缓存数据的回调
     *
     * @param param 参数
     * @param key   键
     * @param data  待缓存的数据
     */
    void onCacheData(Param param, String key, DataPack<Data> data);

    /**
     * 缓存异常的回调
     *
     * @param param 参数
     * @param key   键
     * @param e     待缓存的异常
     */
    void onCacheException(Param param, String key, Exception e);

    // ********************触发类********************

    /**
     * 移除指定的缓存
     *
     * @param param 参数
     * @param key   键
     */
    void removeCache(Param param, String key);

    /**
     * 清除全部缓存
     */
    void clearCache();

    /**
     * 该异常只供框架内部使用，用户不应接触该异常
     * <br>Created by Soybeany on 2020/10/15.
     */
    class NoCacheException extends Exception {
        public NoCacheException() {
            super("没有找到缓存");
        }
    }
}
