package com.soybeany.cache.v2.component;

import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.model.DataFrom;
import com.soybeany.cache.v2.model.DataHolder;
import com.soybeany.cache.v2.model.DataPack;
import com.soybeany.cache.v2.strategy.StdCacheStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * <br>Created by Soybeany on 2020/10/16.
 */
public class DBSimulationStrategy<Param, Data> extends StdCacheStrategy<Param, Data> {
    private final Map<String, TimeWrapper<Data>> map = new HashMap<>();

    @Override
    public String desc() {
        return "仿数据库";
    }

    @Override
    public DataPack<Data> onGetCache(Param param, String key) throws DataException, NoCacheException {
        if (!map.containsKey(key)) {
            throw new NoCacheException();
        }
        TimeWrapper<Data> wrapper = map.get(key);
        long remainingValidTime = wrapper.getRemainingValidTimeInMills(TimeWrapper.currentTimeMillis());
        if (TimeWrapper.isExpired(remainingValidTime)) {
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
    public void onCacheData(Param param, String key, DataPack<Data> data) {
        map.put(key, TimeWrapper.get(data, mExpiry, TimeWrapper.currentTimeMillis()));
    }

    @Override
    public void removeCache(Param param, String key) {
        map.remove(key);
    }

    @Override
    public void clearCache() {
        map.clear();
    }

    @Override
    protected void onCacheException(Param param, String key, Exception e) {
        map.put(key, TimeWrapper.get(e, mFastFailExpiry, TimeWrapper.currentTimeMillis()));
    }
}
