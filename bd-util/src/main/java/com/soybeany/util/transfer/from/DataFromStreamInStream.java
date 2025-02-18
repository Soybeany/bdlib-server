package com.soybeany.util.transfer.from;

import com.soybeany.util.file.BdFileUtils;
import com.soybeany.util.transfer.core.IDataFrom;

import java.io.InputStream;
import java.io.OutputStream;

public class DataFromStreamInStream implements IDataFrom<OutputStream> {
    private final InputStream is;

    public DataFromStreamInStream(InputStream is) {
        this.is = is;
    }

    @Override
    public void onTransfer(OutputStream out) {
        BdFileUtils.readWriteStreamNoBuffer(is, out);
    }
}
