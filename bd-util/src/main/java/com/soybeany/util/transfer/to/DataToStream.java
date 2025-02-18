package com.soybeany.util.transfer.to;

import com.soybeany.util.transfer.core.IDataTo;

import java.io.BufferedOutputStream;
import java.io.OutputStream;

public class DataToStream implements IDataTo<OutputStream> {
    private final OutputStream os;

    public DataToStream(OutputStream os) {
        this.os = os;
    }

    @Override
    public OutputStream onGetOutput() {
        return new BufferedOutputStream(os);
    }
}
