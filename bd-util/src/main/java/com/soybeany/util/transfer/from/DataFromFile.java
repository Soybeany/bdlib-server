package com.soybeany.util.transfer.from;

import com.soybeany.util.file.BdFileUtils;
import com.soybeany.util.transfer.core.DataRange;
import com.soybeany.util.transfer.core.IDataFrom;

import java.io.File;
import java.io.OutputStream;

public class DataFromFile implements IDataFrom.WithRandomAccess<OutputStream> {
    private final File file;

    public DataFromFile(File file) {
        this.file = file;
    }

    @Override
    public void onTransfer(DataRange range, OutputStream os) {
        BdFileUtils.randomRead(file, range.start, range.end, os);
    }
}
