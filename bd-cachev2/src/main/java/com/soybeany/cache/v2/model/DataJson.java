package com.soybeany.cache.v2.model;

import com.google.gson.Gson;
import com.soybeany.cache.v2.exception.DataException;

import java.lang.reflect.Type;

/**
 * <br>Created by Soybeany on 2020/10/16.
 */
public class DataJson<Data> {

    private static final Gson GSON = new Gson();

    private final boolean norm; // 是否为正常数据

    private transient Data data; // 数据
    private transient Exception exception; // 相关的异常

    private String dataJson;
    private Info exceptionJson;

    // ********************Json********************

    public static <Data> String toJson(DataJson<Data> dataJson) {
        if (dataJson.norm) {
            dataJson.dataJson = GSON.toJson(dataJson.data);
        } else {
            String exceptionClazz = dataJson.exception.getClass().getName();
            String exceptionJson = GSON.toJson(dataJson.exception);
            dataJson.exceptionJson = new Info(exceptionClazz, exceptionJson);
        }
        String json = GSON.toJson(dataJson);
        dataJson.release();
        return json;
    }

    @SuppressWarnings("unchecked")
    public static <Data> DataJson<Data> fromJson(String json, Type dataType) throws ClassNotFoundException {
        DataJson<Data> holder = GSON.fromJson(json, DataJson.class);
        if (holder.norm) {
            holder.data = GSON.fromJson(holder.dataJson, dataType);
        } else {
            Info info = holder.exceptionJson;
            holder.exception = (Exception) GSON.fromJson(info.json, Class.forName(info.clazz));
        }
        holder.release();
        return holder;
    }

    // ********************DataHolder********************

    public static <Data> DataHolder<Data> toDataHolder(DataJson<Data> dataJson, long expiryMillis) {
        return new DataHolder<>(dataJson.norm, dataJson.data, dataJson.exception, expiryMillis);
    }

    public static <Data> DataJson<Data> fromDataHolder(DataHolder<Data> holder) {
        return new DataJson<>(holder.data, holder.exception, holder.norm);
    }

    // ********************DataPack********************

    public static <Data> DataJson<Data> fromDataPack(DataPack<Data> dataPack) {
        return fromData(dataPack.data);
    }

    // ********************DataException********************

    public static <Data> DataJson<Data> fromDataException(DataException exception) {
        return fromException(exception.getOriginException());
    }

    // ********************Data********************

    public static <Data> DataJson<Data> fromData(Data data) {
        return new DataJson<>(data, null, true);
    }

    // ********************Exception********************

    public static <Data> DataJson<Data> fromException(Exception exception) {
        return new DataJson<>(null, exception, false);
    }

    public DataJson(Data data, Exception exception, boolean norm) {
        this.exception = exception;
        this.norm = norm;
        this.data = data;
    }

    public boolean normal() {
        return norm;
    }

    public Data getData() {
        return data;
    }

    public Exception getException() {
        return exception;
    }

    // ****************************************内部方法****************************************

    private void release() {
        dataJson = null;
        exceptionJson = null;
    }

    // ****************************************内部类****************************************

    private static class Info {
        public final String clazz;
        public final String json;

        public Info(String clazz, String json) {
            this.clazz = clazz;
            this.json = json;
        }
    }

}
