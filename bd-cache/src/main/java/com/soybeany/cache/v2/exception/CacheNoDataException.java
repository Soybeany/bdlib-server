package com.soybeany.cache.v2.exception;

/**
 * 缓存中没有指定数据时抛出的异常
 *
 * @author Soybeany
 * @date 2020/1/22
 */
public class CacheNoDataException extends CacheException {
    public CacheNoDataException() {
        super("缓存没有指定的数据");
    }
}
