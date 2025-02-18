package com.soybeany.util.transfer.to;

import com.soybeany.exception.BdIoException;
import com.soybeany.util.transfer.core.IDataTo;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.function.Consumer;

public class DataToStringInStream implements IDataTo<OutputStream> {
    private final Charset streamCharset;
    private final Consumer<String> consumer;
    private ByteArrayOutputStream os;

    public DataToStringInStream(Charset streamCharset, Consumer<String> consumer) {
        this.streamCharset = streamCharset;
        this.consumer = consumer;
    }

    @Override
    public OutputStream onGetOutput() {
        return new BufferedOutputStream(os = new ByteArrayOutputStream());
    }

    @Override
    public void onSuccess() {
        consumer.accept(getContent());
    }

    private String getContent() {
        String charsetName = streamCharset.name();
        try {
            return os.toString(charsetName);
        } catch (UnsupportedEncodingException e) {
            throw new BdIoException("不支持字符编码：" + charsetName);
        }
    }
}
