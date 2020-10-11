package com.soybeany.cache.v2.exception;

/**
 * 数据源没有指定的数据时抛出的异常，如尝试在数据源中获取一个不存在的资源
 *
 * @author Soybeany
 * @date 2020/10/9
 */
public class DataSourceNoDataException extends DataSourceException {
    public DataSourceNoDataException() {
        super("数据源没有指定的数据");
    }
}
