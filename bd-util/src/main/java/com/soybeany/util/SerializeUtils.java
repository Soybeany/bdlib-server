package com.soybeany.util;

import java.io.*;

/**
 * 序列化工具类
 * <br>Created by Soybeany on 2020/10/19.
 */
public class SerializeUtils {

    public static byte[] serialize(Object obj) throws IOException {
        ObjectOutputStream objOStream = null;
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            objOStream = new ObjectOutputStream(stream);
            objOStream.writeObject(obj);
            return stream.toByteArray();
        } finally {
            if (objOStream != null) {
                objOStream.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T deserialize(byte[] arr) throws IOException, ClassNotFoundException {
        try (ObjectInputStream objIStream = new ObjectInputStream(new ByteArrayInputStream(arr))) {
            return (T) objIStream.readObject();
        }
    }

}
