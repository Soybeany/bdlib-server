package com.soybeany.cache.v2.exception;

/**
 * 缓存相关的异常
 *
 * @author Soybeany
 * @date 2020/11/4
 */
public class NoCacheException extends Exception {
    public NoCacheException() {
        super("没有指定的缓存");
    }
}
