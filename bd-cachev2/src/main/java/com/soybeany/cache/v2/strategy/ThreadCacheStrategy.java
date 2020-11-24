package com.soybeany.cache.v2.strategy;

import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.contract.IKeyConverter;
import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataHolder;
import com.soybeany.cache.v2.model.DataPack;

import java.util.HashMap;
import java.util.Map;

/**
 * 使用该服务时，需手动调用{@link #start}与{@link #finish}，以免出现性能问题
 *
 * @author Soybeany
 * @date 2020/9/23
 */
public class ThreadCacheStrategy<Param, Data> implements ICacheStrategy<Param, Data> {

    private final ThreadLocal<Map<Param, DataHolder<Data>>> threadLocal = new ThreadLocal<>();

    @Override
    public int order() {
        return ORDER_DEFAULT;
    }

    @Override
    public String desc() {
        return "同线程";
    }

    @Override
    public boolean supportGetCacheBeforeAccessNextStrategy() {
        return false;
    }

    @Override
    public DataPack<Data> onGetCache(DataContext<Param> context, String key) throws DataException, NoCacheException {
        Map<Param, DataHolder<Data>> map = threadLocal.get();
        if (!map.containsKey(context.param)) {
            throw new NoCacheException();
        }
        DataHolder<Data> holder = map.get(context.param);
        return holder.toDataPack(this);
    }

    @Override
    public DataPack<Data> onGetCacheBeforeAccessNextStrategy(DataContext<Param> context, String key) throws DataException, NoCacheException {
        throw new NoCacheException();
    }

    @Override
    public void onCacheData(DataContext<Param> context, String key, DataPack<Data> data) {
        threadLocal.get().put(context.param, DataHolder.get(data, data.expiryMillis));
    }

    @Override
    public DataPack<Data> onHandleException(DataContext<Param> context, String key, DataException e) throws DataException {
        threadLocal.get().put(context.param, DataHolder.get(e.getOriginException(), e.expiryMillis));
        throw e;
    }

    @Override
    public IKeyConverter<Param> getConverter() {
        return null;
    }

    @Override
    public ICacheStrategy<Param, Data> converter(IKeyConverter<Param> converter) {
        return null;
    }

    @Override
    public void removeCache(DataContext<Param> context, String key) {
        threadLocal.get().remove(context.param);
    }

    @Override
    public void clearCache(String dataDesc) {
        threadLocal.get().clear();
    }

    // ********************操作方法********************

    public void start() {
        threadLocal.set(new HashMap<>());
    }

    public void finish() {
        threadLocal.remove();
    }

}
