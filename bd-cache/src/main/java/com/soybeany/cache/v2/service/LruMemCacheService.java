package com.soybeany.cache.v2.service;


import com.soybeany.cache.v2.exception.CacheAntiPenetrateException;
import com.soybeany.cache.v2.exception.CacheException;
import com.soybeany.cache.v2.exception.CacheNoDataException;
import com.soybeany.cache.v2.module.DataPack;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * 使用LRU策略的本地内存缓存
 *
 * @author Soybeany
 * @date 2020/1/19
 */
public class LruMemCacheService<Param, Data> extends BaseMemCacheService<Param, Data> {

    private final LruDataAccessor<DataHolder<Data>> mDataAccessor = new LruDataAccessor<DataHolder<Data>>();

    @Override
    public String getId() {
        return "MEM_LRU";
    }

    @Override
    public boolean supportDoubleCheck() {
        return true;
    }

    @Override
    public DataPack<Data> onRetrieveCachedData(String dataGroup, Param param, String key) throws CacheException {
        DataHolder<Data> holder = mDataAccessor.get(key);
        // 若缓存中没有数据，则直接抛出无数据异常
        if (null == holder) {
            throw new CacheNoDataException();
        }
        // 若缓存中的数据过期，则移除数据后抛出无数据异常
        boolean withDataExpired = holder.hasData && isStampExpired(holder.stamp, mExpiry);
        boolean withoutDataExpired = !holder.hasData && isStampExpired(holder.stamp, mNoDataExpiry);
        if (withDataExpired || withoutDataExpired) {
            mDataAccessor.removeData(key);
            throw new CacheNoDataException();
        }
        // 数据依旧有效，则移到队列末尾
        mDataAccessor.moveDataToLast(key);
        // 没有数据，则抛出防穿透异常
        if (!holder.hasData) {
            throw new CacheAntiPenetrateException();
        }
        // 返回正常缓存的数据
        return DataPack.newCacheDataPack(holder.data, true);
    }

    @Override
    public void onCacheData(String dataGroup, Param param, String key, Data data) {
        mDataAccessor.putData(key, DataHolder.get(data));
    }

    @Override
    public void onNoDataToCache(String dataGroup, Param param, String key) {
        mDataAccessor.putData(key, new DataHolder<Data>(null, false));
    }

    @Override
    public void removeCache(String dataGroup, Param param, String key) {
        mDataAccessor.removeData(key);
    }

    @Override
    public void clearCache(String dataGroup) {
        mDataAccessor.clear();
    }

    public LruMemCacheService<Param, Data> capacity(int size) {
        mDataAccessor.capacity = size;
        return this;
    }

    private static class LruDataAccessor<Data> {
        private final Map<String, Data> mCacheMap = new HashMap<String, Data>();
        private final LinkedList<String> mOrderList = new LinkedList<String>(); // 用于记录访问顺序

        int capacity = 100;

        Data get(String key) {
            return mCacheMap.get(key);
        }

        synchronized void moveDataToLast(String key) {
            mOrderList.remove(key);
            mOrderList.offer(key);
        }

        synchronized void removeData(String key) {
            mOrderList.remove(key);
            mCacheMap.remove(key);
        }

        synchronized void putData(String key, Data data) {
            // 若存储的数据已达到设置的上限，先移除末位数据
            if (mOrderList.size() >= capacity && !mOrderList.isEmpty()) {
                removeData(mOrderList.getFirst());
            }
            mOrderList.offer(key);
            mCacheMap.put(key, data);
        }

        synchronized void clear() {
            mOrderList.clear();
            mCacheMap.clear();
        }
    }
}
