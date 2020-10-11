package com.soybeany.cache.v2.exception;

/**
 * 数据源无法访问时抛出的异常
 * <br>如网络请求时，网络连接异常
 *
 * @author Soybeany
 * @date 2020/10/9
 */
public class DataSourceAccessException extends DataSourceException {
    public DataSourceAccessException() {
        super("数据源连接异常");
    }
}
