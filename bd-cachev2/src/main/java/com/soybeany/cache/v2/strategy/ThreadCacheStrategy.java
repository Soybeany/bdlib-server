package com.soybeany.cache.v2.strategy;

import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.model.DataFrom;
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

    private final ThreadLocal<Map<Param, DataHolder<Data>>> threadLocal = new ThreadLocal<Map<Param, DataHolder<Data>>>();

    @Override
    public int order() {
        return ORDER_DEFAULT;
    }

    @Override
    public String desc() {
        return "同线程";
    }

    @Override
    public boolean supportDoubleCheck() {
        return true;
    }

    @Override
    public DataPack<Data> onGetCache(Param param, String key) throws DataException, NoCacheException {
        Map<Param, DataHolder<Data>> map = threadLocal.get();
        if (!map.containsKey(param)) {
            throw new NoCacheException();
        }
        DataHolder<Data> holder = map.get(param);
        if (holder.abnormal()) {
            throw new DataException(DataFrom.CACHE, holder.getException());
        }
        return DataPack.newCacheDataPack(this, holder.getData(), Long.MAX_VALUE);
    }

    @Override
    public void onCacheData(Param param, String key, DataPack<Data> data) {
        threadLocal.get().put(param, DataHolder.get(data.data));
    }

    @Override
    public DataPack<Data> onHandleException(Param param, String key, DataException e) throws DataException {
        threadLocal.get().put(param, DataHolder.<Data>get(e));
        throw e;
    }

    @Override
    public void removeCache(Param param, String key) {
        threadLocal.get().remove(param);
    }

    @Override
    public void clearCache() {
        threadLocal.get().clear();
    }

    // ********************操作方法********************

    public void start() {
        threadLocal.set(new HashMap<Param, DataHolder<Data>>());
    }

    public void finish() {
        threadLocal.remove();
    }

}
