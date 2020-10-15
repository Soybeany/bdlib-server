package com.soybeany.cache.v2.exception;

/**
 * 缓存用于预防穿透而抛出的异常
 *
 * @author Soybeany
 * @date 2020/1/22
 */
public class CacheAntiPenetrateException extends CacheException {
    public CacheAntiPenetrateException() {
        super("缓存防穿透");
    }
}
