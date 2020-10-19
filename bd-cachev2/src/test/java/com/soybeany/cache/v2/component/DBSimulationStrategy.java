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
    private final Map<String, DataHolder<Data>> map = new HashMap<String, DataHolder<Data>>();

    @Override
    public DataPack<Data> onGetCache(Param param, String key) throws DataException, NoCacheException {
        if (!map.containsKey(key)) {
            throw new NoCacheException();
        }
        DataHolder<Data> holder = map.get(key);
        long leftValidTime = holder.getLeftValidTime();
        if (DataHolder.isExpired(leftValidTime)) {
            map.remove(key);
            throw new NoCacheException();
        }
        if (holder.abnormal()) {
            throw new DataException(DataFrom.CACHE, holder.getException());
        }
        return DataPack.newCacheDataPack(this, holder.getData(), leftValidTime);
    }

    @Override
    public void onCacheData(Param param, String key, DataPack<Data> data) {
        map.put(key, DataHolder.get(data, mExpiry));
    }

    @Override
    public void onCacheException(Param param, String key, Exception e) {
        map.put(key, DataHolder.<Data>get(e, mFastFailExpiry));
    }

    @Override
    public void removeCache(Param param, String key) {
        map.remove(key);
    }

    @Override
    public void clearCache() {
        map.clear();
    }
}
