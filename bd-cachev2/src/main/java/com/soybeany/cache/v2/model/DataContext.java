package com.soybeany.cache.v2.model;

/**
 * 数据上下文
 *
 * @author Soybeany
 * @date 2020/11/19
 */
public class DataContext<Param> {

    public final String dataDesc;
    public final String paramDesc;
    public final Purpose purpose;
    public final Param param;

    public DataContext(String dataDesc, String paramDesc, Purpose purpose, Param param) {
        this.dataDesc = dataDesc;
        this.paramDesc = paramDesc;
        this.purpose = purpose;
        this.param = param;
    }

    public enum Purpose {
        // 一系列的目的
        GET_DATA, GET_CACHE, CACHE_DATA, CACHE_EXCEPTION, REMOVE_CACHE
    }

}
