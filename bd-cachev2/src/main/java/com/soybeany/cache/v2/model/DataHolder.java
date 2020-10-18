package com.soybeany.cache.v2.model;

import com.google.gson.Gson;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * <br>Created by Soybeany on 2020/10/16.
 */
public class DataHolder<Data> {

    private static final Gson GSON = new Gson();

    private final boolean norm; // 是否为正常数据

    private final transient Data data; // 数据
    private final transient Exception exception; // 相关的异常
    private final long expiry; // 超时

    private final long mCreateStamp; // 创建时的时间戳

    private final Map<String, Info> jsons = new HashMap<String, Info>(); // 用于存放对象的json，以正确处理多态问题

    public static <Data> String toJson(DataHolder<Data> holder) throws IllegalAccessException {
        for (Field field : DataHolder.class.getDeclaredFields()) {
            if (!Modifier.isTransient(field.getModifiers())) {
                continue;
            }
            Object value = field.get(holder);
            if (null == value) {
                continue;
            }
            holder.jsons.put(field.getName(), new Info(value.getClass().getName(), GSON.toJson(value)));
        }
        return GSON.toJson(holder);
    }

    @SuppressWarnings("unchecked")
    public static <Data> DataHolder<Data> fromJson(String json) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        DataHolder<Data> holder = GSON.fromJson(json, DataHolder.class);
        for (Map.Entry<String, Info> entry : holder.jsons.entrySet()) {
            Field field = DataHolder.class.getDeclaredField(entry.getKey());
            field.setAccessible(true);
            Info info = entry.getValue();
            field.set(holder, GSON.fromJson(info.json, Class.forName(info.clazz)));
        }
        holder.jsons.clear();
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
        this.exception = exception;
        this.norm = norm;

        if (null != data) {
            this.data = data.data;
            this.expiry = Math.min(data.expiryInMills, expiryInMills);
        } else {
            this.data = null;
            this.expiry = expiryInMills;
        }
        this.mCreateStamp = System.currentTimeMillis();
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

    /**
     * 剩余的有效时间
     */
    public long getLeftValidTime() {
        return expiry - (System.currentTimeMillis() - mCreateStamp);
    }

    /**
     * 判断此数据是否已经失效
     */
    public boolean isExpired() {
        return isExpired(getLeftValidTime());
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
