package com.soybeany.cache.v2.model;

import com.google.gson.Gson;

import java.lang.reflect.Type;

/**
 * <br>Created by Soybeany on 2020/10/16.
 */
public class DataHolder<Data> {

    private static final Gson GSON = new Gson();

    private final boolean norm; // 是否为正常数据

    private transient Data data; // 数据
    private transient Exception exception; // 相关的异常
    private long expiry; // 超时

    private final long mCreateStamp; // 创建时的时间戳

    private String dataJson;
    private Info exceptionJson;

    public static <Data> String toJson(DataHolder<Data> holder) {
        if (holder.norm) {
            holder.dataJson = GSON.toJson(holder.data);
        } else {
            String exceptionClazz = holder.exception.getClass().getName();
            String exceptionJson = GSON.toJson(holder.exception);
            holder.exceptionJson = new Info(exceptionClazz, exceptionJson);
        }
        String json = GSON.toJson(holder);
        holder.release();
        return json;
    }

    @SuppressWarnings("unchecked")
    public static <Data> DataHolder<Data> fromJson(String json, Type dataType) throws ClassNotFoundException {
        DataHolder<Data> holder = GSON.fromJson(json, DataHolder.class);
        if (holder.norm) {
            holder.data = GSON.fromJson(holder.dataJson, dataType);
        } else {
            Info info = holder.exceptionJson;
            holder.exception = (Exception) GSON.fromJson(info.json, Class.forName(info.clazz));
        }
        holder.release();
        return holder;
    }

    public static <Data> DataHolder<Data> get(DataPack<Data> data, long expiryInMills) {
        return new DataHolder<Data>(data, null, true, expiryInMills);
    }

    public static <Data> DataHolder<Data> get(Exception exception, long expiryInMills) {
        return new DataHolder<Data>(null, exception, false, expiryInMills);
    }

    public static boolean isExpired(long leftValidTime) {
        return leftValidTime < 0;
    }

    public DataHolder(DataPack<Data> data, Exception exception, boolean norm, long expiryInMills) {
        this(data, exception, norm, expiryInMills, System.currentTimeMillis());
    }

    public DataHolder(DataPack<Data> data, Exception exception, boolean norm, long expiryInMills, long createStamp) {
        this.exception = exception;
        this.norm = norm;

        if (null != data) {
            this.data = data.data;
            this.expiry = Math.min(data.expiryInMills, expiryInMills);
        } else {
            this.data = null;
            this.expiry = expiryInMills;
        }
        this.mCreateStamp = createStamp;
    }

    public boolean abnormal() {
        return !norm;
    }

    public Data getData() {
        return data;
    }

    public Exception getException() {
        return exception;
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    public long getmCreateStamp() {
        return mCreateStamp;
    }

    /**
     * 剩余的有效时间
     */
    public long getLeftValidTime() {
        return getLeftValidTime(System.currentTimeMillis());
    }

    public long getLeftValidTime(long curTimeMills) {
        return expiry - (curTimeMills - mCreateStamp);
    }

    /**
     * 判断此数据是否已经失效
     */
    public boolean isExpired() {
        return isExpired(getLeftValidTime());
    }

    // ****************************************内部方法****************************************

    private void release() {
        dataJson = null;
        exceptionJson = null;
    }

    // ****************************************内部类****************************************

    private static class Info {
        public String clazz;
        public String json;

        public Info(String clazz, String json) {
            this.clazz = clazz;
            this.json = json;
        }
    }

}
