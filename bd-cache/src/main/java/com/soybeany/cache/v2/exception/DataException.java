package com.soybeany.cache.v2.exception;

/**
 * 没有找到数据时抛出的异常
 *
 * @author Soybeany
 * @date 2020/1/19
 */
public class DataException extends Exception {
    public DataException() {
        this("数据异常");
    }

    public DataException(String msg) {
        super(msg);
    }
}
