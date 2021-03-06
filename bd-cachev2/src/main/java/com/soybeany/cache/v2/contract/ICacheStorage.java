package com.soybeany.cache.v2.contract;


import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataPack;

/**
 * 缓存存储器
 *
 * @author Soybeany
 * @date 2020/1/19
 */
public interface ICacheStorage<Param, Data> {

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
     * 该存储器的描述
     *
     * @return 具体的描述文本
     */
    String desc();

    /**
     * 是否支持“访问下一缓存存储器前，再次获取缓存”，影响{@link #onGetCacheBeforeAccessNextStorage}
     */
    boolean supportGetCacheBeforeAccessNextStorage();

    // ********************操作回调类********************

    /**
     * 获取缓存(没有线程安全保障)
     *
     * @param context 上下文，含有当前环境的一些信息
     * @param key     使用{@link IKeyConverter}对{@link Param}进行转化后的键，用于KV
     * @return 数据
     */
    DataPack<Data> onGetCache(DataContext<Param> context, String key) throws NoCacheException;

    /**
     * 访问下一缓存存储器前，再次获取缓存。需将{@link #supportGetCacheBeforeAccessNextStorage}设置为true(有限的线程安全保障，只锁相同key)
     *
     * @param context 上下文，含有当前环境的一些信息
     * @param key     使用{@link IKeyConverter}对{@link Param}进行转化后的键，用于KV
     * @return 数据
     */
    DataPack<Data> onGetCacheBeforeAccessNextStorage(DataContext<Param> context, String key) throws NoCacheException;

    /**
     * 缓存数据(有限的线程安全保障，只锁相同key)
     *
     * @param context 上下文，含有当前环境的一些信息
     * @param key     使用{@link IKeyConverter}对{@link Param}进行转化后的键，用于KV
     * @param data    待缓存的数据
     */
    void onCacheData(DataContext<Param> context, String key, DataPack<Data> data);

    // ********************操作触发类********************

    /**
     * 获取目前使用的转换器
     */
    IKeyConverter<Param> getConverter();

    /**
     * 为该存储器设置指定的键转换器
     */
    ICacheStorage<Param, Data> converter(IKeyConverter<Param> converter);

    /**
     * 移除指定的缓存(有限的线程安全保障，只锁相同key)
     *
     * @param context 上下文，含有当前环境的一些信息
     * @param key     使用{@link IKeyConverter}对{@link Param}进行转化后的键，用于KV
     */
    void removeCache(DataContext<Param> context, String key);

    /**
     * 清除全部缓存(有限的线程安全保障，只锁相同key)
     */
    void clearCache(String dataDesc);

}
