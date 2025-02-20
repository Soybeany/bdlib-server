package com.soybeany.util.transfer.core;

import com.soybeany.exception.BdIoException;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public interface IDataTo<T> {

    static String toErrMsg(Exception e) {
        return "[" + e.getClass().getName() + "]" + e.getMessage();
    }

    T onGetOutput(Map<String, Object> context) throws IOException;

    default void onSuccess(Map<String, Object> context) {
    }

    default void onFailure(Map<String, Object> context, Exception e) {
        throw new BdIoException(toErrMsg(e));
    }

    interface WithRandomAccess<T> extends IDataTo<T> {
        Optional<DataRange> onGetRange(Map<String, Object> context);
    }
}
