package com.soybeany.cache.v2.exception;

/**
 * <br>Created by Soybeany on 2020/10/15.
 */
public class NoCacheException extends Exception {
    public NoCacheException() {
        super("没有找到缓存");
    }
}
