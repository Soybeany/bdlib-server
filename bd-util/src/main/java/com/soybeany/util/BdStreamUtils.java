package com.soybeany.util;

import com.soybeany.exception.BdRtException;

import java.io.*;

public abstract class BdStreamUtils {

    public static final int FLAG_NO_BUFFER = 0;
    public static final int FLAG_IN_BUFFER = 1;
    public static final int FLAG_IN_CLOSE = 1 << 1;
    public static final int FLAG_OUT_BUFFER = 1 << 2;
    public static final int FLAG_OUT_CLOSE = 1 << 3;
    public static final int FLAG_ALL = FLAG_IN_BUFFER | FLAG_IN_CLOSE | FLAG_OUT_BUFFER | FLAG_OUT_CLOSE;

    /**
     * 默认的分段尺寸
     */
    public static final int DEFAULT_BLOCK_SIZE = 25 * 1024;

    public static String transAndCalMd5(InputStream in, OutputStream out) {
        return Md5Utils.calMd5(Long.MAX_VALUE, in::read, b -> out.write(b.buffer, b.offset, b.length));
    }

    /**
     * 读写流操作
     * Created by Soybeany on 2018/5/14 10:41
     */
    public static void readWriteStream(InputStream in, OutputStream out, Integer bufferArrSize, ILoopCallback callback, int flags) {
        byte[] buffer = new byte[null != bufferArrSize ? bufferArrSize : DEFAULT_BLOCK_SIZE];
        BdStreamUtils.wrapBuffer(in, out, flags, (input, output) -> {
            int len;
            if (null != callback) {
                while ((len = input.read(buffer)) > 0) {
                    callback.onBeforeWrite(buffer, len);
                    output.write(buffer, 0, len);
                    callback.onAfterWrite(buffer, len);
                }
            } else {
                while ((len = input.read(buffer)) > 0) {
                    output.write(buffer, 0, len);
                }
            }
        });
    }

    public static void wrapBuffer(InputStream in, OutputStream out, int flags, DataHandler handler) {
        if (null == in) {
            throw new RuntimeException("输入流不能为null");
        }
        if (null == out) {
            throw new RuntimeException("输出流不能为null");
        }
        InputStream input = null;
        OutputStream output = null;
        try {
            input = (flags & FLAG_IN_BUFFER) > 0 ? in : new BufferedInputStream(in);
            output = (flags & FLAG_OUT_BUFFER) > 0 ? out : new BufferedOutputStream(out);
            handler.onHandle(input, output);
        } catch (IOException e) {
            throw new BdRtException(e.getMessage());
        } finally {
            if ((flags & FLAG_OUT_CLOSE) > 0) {
                closeStream(output);
            }
            if ((flags & FLAG_IN_CLOSE) > 0) {
                closeStream(input);
            }
        }
    }

    /**
     * 关闭流
     * Created by Soybeany on 2018/5/14 10:41
     */
    public static void closeStream(Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (Exception ignore) {
            }
        }
    }

    public interface ILoopCallback {
        default void onBeforeWrite(byte[] data, int length) throws IOException {
        }

        default void onAfterWrite(byte[] data, int length) throws IOException {
        }
    }

    public interface DataHandler {
        void onHandle(InputStream is, OutputStream os) throws IOException;
    }

}
