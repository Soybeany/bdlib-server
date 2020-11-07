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

    /**
     * 标识是否支持双重检查，即加锁后是否能够再次调用{@link #onGetCache}方法
     * <br>默认为false，但查询过程资源消耗小的服务，可以将此值设为true
     *
     * @return true表示支持，false表示不支持
     */
    boolean supportDoubleCheck();

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
     * 处理异常的回调，一般的实现为缓存异常并重新抛出异常
     *
     * @param param 参数
     * @param key   键
     * @param e     待缓存的异常
     */
    DataPack<Data> onHandleException(Param param, String key, DataException e) throws DataException;

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
