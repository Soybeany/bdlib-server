package com.soybeany.util;

import com.soybeany.exception.BdRtException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Soybeany
 * @date 2018/9/11
 */
@SuppressWarnings("unused")
public abstract class Md5Utils {

    public static String strToMd5(String msg) {
        return bytesToMd5(msg.getBytes(StandardCharsets.UTF_8));
    }

    public static String bytesToMd5(byte[] input) {
        return HexUtils.bytesToHex(bytesToMd5Bytes(input));
    }

    public static byte[] bytesToMd5Bytes(byte[] input) {
        return getDigest().digest(input);
    }

    public static String calMd5(long dataLength, BdBufferUtils.DataSupplier supplier, BdBufferUtils.DataConsumer<BdBufferUtils.Buffer> consumer) {
        MessageDigest digest = getDigest();
        long l = BdBufferUtils.dataCopy(dataLength, new BdBufferUtils.Buffer(0, BdStreamUtils.DEFAULT_BLOCK_SIZE), supplier, buffer -> {
            // 更新md5
            digest.update(buffer.buffer, buffer.offset, buffer.length);
            // 输出
            consumer.onHandle(buffer);
        });
        return HexUtils.bytesToHex(digest.digest());
    }

    public static MessageDigest getDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new BdRtException(e.getMessage());
        }
    }

    // ***********************内部类****************************

    public static class Md5InputStream extends InputStream {
        private final MessageDigest digest = getDigest();

        private final InputStream target;

        public Md5InputStream(InputStream target) {
            this.target = target;
        }

        @Override
        public int read() {
            throw new BdRtException("不支持读单个字节");
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = target.read(b, off, len);
            if (read > 0) {
                digest.update(b, off, read);
            }
            return read;
        }

        public String getMd5() {
            return HexUtils.bytesToHex(digest.digest());
        }
    }

    public static class Md5OutputStream extends OutputStream {
        private final MessageDigest digest = getDigest();

        private final OutputStream target;

        public Md5OutputStream(OutputStream target) {
            this.target = target;
        }

        @Override
        public void write(int b) {
            throw new BdRtException("不支持写单个字节");
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            target.write(b, off, len);
            digest.update(b, off, len);
        }

        public String getMd5() {
            return HexUtils.bytesToHex(digest.digest());
        }
    }
}
