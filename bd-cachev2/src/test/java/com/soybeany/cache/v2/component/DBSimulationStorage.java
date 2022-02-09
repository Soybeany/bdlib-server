package com.soybeany.cache.v2.component;

import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.model.CacheEntity;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.storage.StdStorageBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * <br>Created by Soybeany on 2020/10/16.
 */
public class DBSimulationStorage<Param, Data> extends StdStorageBuilder.StdStorage<Param, Data> {
    private final Map<String, CacheEntity<Data>> map = new HashMap<>();

    public DBSimulationStorage() {
        this(Integer.MAX_VALUE);
    }

    public DBSimulationStorage(int pTtl) {
        super(pTtl, 60 * 1000);
    }

    @Override
    public String desc() {
        return "仿数据库";
    }

    @Override
    protected CacheEntity<Data> onLoadCacheEntity(DataContext<Param> context, String key) throws NoCacheException {
        if (!map.containsKey(key)) {
            throw new NoCacheException();
        }
        return map.get(key);
    }

    @Override
    protected CacheEntity<Data> onSaveCacheEntity(DataContext<Param> context, String key, CacheEntity<Data> entity) {
        map.put(key, entity);
        return entity;
    }

    @Override
    protected long onGetCurTimestamp() {
        return System.currentTimeMillis();
    }

    @Override
    public void onRemoveCache(DataContext<Param> context, String key) {
        map.remove(key);
    }

    @Override
    public void onClearCache() {
        map.clear();
    }

    @Override
    public int cacheSize() {
        return map.size();
    }

}
