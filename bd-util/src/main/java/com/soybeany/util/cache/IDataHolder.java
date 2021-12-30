package com.soybeany.util.cache;

import java.util.Collection;

/**
 * <br>Created by Soybeany on 2021/3/23.
 */
public interface IDataHolder<T> {

    T put(String key, T data, int expiryInSec);

    T get(String key);

    T remove(String key);

    Collection<T> getAll();

    void clear();

}
