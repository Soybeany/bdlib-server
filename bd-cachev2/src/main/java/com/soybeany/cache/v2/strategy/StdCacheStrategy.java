package com.soybeany.cache.v2.strategy;


import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.contract.IKeyConverter;
import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataPack;

/**
 * @author Soybeany
 * @date 2020/1/20
 */
public abstract class StdCacheStrategy<Param, Data> implements ICacheStrategy<Param, Data> {

    protected long mExpiry = Long.MAX_VALUE; // 永不超时
    protected long mFastFailExpiry = 60000; // 1分钟;

    private IKeyConverter<Param> mKeyConverter;

    @Override
    public int order() {
        return ORDER_DEFAULT;
    }

    @Override
    public IKeyConverter<Param> getConverter() {
        return mKeyConverter;
    }

    @Override
    public ICacheStrategy<Param, Data> converter(IKeyConverter<Param> converter) {
        mKeyConverter = converter;
        return this;
    }

    @Override
    public DataPack<Data> onGetCache(Channel channel, DataContext<Param> context, String key) throws DataException, NoCacheException {
        switch (channel) {
            case GET_DATA:
                return onGetCache(context, key);
            case GET_CACHE:
                return onGetCacheWithGetCacheChannel(context, key);
            default:
                throw new RuntimeException("使用了未知的channel");
        }
    }

    @Override
    public void onCacheData(Channel channel, DataContext<Param> context, String key, DataPack<Data> data) {
        switch (channel) {
            case GET_DATA:
                onCacheData(context, key, data);
                break;
            case GET_CACHE:
                onCacheDataWithGetCacheChannel(context, key, data);
                break;
            default:
                throw new RuntimeException("使用了未知的channel");
        }
    }

    @Override
    public DataPack<Data> onHandleException(Channel channel, DataContext<Param> context, String key, DataException e) throws DataException {
        switch (channel) {
            case GET_DATA:
                onCacheException(context, key, e.provider, e.getOriginException());
                break;
            case GET_CACHE:
                onCacheExceptionWithGetCacheChannel(context, key, e.provider, e.getOriginException());
                break;
            default:
                throw new RuntimeException("使用了未知的channel");
        }
        throw e;
    }

    /**
     * 数据失效的超时，用于一般场景
     *
     * @param millis 失效时间(毫秒)
     * @return 自身，方便链式调用
     */
    public StdCacheStrategy<Param, Data> expiry(long millis) {
        mExpiry = getCheckedTime(millis);
        return this;
    }

    /**
     * 快速失败的超时，用于防缓存穿透等场景
     *
     * @param millis 失效时间(毫秒)
     * @return 自身，方便链式调用
     */
    public StdCacheStrategy<Param, Data> fastFailExpiry(long millis) {
        mFastFailExpiry = getCheckedTime(millis);
        return this;
    }

    // ********************子类按需重写的方法********************

    protected DataPack<Data> onGetCacheWithGetCacheChannel(DataContext<Param> context, String key) throws DataException, NoCacheException {
        throw new NoCacheException();
    }

    protected void onCacheDataWithGetCacheChannel(DataContext<Param> context, String key, DataPack<Data> data) {
        onCacheData(context, key, data);
    }

    protected void onCacheExceptionWithGetCacheChannel(DataContext<Param> context, String key, Object producer, Exception e) {
        // 默认不作操作
    }

    // ********************内部方法********************

    private long getCheckedTime(long millis) {
        return millis > 0 ? millis : 0;
    }

    // ********************子类抽象方法********************

    protected abstract DataPack<Data> onGetCache(DataContext<Param> context, String key) throws DataException, NoCacheException;

    protected abstract void onCacheData(DataContext<Param> context, String key, DataPack<Data> data);

    /**
     * 缓存异常时的回调
     */
    protected abstract void onCacheException(DataContext<Param> context, String key, Object producer, Exception e);

}
