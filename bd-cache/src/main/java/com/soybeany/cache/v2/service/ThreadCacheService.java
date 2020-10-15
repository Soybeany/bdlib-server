package com.soybeany.cache.v2.service;

import com.soybeany.cache.v2.exception.CacheAntiPenetrateException;
import com.soybeany.cache.v2.exception.CacheException;
import com.soybeany.cache.v2.exception.CacheNoDataException;
import com.soybeany.cache.v2.module.DataPack;

import java.util.HashMap;
import java.util.Map;

/**
 * 使用该服务时，需手动调用{@link #start}与{@link #finish}，以免出现性能问题
 *
 * @author Soybeany
 * @date 2020/9/23
 */
public class ThreadCacheService<Param, Data> extends BaseMemCacheService<Param, Data> {

    private final ThreadLocal<Map<Param, DataHolder<Data>>> threadLocal = new ThreadLocal<Map<Param, DataHolder<Data>>>();

    @Override
    public String getId() {
        return "THREAD_LOCAL";
    }

    @Override
    public boolean supportDoubleCheck() {
        return true;
    }

    @Override
    public DataPack<Data> onRetrieveCachedData(String dataGroup, Param param, String key) throws CacheException {
        Map<Param, DataHolder<Data>> map = threadLocal.get();
        if (!map.containsKey(param)) {
            throw new CacheNoDataException();
        }
        DataHolder<Data> holder = map.get(param);
        if (!holder.hasData) {
            throw new CacheAntiPenetrateException();
        }
        return DataPack.newCacheDataPack(holder.data, true);
    }

    @Override
    public void onCacheData(String dataGroup, Param param, String key, Data data) {
        threadLocal.get().put(param, DataHolder.get(data));
    }

    @Override
    public void onNoDataToCache(String dataGroup, Param param, String key) {
        threadLocal.get().put(param, new DataHolder<Data>(null, false));
    }

    @Override
    public void removeCache(String dataGroup, Param param, String key) {
        threadLocal.get().remove(param);
    }

    @Override
    public void clearCache(String dataGroup) {
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
