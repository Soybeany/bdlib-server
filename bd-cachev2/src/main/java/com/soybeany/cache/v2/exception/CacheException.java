package com.soybeany.cache.v2.exception;

/**
 * 缓存相关的异常
 *
 * @author Soybeany
 * @date 2020/10/9
 */
public class CacheException extends DataException {
    public CacheException() {
        this("缓存异常");
    }

    public CacheException(String msg) {
        super(msg);
    }
}
