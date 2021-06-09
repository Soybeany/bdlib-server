package com.soybeany.cache.v2.storage;


import com.soybeany.cache.v2.contract.ICacheStorage;
import com.soybeany.cache.v2.contract.IKeyConverter;
import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataPack;

/**
 * @author Soybeany
 * @date 2020/1/20
 */
public abstract class StdCacheStorage<Param, Data> implements ICacheStorage<Param, Data> {

    protected int mExpiry = Integer.MAX_VALUE; // 永不超时
    protected int mFastFailExpiry = 60000; // 1分钟;

    private IKeyConverter<Param> mKeyConverter;

    @Override
    public int order() {
        return ORDER_DEFAULT;
    }

    @Override
    public boolean supportGetCacheBeforeAccessNextStorage() {
        return false;
    }

    @Override
    public IKeyConverter<Param> getConverter() {
        return mKeyConverter;
    }

    @Override
    public ICacheStorage<Param, Data> converter(IKeyConverter<Param> converter) {
        mKeyConverter = converter;
        return this;
    }

    @Override
    public DataPack<Data> onGetCacheBeforeAccessNextStorage(DataContext<Param> context, String key) throws NoCacheException {
        throw new NoCacheException();
    }

    /**
     * 数据失效的超时，用于一般场景
     *
     * @param millis 失效时间(毫秒)
     * @return 自身，方便链式调用
     */
    public StdCacheStorage<Param, Data> expiry(int millis) {
        mExpiry = Math.max(millis, 0);
        return this;
    }

    /**
     * 快速失败的超时，用于防缓存穿透等场景
     *
     * @param millis 失效时间(毫秒)
     * @return 自身，方便链式调用
     */
    public StdCacheStorage<Param, Data> fastFailExpiry(int millis) {
        mFastFailExpiry = Math.max(millis, 0);
        return this;
    }

}
