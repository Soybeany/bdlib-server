package com.soybeany.cache.v2.strategy;


import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataPack;

/**
 * @author Soybeany
 * @date 2020/1/20
 */
public abstract class StdCacheStrategy<Param, Data> implements ICacheStrategy<Param, Data> {

    protected long mExpiry = Long.MAX_VALUE; // 永不超时
    protected long mFastFailExpiry = 60000; // 1分钟;
    protected long mAntiPenetrateMillis; // 默认不做防穿透

    @Override
    public int order() {
        return ORDER_DEFAULT;
    }

    @Override
    public long antiPenetrateMillis() {
        return mAntiPenetrateMillis;
    }

    @Override
    public DataPack<Data> onHandleException(DataContext<Param> context, String key, DataException e) throws DataException {
        onCacheException(context, key, e.getOriginException());
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

    /**
     * 用于防护“缓存穿透”的时间，获取数据后，将在这段时间内
     *
     * @param millis 失效时间(毫秒)
     * @return 自身，方便链式调用
     */
    public StdCacheStrategy<Param, Data> antiPenetrateMillis(long millis) {
        mAntiPenetrateMillis = getCheckedTime(millis);
        return this;
    }

    private long getCheckedTime(long millis) {
        return millis > 0 ? millis : 0;
    }

    /**
     * 缓存异常时的回调
     */
    protected abstract void onCacheException(DataContext<Param> context, String key, Exception e);


}
