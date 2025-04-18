package com.soybeany.util.file;


import com.soybeany.exception.BdRtException;
import com.soybeany.util.BdBufferUtils;
import com.soybeany.util.BdStreamUtils;
import com.soybeany.util.Md5Utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

/**
 * 文件操作工具类
 *
 * @author Soybeany
 * @date 2018/5/30
 */
@SuppressWarnings("UnusedReturnValue")
public abstract class BdFileUtils {

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
        return randomRead(file, start, end, (raf, len) -> Md5Utils.calMd5(len, raf::read, b -> {
        }));
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
    public static long randomRead(File inFile, long start, long end, OutputStream out) {
        return randomRead(inFile, start, end, new BdBufferUtils.Buffer(0, BdStreamUtils.DEFAULT_BLOCK_SIZE), buffer -> out.write(buffer.buffer, buffer.offset, buffer.length));
    }

    public static <T extends BdBufferUtils.Buffer> long randomRead(File inFile, long start, long end, T buffer, BdBufferUtils.DataConsumer<T> callback) {
        return randomRead(inFile, start, end, (raf, len) -> BdBufferUtils.dataCopy(len, buffer, raf::read, callback));
    }

    public static <T> T randomRead(File inFile, long start, long end, RandomReadCallback<T> callback) {
        if (start < 0) {
            throw new BdRtException("start < 0");
        }
        if (end < start) {
            throw new BdRtException("end < start");
        }
        try (RandomAccessFile raf = new BufferedRandomAccessFile(inFile, "r")) {
            raf.seek(start);
            return callback.onHandle(raf, end - start);
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
        readWriteStream(in, out, BdStreamUtils.FLAG_ALL);
    }

    public static void readWriteStreamNoBuffer(InputStream in, OutputStream out) {
        BdStreamUtils.readWriteStream(in, out, null, null, BdStreamUtils.FLAG_NO_BUFFER);
    }

    public static void readWriteStream(InputStream in, OutputStream out, int flags) {
        BdStreamUtils.readWriteStream(in, out, null, null, flags);
    }

    /**
     * 关闭流
     * Created by Soybeany on 2018/5/14 10:41
     */
    public static void closeStream(Closeable closeable) {
        BdStreamUtils.closeStream(closeable);
    }

    /**
     * 获得UUID
     * Created by Soybeany on 2018/5/30 9:44
     */
    public static String getUuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public interface RandomReadCallback<T> {
        T onHandle(RandomAccessFile raf, long len) throws IOException;
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
