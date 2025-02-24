package com.soybeany.util.transfer.core;

import java.io.IOException;

@SuppressWarnings("unused")
public interface IDataFrom<T> {
    default void onInit() {
    }

    void onTransfer(T out) throws IOException;

    interface WithRandomAccess<T> extends IDataFrom<T> {
        void onTransfer(DataRange range, T out) throws IOException;
    }
}
