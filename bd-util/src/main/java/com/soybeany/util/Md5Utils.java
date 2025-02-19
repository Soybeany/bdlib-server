package com.soybeany.util;

import com.soybeany.exception.BdRtException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Soybeany
 * @date 2018/9/11
 */
public abstract class Md5Utils {

    public static final int MD5_SIZE = 16;
    public static final int DEFAULT_SECTION_LENGTH = 10 * 1024 * 1024 - MD5_SIZE;

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
        BufferWithMd5 buffer = new BufferWithMd5();
        long l = BdBufferUtils.dataCopy(dataLength, buffer, supplier, b -> {
            // 更新md5
            updateMd5(digest, b);
            // 输出
            consumer.onHandle(b);
        });
        return HexUtils.bytesToHex(buffer.md5);
    }

    // ***********************内部方法****************************

    private static MessageDigest getDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new BdRtException(e.getMessage());
        }
    }

    private static void updateMd5(MessageDigest digest, BufferWithMd5 buffer) {
        // 将上一次的结果与新数据混合
        System.arraycopy(buffer.md5, 0, buffer.buffer, 0, MD5_SIZE);
        // 开始计算混合后数据的md5
        digest.update(buffer.buffer, 0, MD5_SIZE + buffer.length);
        buffer.md5 = digest.digest();
    }

    // ***********************内部类****************************

    public static class Md5InputStream extends InputStream {
        private final MessageDigest digest = getDigest();
        private final BufferWithMd5 buffer = new BufferWithMd5();
        private final BdBufferUtils.Calculator<BufferWithMd5> calculator = new BdBufferUtils.Calculator<>(buffer, b -> updateMd5(digest, b));

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
            calculator.write(b, off, read);
            return read;
        }

        @Override
        public void close() throws IOException {
            super.close();
            calculator.finish();
        }

        public String getMd5() {
            return HexUtils.bytesToHex(buffer.md5);
        }
    }

    private static class BufferWithMd5 extends BdBufferUtils.Buffer {
        byte[] md5 = new byte[MD5_SIZE];

        public BufferWithMd5() {
            super(MD5_SIZE, DEFAULT_SECTION_LENGTH);
        }
    }
}
