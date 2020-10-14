package com.soybeany.reflection;

/**
 * @author Soybeany
 * @date 2020/10/14
 */
public class ClassReflectUtils {

    @SuppressWarnings("unchecked")
    public static <T> T get(String classFullName) throws ClassNotFoundException {
        return (T) Class.forName(classFullName);
    }

}
