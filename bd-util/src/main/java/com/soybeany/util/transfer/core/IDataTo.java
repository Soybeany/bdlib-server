package com.soybeany.util.transfer.core;

import com.soybeany.exception.BdIoException;

import java.io.IOException;
import java.util.Optional;

public interface IDataTo<T> {

    static String toErrMsg(Exception e) {
        return "[" + e.getClass().getName() + "]" + e.getMessage();
    }

    default void onInit() {
    }

    T onGetOutput() throws IOException;

    default void onSuccess() {
    }

    default void onFailure(Exception e) {
        throw new BdIoException(toErrMsg(e));
    }

    interface WithRandomAccess<T> extends IDataTo<T> {
        Optional<DataRange> onGetRange();
    }
}
