package com.soybeany.cache.v2.strategy;

import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.model.DataFrom;
import com.soybeany.cache.v2.model.DataPack;

import java.util.HashMap;
import java.util.Map;

/**
 * 使用该服务时，需手动调用{@link #start}与{@link #finish}，以免出现性能问题
 *
 * @author Soybeany
 * @date 2020/9/23
 */
public class ThreadCacheStrategy<Param, Data> extends BaseMemCacheStrategy<Param, Data> {

    private final ThreadLocal<Map<Param, DataHolder<Data>>> threadLocal = new ThreadLocal<Map<Param, DataHolder<Data>>>();

    @Override
    public String getName() {
        return "THREAD_LOCAL";
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
        if (!holder.isNorm) {
            throw new DataException(DataFrom.CACHE, holder.exception);
        }
        return DataPack.newCacheDataPack(holder.data);
    }

    @Override
    public void onCacheData(Param param, String key, Data data) {
        threadLocal.get().put(param, DataHolder.get(data));
    }

    @Override
    public void onCacheException(Param param, String key, Exception e) {
        threadLocal.get().put(param, DataHolder.<Data>get(e));
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
