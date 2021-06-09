package com.soybeany.cache.v2.component;

import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.model.CacheEntity;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataPack;
import com.soybeany.cache.v2.storage.StdCacheStorage;

import java.util.HashMap;
import java.util.Map;

/**
 * <br>Created by Soybeany on 2020/10/16.
 */
public class DBSimulationStorage<Param, Data> extends StdCacheStorage<Param, Data> {
    private final Map<String, CacheEntity<Data>> map = new HashMap<>();

    @Override
    public String desc() {
        return "仿数据库";
    }

    @Override
    public DataPack<Data> onGetCache(DataContext<Param> context, String key) throws NoCacheException {
        if (!map.containsKey(key)) {
            throw new NoCacheException();
        }
        CacheEntity<Data> cacheEntity = map.get(key);
        long currentTimeMillis = System.currentTimeMillis();
        if (cacheEntity.isExpired(currentTimeMillis)) {
            map.remove(key);
            throw new NoCacheException();
        }
        return CacheEntity.toDataPack(cacheEntity, this, currentTimeMillis);
    }

    @Override
    public void onCacheData(DataContext<Param> context, String key, DataPack<Data> data) {
        CacheEntity<Data> cacheEntity = CacheEntity.fromDataPack(data, System.currentTimeMillis(), mExpiry, mFastFailExpiry);
        map.put(key, cacheEntity);
    }

    @Override
    public void removeCache(DataContext<Param> context, String key) {
        map.remove(key);
    }

    @Override
    public void clearCache(String dataDesc) {
        map.clear();
    }

}
