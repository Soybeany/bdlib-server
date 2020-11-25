package com.soybeany.cache.v2.exception;

/**
 * 没有找到数据时抛出的异常
 *
 * @author Soybeany
 * @date 2020/1/19
 */
public class DataException extends Exception {

    public final Exception originException;

    /**
     * 异常产生来源
     */
    public final Object provider;

    /**
     * 该数据剩余的有效时间
     */
    public final long expiryMillis;

    public DataException(Exception originException, Object provider, long expiryMillis) {
        super(originException.getMessage());
        this.originException = originException;
        this.provider = provider;
        this.expiryMillis = expiryMillis;
    }

    /**
     * 原始异常是否与指定的异常相同
     */
    public boolean isOriginExceptionTheSameWith(Class<? extends Exception> clazz) {
        return originException.getClass().equals(clazz);
    }

}
