package com.soybeany.util;

import java.io.*;

/**
 * 序列化工具类
 * <br>Created by Soybeany on 2020/10/19.
 */
public class SerializeUtils {

    public static byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             ObjectOutputStream os = new ObjectOutputStream(stream)) {
            os.writeObject(obj);
            return stream.toByteArray();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] arr) throws IOException, ClassNotFoundException {
        try (ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(arr))) {
            return (T) is.readObject();
        }
    }

}
