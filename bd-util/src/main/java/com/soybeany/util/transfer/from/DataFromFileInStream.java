package com.soybeany.util.transfer.from;

import com.soybeany.util.file.BdFileUtils;
import com.soybeany.util.transfer.core.DataRange;
import com.soybeany.util.transfer.core.IDataFrom;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class DataFromFileInStream implements IDataFrom.WithRandomAccess<OutputStream> {
    private final File file;

    public DataFromFileInStream(File file) {
        this.file = file;
    }

    @Override
    public void onTransfer(DataRange range, OutputStream os) {
        BdFileUtils.randomRead(file, range.start, range.end, os);
    }

    @Override
    public void onTransfer(OutputStream out) throws IOException {
        try (InputStream is = Files.newInputStream(file.toPath())) {
            BdFileUtils.readWriteStream(is, out);
        }
    }
}
