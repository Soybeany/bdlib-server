package com.soybeany.cache.v2.core;


import com.soybeany.cache.v2.exception.CacheException;
import com.soybeany.cache.v2.module.DataPack;

/**
 * 缓存服务
 *
 * @author Soybeany
 * @date 2020/1/19
 */
public interface ICacheService<Param, Data> extends IDataProvider {

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
     * 获取标识此缓存服务的唯一标识
     *
     * @return 唯一标识
     */
    String getId();

    /**
     * 标识是否支持双重检查，即加锁后是否能够再次调用{@link #onRetrieveCachedData}方法
     * <br>默认为false，但查询过程资源消耗小的服务，可以将此值设为true
     *
     * @return true表示支持，false表示不支持
     */
    boolean supportDoubleCheck();

    /**
     * 设置数据失效时间
     *
     * @param millis 失效时间(毫秒)
     * @return 自身，方便链式调用
     */
    ICacheService<Param, Data> expiry(long millis);

    /**
     * 设置没数据时的失效时间
     *
     * @param millis 失效时间(毫秒)
     * @return 自身，方便链式调用
     */
    ICacheService<Param, Data> noDataExpiry(long millis);

    // ********************回调类********************

    /**
     * 获取缓存数据的回调
     *
     * @param dataGroup 数据分组
     * @param param     参数
     * @param key       键
     * @return 数据
     * @throws CacheException 缓存相关的异常
     */
    DataPack<Data> onRetrieveCachedData(String dataGroup, Param param, String key) throws CacheException;

    /**
     * 缓存数据的回调，与{@link #expiry(long)}对应
     *
     * @param param 参数
     * @param key   键
     * @param data  待缓存的数据
     */
    void onCacheData(String dataGroup, Param param, String key, Data data);

    /**
     * 没有数据可供缓存时的回调，与{@link #noDataExpiry(long)}对应
     *
     * @param param 参数
     * @param key   键
     */
    void onNoDataToCache(String dataGroup, Param param, String key);

    // ********************触发类********************

    /**
     * 移除指定的缓存
     *
     * @param param 参数
     * @param key   键
     */
    void removeCache(String dataGroup, Param param, String key);

    /**
     * 清除全部缓存
     */
    void clearCache(String dataGroup);
}
