package com.soybeany.util.file;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 文件操作工具类
 *
 * @author Soybeany
 * @date 2018/5/30
 */
@SuppressWarnings("UnusedReturnValue")
public class BdFileUtils {

    /**
     * 默认的分段尺寸
     */
    public static final int DEFAULT_BLOCK_SIZE = 25 * 1024;

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

    /**
     * 读写流操作(流输出到文件)
     * Created by Soybeany on 2018/5/14 10:41
     */
    public static void readWriteStream(InputStream in, File file) throws IOException {
        mkParentDirs(file);
        try (OutputStream out = new FileOutputStream(file)) {
            readWriteStream(in, out);
        }
    }

    /**
     * 从流中读取字符串
     */
    public static String readString(InputStream in) throws IOException {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            readWriteStream(in, stream);
            return stream.toString("utf-8");
        }
    }

    /**
     * 随机读取
     *
     * @return 总共读取的字节
     * Created by Soybeany on 2018/5/8 11:00
     */
    public static long randomRead(File inFile, OutputStream out, long start, long end) throws IOException {
        try (RandomAccessFile raf = new BufferedRandomAccessFile(inFile, "r");) {
            raf.seek(start);
            int bufferSize = DEFAULT_BLOCK_SIZE;
            byte[] tempArr = new byte[bufferSize];
            int curRead = 0;
            long totalRead = 0, delta = end - start;
            while (totalRead <= delta - bufferSize) {
                curRead = raf.read(tempArr, 0, bufferSize);
                totalRead += curRead;
                out.write(tempArr, 0, curRead);
            }
            while (totalRead < delta && -1 != curRead) {
                curRead = raf.read(tempArr, 0, (int) (delta - totalRead));
                totalRead += curRead;
                out.write(tempArr, 0, curRead);
            }
            return totalRead;
        }
    }

    /**
     * 随机读取
     */
    public static void randomReadLine(File inFile, long startPointer, RandomReadLineCallback callback) throws IOException {
        try (RandomAccessFile raf = new BufferedRandomAccessFile(inFile, "r");) {
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
        }
    }

    /**
     * 读写流操作
     * Created by Soybeany on 2018/5/14 10:41
     */
    public static void readWriteStream(InputStream in, OutputStream out) throws IOException {
        readWriteStream(in, out, null, null);
    }

    /**
     * 读写流操作
     * Created by Soybeany on 2018/5/14 10:41
     */
    public static void readWriteStream(InputStream in, OutputStream out, Integer bufferArrSize, ILoopCallback callback) throws IOException {
        if (null == in) {
            throw new RuntimeException("输入流不能为null");
        }
        if (null == out) {
            throw new RuntimeException("输出流不能为null");
        }
        InputStream input = null;
        OutputStream output = null;
        try {
            input = in instanceof BufferedInputStream ? in : new BufferedInputStream(in);
            output = out instanceof BufferedOutputStream ? out : new BufferedOutputStream(out);

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
        } finally {
            if (!(out instanceof BufferedOutputStream)) {
                closeStream(output);
            }
            if (!(in instanceof BufferedInputStream)) {
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

}
