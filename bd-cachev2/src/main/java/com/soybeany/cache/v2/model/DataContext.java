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
    public final Param param;

    // todo 引入method，修改tmp_cache

    public DataContext(String dataDesc, String paramDesc, Param param) {
        this.dataDesc = dataDesc;
        this.paramDesc = paramDesc;
        this.param = param;
    }
}
