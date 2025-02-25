package com.soybeany.util.transfer.to;

import com.soybeany.exception.BdIoException;
import com.soybeany.util.transfer.core.IDataTo;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class DataToString implements IDataTo<OutputStream> {

    private final ICallback callback;
    private ByteArrayOutputStream os;

    public DataToString(ICallback callback) {
        this.callback = callback;
    }

    @Override
    public void onInit() {
        os = new ByteArrayOutputStream();
    }

    @Override
    public OutputStream onGetOutput() {
        return new BufferedOutputStream(os);
    }

    @Override
    public void onSuccess() {
        callback.onFinish(getContent(os));
    }

    private String getContent(ByteArrayOutputStream os) {
        String charsetName = callback.streamCharset().name();
        try {
            return os.toString(charsetName);
        } catch (UnsupportedEncodingException e) {
            throw new BdIoException("不支持字符编码：" + charsetName);
        }
    }

    // ***********************内部类****************************

    public interface ICallback {
        default Charset streamCharset() {
            return StandardCharsets.UTF_8;
        }

        void onFinish(String content);
    }

}
