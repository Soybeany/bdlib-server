package com.soybeany.util.transfer.to;

import com.soybeany.util.transfer.core.IDataTo;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.util.Map;

public class DataToStream implements IDataTo<OutputStream> {
    private final OutputStream os;

    public DataToStream(OutputStream os) {
        this.os = os;
    }

    @Override
    public OutputStream onGetOutput(Map<String, Object> context) {
        return new BufferedOutputStream(os);
    }
}
