package com.soybeany.bypass;

/**
 * @author Soybeany
 * @date 2020/10/14
 */
public class ClassReflectUtils {

    @SuppressWarnings("unchecked")
    public static <T> Class<T> get(String classFullName) throws ClassNotFoundException {
        return (Class<T>) Class.forName(classFullName);
    }

}
