package com.soybeany.cache.v2.exception;

/**
 * 没有找到数据时抛出的异常
 *
 * @author Soybeany
 * @date 2020/1/19
 */
public class DataException extends Exception {

    /**
     * 异常产生来源
     */
    public final Object provider;

    /**
     * 原始异常的全名
     */
    public final String originExceptionFullName;

    /**
     * 该数据剩余的有效时间
     */
    public final long expiryMillis;

    private Class<?> mOriginExceptionClass;
    private Exception mOriginException;

    public DataException(Object provider, String originExceptionFullName, String errMsg, long expiryMillis) {
        super(errMsg);
        this.provider = provider;
        this.originExceptionFullName = originExceptionFullName;
        this.expiryMillis = expiryMillis;
    }

    public DataException(Object provider, Exception originException, long expiryMillis) {
        this(provider, originException.getClass().getName(), originException.getMessage(), expiryMillis);
        mOriginExceptionClass = originException.getClass();
        mOriginException = originException;
    }

    /**
     * 获取原始异常的类
     */
    @SuppressWarnings("unchecked")
    public <T> Class<T> getOriginExceptionClass() {
        if (null != mOriginExceptionClass) {
            return (Class<T>) mOriginExceptionClass;
        }
        synchronized (this) {
            if (null == mOriginExceptionClass) {
                try {
                    mOriginExceptionClass = Class.forName(originExceptionFullName);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return (Class<T>) mOriginExceptionClass;
    }

    /**
     * 获取原始异常的实例
     */
    public Exception getOriginException() {
        if (null != mOriginException) {
            return mOriginException;
        }
        synchronized (this) {
            if (null == mOriginException) {
                try {
                    mOriginException = (Exception) getOriginExceptionClass()
                            .getConstructor(String.class)
                            .newInstance(getMessage());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return mOriginException;
    }

    /**
     * 原始异常是否与指定的异常相同
     */
    public boolean isOriginExceptionTheSameWith(Class<? extends Exception> clazz) {
        return originExceptionFullName.equals(clazz.getName());
    }

}
