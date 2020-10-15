package com.soybeany.cache.v2.strategy;


import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.model.DataFrom;
import com.soybeany.cache.v2.model.DataPack;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * 使用LRU策略的本地内存缓存
 *
 * @author Soybeany
 * @date 2020/1/19
 */
public class LruMemCacheStrategy<Param, Data> extends BaseMemCacheStrategy<Param, Data> {

    private final LruDataAccessor<DataHolder<Data>> mDataAccessor = new LruDataAccessor<DataHolder<Data>>();

    @Override
    public String getName() {
        return "MEM_LRU";
    }

    @Override
    public boolean supportDoubleCheck() {
        return true;
    }

    @Override
    public DataPack<Data> onGetCache(Param param, String key) throws DataException, NoCacheException {
        DataHolder<Data> holder = mDataAccessor.get(key);
        // 若缓存中没有数据，则直接抛出无数据异常
        if (null == holder) {
            throw new NoCacheException();
        }
        // 若缓存中的数据过期，则移除数据后抛出无数据异常
        boolean withDataExpired = holder.isNorm && isStampExpired(holder.stamp, mExpiry);
        boolean withoutDataExpired = !holder.isNorm && isStampExpired(holder.stamp, mFastFailExpiry);
        if (withDataExpired || withoutDataExpired) {
            mDataAccessor.removeData(key);
            throw new NoCacheException();
        }
        // 数据依旧有效，则移到队列末尾
        mDataAccessor.moveDataToLast(key);
        // 没有数据，则抛出防穿透异常
        if (!holder.isNorm) {
            throw new DataException(DataFrom.CACHE, holder.exception);
        }
        // 返回正常缓存的数据
        return DataPack.newCacheDataPack(holder.data);
    }

    @Override
    public void onCacheData(Param param, String key, Data data) {
        mDataAccessor.putData(key, DataHolder.get(data));
    }

    @Override
    public void onCacheException(Param param, String key, Exception e) {
        mDataAccessor.putData(key, DataHolder.<Data>get(e));
    }

    @Override
    public void removeCache(Param param, String key) {
        mDataAccessor.removeData(key);
    }

    @Override
    public void clearCache() {
        mDataAccessor.clear();
    }

    public LruMemCacheStrategy<Param, Data> capacity(int size) {
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
