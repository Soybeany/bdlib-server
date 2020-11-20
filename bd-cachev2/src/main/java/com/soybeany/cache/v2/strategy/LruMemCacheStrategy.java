package com.soybeany.cache.v2.strategy;


import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataHolder;
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
public class LruMemCacheStrategy<Param, Data> extends StdCacheStrategy<Param, Data> {

    private static final String KEY_PREFIX_NORM = "norm";
    private static final String KEY_PREFIX_TEMP = "temp";

    private final LruDataAccessor<DataHolder<Data>> mDataAccessor = new LruDataAccessor<>();

    private long mTempExpiry = 10 * 1000; // 10秒

    @Override
    public String desc() {
        return "LRU";
    }

    @Override
    public void removeCache(DataContext<Param> context, String key) {
        mDataAccessor.removeData(KEY_PREFIX_NORM, key);
    }

    @Override
    public void clearCache(String dataDesc) {
        mDataAccessor.clear();
    }

    @Override
    protected DataPack<Data> onGetCache(DataContext<Param> context, String key) throws DataException, NoCacheException {
        return innerGetCache(KEY_PREFIX_NORM, key);
    }

    @Override
    protected void onCacheData(DataContext<Param> context, String key, DataPack<Data> data) {
        mDataAccessor.putData(KEY_PREFIX_NORM, key, DataHolder.get(data, mExpiry));
    }

    @Override
    protected void onCacheException(DataContext<Param> context, String key, Object producer, Exception e) {
        mDataAccessor.putData(KEY_PREFIX_NORM, key, DataHolder.get(e, mFastFailExpiry));
    }

    @Override
    protected DataPack<Data> onGetCacheWithGetCacheChannel(DataContext<Param> context, String key) throws DataException, NoCacheException {
        return innerGetCache(KEY_PREFIX_TEMP, key);
    }

    @Override
    protected void onCacheExceptionWithGetCacheChannel(DataContext<Param> context, String key, Object producer, Exception e) {
        mDataAccessor.putData(KEY_PREFIX_TEMP, key, DataHolder.get(e, mTempExpiry));
    }

    /**
     * 设置用于存放数据的队列容量
     */
    public LruMemCacheStrategy<Param, Data> capacity(int size) {
        mDataAccessor.capacity = size;
        return this;
    }

    /**
     * 设置临时类数据的超时，如只查缓存时，对下级异常缓存的时间
     */
    public LruMemCacheStrategy<Param, Data> tempExpiry(long millis) {
        mTempExpiry = millis;
        return this;
    }

    // ********************内部方法********************

    private DataPack<Data> innerGetCache(String keyPrefix, String key) throws DataException, NoCacheException {
        DataHolder<Data> dataHolder = mDataAccessor.get(keyPrefix, key);
        // 若缓存中没有数据，则直接抛出无数据异常
        if (null == dataHolder) {
            throw new NoCacheException();
        }
        // 若缓存中的数据过期，则移除数据后抛出无数据异常
        if (dataHolder.isExpired()) {
            mDataAccessor.removeData(keyPrefix, key);
            throw new NoCacheException();
        }
        // 数据依旧有效，则移到队列末尾
        mDataAccessor.moveDataToLast(keyPrefix, key);
        // 返回数据
        return dataHolder.toDataPack(this);
    }

    // ********************内部类********************

    private static class LruDataAccessor<Data> {
        private final Map<String, Data> mCacheMap = new HashMap<>();
        private final LinkedList<String> mOrderList = new LinkedList<>(); // 用于记录访问顺序

        int capacity = 100;

        Data get(String keyPrefix, String key) {
            key = getFullKey(keyPrefix, key);
            return mCacheMap.get(key);
        }

        synchronized void moveDataToLast(String keyPrefix, String key) {
            key = getFullKey(keyPrefix, key);
            mOrderList.remove(key);
            mOrderList.offer(key);
        }

        synchronized void removeData(String keyPrefix, String key) {
            key = getFullKey(keyPrefix, key);
            mOrderList.remove(key);
            mCacheMap.remove(key);
        }

        synchronized void putData(String keyPrefix, String key, Data data) {
            key = getFullKey(keyPrefix, key);
            // 若存储的数据已达到设置的上限，先移除末位数据
            if (mOrderList.size() >= capacity && !mOrderList.isEmpty()) {
                removeData("", mOrderList.getFirst());
            }
            mOrderList.offer(key);
            mCacheMap.put(key, data);
        }

        private String getFullKey(String keyPrefix, String key) {
            return keyPrefix + ":" + key;
        }

        synchronized void clear() {
            mOrderList.clear();
            mCacheMap.clear();
        }
    }
}
