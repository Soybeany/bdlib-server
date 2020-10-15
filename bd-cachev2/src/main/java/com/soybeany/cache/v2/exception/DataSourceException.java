package com.soybeany.cache.v2.exception;

/**
 * 数据源相关的异常
 *
 * @author Soybeany
 * @date 2020/10/9
 */
public class DataSourceException extends DataException {
    public DataSourceException() {
        this("数据源异常");
    }

    public DataSourceException(String msg) {
        super(msg);
    }
}
