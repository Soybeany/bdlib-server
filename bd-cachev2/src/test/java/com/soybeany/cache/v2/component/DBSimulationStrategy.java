package com.soybeany.cache.v2.component;

import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.model.*;
import com.soybeany.cache.v2.strategy.StdCacheStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * <br>Created by Soybeany on 2020/10/16.
 */
public class DBSimulationStrategy<Param, Data> extends StdCacheStrategy<Param, Data> {
    private final Map<String, DataHolderTimeWrapper<Data>> map = new HashMap<>();

    @Override
    public String desc() {
        return "仿数据库";
    }

    @Override
    public DataPack<Data> onGetCache(DataContext<Param> context, String key) throws DataException, NoCacheException {
        if (!map.containsKey(key)) {
            throw new NoCacheException();
        }
        DataHolderTimeWrapper<Data> wrapper = map.get(key);
        long remainingValidTime = wrapper.getRemainingValidTimeInMillis();
        if (DataHolderTimeWrapper.isExpired(remainingValidTime)) {
            map.remove(key);
            throw new NoCacheException();
        }
        DataHolder<Data> holder = wrapper.target;
        if (holder.abnormal()) {
            throw new DataException(DataFrom.CACHE, holder.getException());
        }
        return DataPack.newCacheDataPack(this, holder.getData(), remainingValidTime);
    }

    @Override
    public void onCacheData(DataContext<Param> context, String key, DataPack<Data> data) {
        map.put(key, DataHolderTimeWrapper.get(data, mExpiry));
    }

    @Override
    public void removeCache(DataContext<Param> context, String key) {
        map.remove(key);
    }

    @Override
    public void clearCache(String dataDesc) {
        map.clear();
    }

    @Override
    protected void onCacheException(DataContext<Param> context, String key, Exception e) {
        map.put(key, DataHolderTimeWrapper.get(e, mFastFailExpiry));
    }
}
