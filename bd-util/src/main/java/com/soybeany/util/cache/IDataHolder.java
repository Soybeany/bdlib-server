package com.soybeany.util.cache;

/**
 * <br>Created by Soybeany on 2021/3/23.
 */
public interface IDataHolder<T> {

    void put(String key, T data, int expiryInSec);

    T updateAndGet(String key);

    void remove(String key);

    void clear();

}
