package com.soybeany.cache.v2.model;

import com.google.gson.Gson;
import com.soybeany.util.HexUtils;
import com.soybeany.util.SerializeUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * <br>Created by Soybeany on 2020/10/16.
 */
public class DataHolder<Data> {

    private static final Gson GSON = new Gson();
    private static final ICallback C_CALLBACK = new CImpl();
    private static final ICallback S_CALLBACK = new SImpl();

    private final boolean norm; // 是否为正常数据

    private final transient Data data; // 数据
    private final transient Exception exception; // 相关的异常
    private final long expiry; // 超时

    private final long mCreateStamp; // 创建时的时间戳

    private Map<String, Info> cJsons; // 用于存放对象的json(类名)
    private Map<String, Info> sJsons; // 用于存放对象的json(序列化)

    public static <Data> String toJson(DataHolder<Data> holder, Type dataType) throws NoSuchFieldException, IllegalAccessException, IOException {
        holder.addJson("data", dataType);
        holder.addJson("exception", null);
        String json = GSON.toJson(holder);
        holder.release();
        return json;
    }

    @SuppressWarnings("unchecked")
    public static <Data> DataHolder<Data> fromJson(String json) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, IOException {
        DataHolder<Data> holder = GSON.fromJson(json, DataHolder.class);
        holder.parseJsons(holder.cJsons, C_CALLBACK);
        holder.parseJsons(holder.sJsons, S_CALLBACK);
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

    private void addJson(String fieldName, Type type) throws NoSuchFieldException, IllegalAccessException, IOException {
        Object value = DataHolder.class.getDeclaredField(fieldName).get(this);
        if (null == value) {
            return;
        }
        if (null == type) {
            type = value.getClass();
        }
        if (type instanceof ParameterizedType) {
            if (null == sJsons) {
                sJsons = new HashMap<String, Info>();
            }
            String clazz = HexUtils.bytesToHex(SerializeUtils.serialize(type));
            sJsons.put(fieldName, new Info(clazz, GSON.toJson(value)));
        } else if (type instanceof Class) {
            if (null == cJsons) {
                cJsons = new HashMap<String, Info>();
            }
            cJsons.put(fieldName, new Info(((Class<?>) type).getName(), GSON.toJson(value)));
        } else {
            throw new RuntimeException("使用了不支持的type");
        }
    }

    private void parseJsons(Map<String, Info> jsons, ICallback callback) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException, IOException {
        if (null == jsons) {
            return;
        }
        for (Map.Entry<String, Info> entry : jsons.entrySet()) {
            Field field = DataHolder.class.getDeclaredField(entry.getKey());
            field.setAccessible(true);
            Info info = entry.getValue();
            Type type = callback.onGetType(info);
            field.set(this, GSON.fromJson(info.json, type));
        }
    }

    private void release() {
        cJsons = null;
        sJsons = null;
    }

    // ****************************************内部类****************************************

    private interface ICallback {
        Type onGetType(Info info) throws ClassNotFoundException, IOException;
    }

    private static class CImpl implements ICallback {
        @Override
        public Type onGetType(Info info) throws ClassNotFoundException {
            return Class.forName(info.clazz);
        }
    }

    private static class SImpl implements ICallback {
        @Override
        public Type onGetType(Info info) throws ClassNotFoundException, IOException {
            return SerializeUtils.deserialize(HexUtils.hexToByteArray(info.clazz));
        }
    }

    private static class Info {
        public String clazz;
        public String json;

        public Info(String clazz, String json) {
            this.clazz = clazz;
            this.json = json;
        }
    }

}
