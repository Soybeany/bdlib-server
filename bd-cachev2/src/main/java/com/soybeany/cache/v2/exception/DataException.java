package com.soybeany.cache.v2.exception;

import com.soybeany.cache.v2.model.DataFrom;

/**
 * 没有找到数据时抛出的异常
 *
 * @author Soybeany
 * @date 2020/1/19
 */
public class DataException extends Exception {

    private final DataFrom mDataFrom;
    private final String mOriginExceptionFullName;

    private Class<?> mOriginExceptionClass;
    private Exception mOriginException;

    public DataException(DataFrom dataFrom, String originExceptionFullName, String errMsg) {
        super(errMsg);
        mDataFrom = dataFrom;
        mOriginExceptionFullName = originExceptionFullName;
    }

    public DataException(DataFrom dataFrom, Exception originException) {
        super(originException.getMessage());
        mDataFrom = dataFrom;
        mOriginException = originException;
        mOriginExceptionClass = originException.getClass();
        mOriginExceptionFullName = mOriginExceptionClass.getName();
    }

    public DataFrom getDataFrom() {
        return mDataFrom;
    }

    /**
     * 获取原始异常的全名称
     */
    public String getOriginExceptionFullName() {
        return mOriginExceptionFullName;
    }

    /**
     * 获取原始异常的类
     */
    @SuppressWarnings("unchecked")
    public <T> Class<T> getOriginExceptionClass() {
        if (null == mOriginExceptionClass) {
            synchronized (this) {
                if (null == mOriginExceptionClass) {
                    try {
                        mOriginExceptionClass = Class.forName(mOriginExceptionFullName);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return (Class<T>) mOriginExceptionClass;
    }

    /**
     * 获取原始异常的实例
     */
    public Exception getOriginException() {
        if (null == mOriginException) {
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
        }
        return mOriginException;
    }

    /**
     * 原始异常是否与指定的异常相同
     */
    public boolean isOriginExceptionTheSameWith(Class<Exception> clazz) {
        return mOriginExceptionFullName.equals(clazz.getName());
    }

}
