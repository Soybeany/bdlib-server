package com.soybeany.cache.v2.model;

/**
 * 节点间用于通讯的对象
 * <br>Created by Soybeany on 2020/11/24.
 */
public class DataPack<Data> {

    /**
     * 目标数据缓存
     */
    public final DataCore<Data> dataCore;

    /**
     * 数据的提供者，即数据最近一次的提供者
     */
    public final Object provider;

    /**
     * 该数据剩余的有效时间(时间段)
     */
    public final int remainValidMillis;

    public DataPack(DataCore<Data> dataCore, Object provider, int remainValidMillis) {
        this.dataCore = dataCore;
        this.provider = provider;
        this.remainValidMillis = Math.max(remainValidMillis, 0);
    }

    public Data getData() throws Exception {
        if (norm()) {
            return dataCore.data;
        }
        throw dataCore.exception;
    }

    public boolean norm() {
        return dataCore.norm;
    }

}
