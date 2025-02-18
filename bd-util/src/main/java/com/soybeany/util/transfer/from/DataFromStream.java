package com.soybeany.util.transfer.from;

import com.soybeany.util.file.BdFileUtils;
import com.soybeany.util.transfer.core.IDataFrom;

import java.io.InputStream;
import java.io.OutputStream;

public class DataFromStream implements IDataFrom<OutputStream> {
    private final InputStream is;

    public DataFromStream(InputStream is) {
        this.is = is;
    }

    @Override
    public void onTransfer(OutputStream out) {
        BdFileUtils.readWriteStreamNoBuffer(is, out);
    }
}
