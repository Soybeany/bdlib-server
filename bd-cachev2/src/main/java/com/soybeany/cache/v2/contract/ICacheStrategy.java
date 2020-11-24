package com.soybeany.cache.v2.contract;


import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.model.DataContext;
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
     * 是否支持“访问下一缓存策略前，再次获取缓存”，影响{@link #onGetCacheBeforeAccessNextStrategy}
     */
    boolean supportGetCacheBeforeAccessNextStrategy();

    // ********************操作回调类********************

    /**
     * 获取缓存(没有线程安全保障)
     *
     * @param context 上下文，含有当前环境的一些信息
     * @param key     使用{@link IKeyConverter}对{@link Param}进行转化后的键，用于KV
     * @return 数据
     */
    DataPack<Data> onGetCache(DataContext<Param> context, String key) throws DataException, NoCacheException;

    /**
     * 访问下一缓存策略前，再次获取缓存。需将{@link #supportGetCacheBeforeAccessNextStrategy}设置为true(有限的线程安全保障，只锁相同key)
     *
     * @param context 上下文，含有当前环境的一些信息
     * @param key     使用{@link IKeyConverter}对{@link Param}进行转化后的键，用于KV
     * @return 数据
     */
    DataPack<Data> onGetCacheBeforeAccessNextStrategy(DataContext<Param> context, String key) throws DataException, NoCacheException;

    /**
     * 缓存数据(有限的线程安全保障，只锁相同key)
     *
     * @param context 上下文，含有当前环境的一些信息
     * @param key     使用{@link IKeyConverter}对{@link Param}进行转化后的键，用于KV
     * @param data    待缓存的数据
     */
    void onCacheData(DataContext<Param> context, String key, DataPack<Data> data);

    /**
     * 处理异常，一般的实现为缓存异常并重新抛出异常(有限的线程安全保障，只锁相同key)
     *
     * @param context 上下文，含有当前环境的一些信息
     * @param key     使用{@link IKeyConverter}对{@link Param}进行转化后的键，用于KV
     * @param e       待处理的异常
     */
    DataPack<Data> onHandleException(DataContext<Param> context, String key, DataException e) throws DataException;

    // ********************操作触发类********************

    /**
     * 获取目前使用的转换器
     */
    IKeyConverter<Param> getConverter();

    /**
     * 为该策略设置指定的键转换器
     */
    ICacheStrategy<Param, Data> converter(IKeyConverter<Param> converter);

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
