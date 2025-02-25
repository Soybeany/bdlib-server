package com.soybeany.util.transfer.core;

import java.io.IOException;

@SuppressWarnings("unused")
public interface IDataFrom<T> {
    default void onInit() {
    }

    void onTransfer(T out) throws IOException;

    interface WithRandomAccess<T> extends IDataFrom<T> {
        @Override
        default void onTransfer(T out) throws IOException {
            onTransfer(DataRange.from(0), out);
        }

        void onTransfer(DataRange range, T out) throws IOException;
    }
}
