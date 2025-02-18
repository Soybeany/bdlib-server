package com.soybeany.util.transfer.from;

import com.soybeany.util.file.BdFileUtils;
import com.soybeany.util.transfer.core.IDataFrom;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class DataFromString implements IDataFrom<OutputStream> {
    private final String content;
    private final Charset streamCharset;

    public DataFromString(String content) {
        this(content, StandardCharsets.UTF_8);
    }

    public DataFromString(String content, Charset streamCharset) {
        this.content = content;
        this.streamCharset = streamCharset;
    }

    @Override
    public void onTransfer(OutputStream os) throws IOException {
        try (InputStream is = new ByteArrayInputStream(content.getBytes(streamCharset))) {
            BdFileUtils.readWriteStreamNoBuffer(is, os);
        }
    }
}
