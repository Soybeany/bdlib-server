package com.soybeany.util.file;


import com.soybeany.exception.BdRtException;
import com.soybeany.util.HexUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * 文件操作工具类
 *
 * @author Soybeany
 * @date 2018/5/30
 */
@SuppressWarnings("UnusedReturnValue")
public class BdFileUtils {

    public static final int FLAG_CLEAN = 0;
    public static final int FLAG_IN_BUFFER = 1;
    public static final int FLAG_IN_CLOSE = 1 << 1;
    public static final int FLAG_OUT_BUFFER = 1 << 2;
    public static final int FLAG_OUT_CLOSE = 1 << 3;

    /**
     * 默认的分段尺寸
     */
    public static final int DEFAULT_BLOCK_SIZE = 25 * 1024;
    public static final int MD5_SIZE = 16;
    public static final int DEFAULT_SECTION_LENGTH = 10 * 1024 * 1024 - MD5_SIZE;

    /**
     * 创建指定文件的父目录
     * Created by Soybeany on 2018/5/14 10:41
     */
    public static boolean mkParentDirs(File file) {
        return mkDirs(file.getParentFile());
    }

    /**
     * 根据路径名创建目录
     *
     * @return 目录是否创建
     * Created by Soybeany on 2018/5/14 10:41
     */
    public static boolean mkDirs(File file) {
        if (file.exists()) {
            return false;
        }
        return file.mkdirs();
    }

    public static String md5(File file) {
        return md5(file, 0, file.length());
    }

    public static String md5(File file, long start, long end) {
        return md5(file, start, end, DEFAULT_SECTION_LENGTH);
    }

    public static String md5(File file, long start, long end, int sectionLength) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new BdRtException(e.getMessage());
        }
        BufferWithMd5 buffer = new BufferWithMd5(MD5_SIZE, sectionLength);
        long l = randomRead(file, start, end, buffer, b -> {
            // 将上一次的结果与新数据混合
            System.arraycopy(b.md5, 0, b.buffer, 0, MD5_SIZE);
            // 开始计算混合后数据的md5
            digest.update(b.buffer, 0, MD5_SIZE + b.length);
            b.md5 = digest.digest();
        });
        return HexUtils.bytesToHex(buffer.md5);
    }

    /**
     * 读写流操作(流输出到文件)
     * Created by Soybeany on 2018/5/14 10:41
     */
    public static void readWriteStream(InputStream in, File file) {
        mkParentDirs(file);
        try (OutputStream out = Files.newOutputStream(file.toPath())) {
            readWriteStream(in, out);
        } catch (IOException e) {
            throw new BdRtException(e.getMessage());
        }
    }

    /**
     * 从流中读取字符串
     */
    public static String readString(InputStream in) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            readWriteStream(in, stream);
            return stream.toString("utf-8");
        } catch (IOException e) {
            throw new BdRtException(e.getMessage());
        }
    }

    /**
     * 随机读取
     *
     * @return 总共读取的字节
     * Created by Soybeany on 2018/5/8 11:00
     */
    public static long randomRead(File inFile, OutputStream out, long start, long end) {
        return randomRead(inFile, start, end, new Buffer(0, DEFAULT_BLOCK_SIZE), buffer -> out.write(buffer.buffer, buffer.offset, buffer.length));
    }

    public static <T extends Buffer> long randomRead(File inFile, long start, long end, T buffer, RandomReadCallback<T> callback) {
        if (start < 0) {
            throw new BdRtException("start < 0");
        }
        if (end < start) {
            throw new BdRtException("end < start");
        }
        try (RandomAccessFile raf = new BufferedRandomAccessFile(inFile, "r")) {
            raf.seek(start);
            int curRead, written = 0;
            long totalRead = 0, dataLength = end - start, remaining;
            while ((remaining = dataLength - totalRead) > 0) {
                // 读取数据
                curRead = raf.read(buffer.buffer, buffer.offset + written, (int) Math.min(buffer.length - written, remaining));
                // 已到文件末尾，则提前结束
                if (curRead < 0) {
                    break;
                }
                // 累计数据
                totalRead += curRead;
                written += curRead;
                // 累积足够数据，则回调业务层
                if (buffer.length == written) {
                    callback.onHandle(buffer);
                    written = 0;
                }
            }
            // 将缓存的数据也返回业务层
            if (written > 0) {
                buffer.length = written;
                callback.onHandle(buffer);
            }
            return totalRead;
        } catch (IOException e) {
            throw new BdRtException(e.getMessage());
        }
    }

    /**
     * 随机读取
     */
    public static void randomReadLine(File inFile, long startPointer, RandomReadLineCallback callback) {
        try (RandomAccessFile raf = new BufferedRandomAccessFile(inFile, "r")) {
            raf.seek(startPointer);
            callback.onInit();
            String line;
            int status = 0;
            String charSet = callback.onSetupCharset();
            while (null != (line = raf.readLine())) {
                if (0 != (status = callback.onHandleLine(startPointer, startPointer = raf.getFilePointer(),
                        new String(line.getBytes(StandardCharsets.ISO_8859_1), charSet)))) {
                    break;
                }
            }
            callback.onFinish(status);
        } catch (IOException e) {
            throw new BdRtException(e.getMessage());
        }
    }

    /**
     * 读写流操作
     * Created by Soybeany on 2018/5/14 10:41
     */
    public static void readWriteStream(InputStream in, OutputStream out) {
        readWriteStream(in, out, FLAG_IN_BUFFER | FLAG_IN_CLOSE | FLAG_OUT_BUFFER | FLAG_OUT_CLOSE);
    }

    public static void readWriteStream(InputStream in, OutputStream out, int flags) {
        readWriteStream(in, out, null, null, flags);
    }

    /**
     * 读写流操作
     * Created by Soybeany on 2018/5/14 10:41
     */
    public static void readWriteStream(InputStream in, OutputStream out, Integer bufferArrSize, ILoopCallback callback, int flags) {
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

            int len;
            byte[] buffer = new byte[null != bufferArrSize ? bufferArrSize : DEFAULT_BLOCK_SIZE];
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

    /**
     * 获得UUID
     * Created by Soybeany on 2018/5/30 9:44
     */
    public static String getUuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public interface ILoopCallback {
        default void onBeforeWrite(byte[] data, int length) throws IOException {
        }

        default void onAfterWrite(byte[] data, int length) throws IOException {
        }
    }

    public interface RandomReadCallback<T extends Buffer> {
        void onHandle(T buffer) throws IOException;
    }

    public interface RandomReadLineCallback {
        /**
         * 设置字符集
         */
        default String onSetupCharset() {
            return "utf-8";
        }

        /**
         * 初始化时的回调
         */
        default void onInit() {
            // 子类按需实现
        }

        /**
         * 完成时的回调
         *
         * @param status 状态值，由{@link #onHandleLine(long, long, String)}返回，0表示读取到文件末尾
         */
        default void onFinish(int status) throws IOException {
            // 子类按需实现
        }

        /**
         * @param startPointer 开始位点
         * @param endPointer   结束位点
         * @param line         此行内容
         * @return 状态码 0:正常读取下一行；其它值表示各种中断状态
         */
        int onHandleLine(long startPointer, long endPointer, String line) throws IOException;
    }

    public static class Buffer {
        public byte[] buffer;
        public int offset;
        public int length;

        public Buffer(int offset, int length) {
            this.offset = offset;
            this.length = length;
            this.buffer = new byte[offset + length];
        }
    }

    private static class BufferWithMd5 extends BdFileUtils.Buffer {
        byte[] md5 = new byte[MD5_SIZE];

        public BufferWithMd5(int offset, int length) {
            super(offset, length);
        }
    }

}
