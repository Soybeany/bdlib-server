package com.soybeany.cache.v2.contract;


import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.exception.NoCacheException;
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
     * 获取该缓存策略的名称
     */
    String getName();

    /**
     * 标识是否支持双重检查，即加锁后是否能够再次调用{@link #onGetCache}方法
     * <br>默认为false，但查询过程资源消耗小的服务，可以将此值设为true
     *
     * @return true表示支持，false表示不支持
     */
    boolean supportDoubleCheck();

    /**
     * 数据失效的超时，用于一般场景
     *
     * @param millis 失效时间(毫秒)
     * @return 自身，方便链式调用
     */
    ICacheStrategy<Param, Data> expiry(long millis);

    /**
     * 快速失败的超时，用于防缓存穿透等场景
     *
     * @param millis 失效时间(毫秒)
     * @return 自身，方便链式调用
     */
    ICacheStrategy<Param, Data> fastFailExpiry(long millis);

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
     * 缓存数据的回调，与{@link #expiry(long)}对应
     *
     * @param param 参数
     * @param key   键
     * @param data  待缓存的数据
     */
    void onCacheData(Param param, String key, DataPack<Data> data);

    /**
     * 缓存异常的回调，与{@link #fastFailExpiry(long)}对应
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
}
