package com.soybeany.cache.v2.contract;


import com.soybeany.cache.v2.exception.DataException;
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

    // ********************回调类********************

    /**
     * 获取缓存的回调(没有线程安全保障)
     *
     * @param context 上下文，含有当前环境的一些信息
     * @param key     使用{@link IKeyConverter}对{@link Param}进行转化后的键，用于KV
     * @return 数据
     */
    DataPack<Data> onGetCache(Channel channel, DataContext<Param> context, String key) throws DataException, NoCacheException;

    /**
     * 缓存数据的回调(没有线程安全保障，需实现类自行保证)
     *
     * @param context 上下文，含有当前环境的一些信息
     * @param key     使用{@link IKeyConverter}对{@link Param}进行转化后的键，用于KV
     * @param data    待缓存的数据
     */
    void onCacheData(Channel channel, DataContext<Param> context, String key, DataPack<Data> data);

    /**
     * 处理异常的回调，一般的实现为缓存异常并重新抛出异常(没有线程安全保障，需实现类自行保证)
     *
     * @param channel 使用的频道
     * @param context 上下文，含有当前环境的一些信息
     * @param key     使用{@link IKeyConverter}对{@link Param}进行转化后的键，用于KV
     * @param e       待处理的异常
     */
    DataPack<Data> onHandleException(Channel channel, DataContext<Param> context, String key, DataException e) throws DataException;

    // ********************触发类********************

    /**
     * 获取目前使用的转换器
     */
    IKeyConverter<Param> getConverter();

    /**
     * 为该策略设置指定的键转换器
     */
    ICacheStrategy<Param, Data> converter(IKeyConverter<Param> converter);

    /**
     * 移除指定的缓存(没有线程安全保障，需实现类自行保证)
     *
     * @param context 上下文，含有当前环境的一些信息
     * @param key     使用{@link IKeyConverter}对{@link Param}进行转化后的键，用于KV
     */
    void removeCache(DataContext<Param> context, String key);

    /**
     * 清除全部缓存(没有线程安全保障，需实现类自行保证)
     */
    void clearCache(String dataDesc);

    /**
     * 该异常只供框架内部使用，用户不应接触该异常
     * <br>Created by Soybeany on 2020/10/15.
     */
    class NoCacheException extends Exception {
        public NoCacheException() {
            super("没有找到缓存");
        }
    }

    enum Channel {
        // 获取数据  获取缓存
        GET_DATA, GET_CACHE
    }

}
