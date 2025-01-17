package com.soybeany.download;

import com.soybeany.download.core.BdDownloadException;
import com.soybeany.download.core.FileInfo;
import com.soybeany.exception.BdRtException;
import com.soybeany.util.file.BdFileUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.function.Supplier;

import static com.soybeany.download.core.BdDownloadHeaders.*;

/**
 * @author Soybeany
 * @date 2020/12/9
 */
public class FileServerUtils {

    /**
     * 使用随机访问下载指定文件（简单）
     */
    public static void supply(HttpServletRequest request, HttpServletResponse response, File file) {
        FileInfo.Server info = new FileInfo.Server(FileInfo.Server.toDisposition(file.getName()), String.valueOf(file.lastModified()));
        supply(info, request, response, file);
    }

    /**
     * 使用随机访问下载指定文件
     */
    public static void supply(FileInfo.Server info, HttpServletRequest request, HttpServletResponse response, File file) {
        supply(request, response, new ICallback<FileInfo.Server>() {
            @Override
            public Optional<String> onCalculateMd5(FileInfo.Server info, FileInfo.Range range) {
                return Optional.of(BdFileUtils.md5(file));
            }

            @Override
            public FileInfo.Server onGetFileInfo() {
                return info;
            }

            @Override
            public long onGetContentMaxLength() {
                return file.length();
            }

            @Override
            public void onWrite(FileInfo.Server info, OutputStream out, FileInfo.Range range) {
                BdFileUtils.randomRead(file, out, range.start, range.end);
            }
        });
    }

    /**
     * 使用随机访问下载
     */
    public static <T extends FileInfo.Server> void supply(HttpServletRequest request, HttpServletResponse response, ICallback<T> callback) {
        long maxLength = callback.onGetContentMaxLength();
        T info = callback.onGetFileInfo();
        // 获取待读取的范围
        FileInfo.Range range = getRange(request, maxLength, info);
        // 设置断点续传的响应头
        setupRandomAccessResponseHeader(info, range, response, callback);
        // 将内容写入到响应
        try (ServletOutputStream os = response.getOutputStream()) {
            callback.onWrite(info, os, range);
        } catch (IOException e) {
            throw new BdRtException(e.getMessage());
        }
    }

    // ***********************内部方法****************************

    private static void applyInfo(FileInfo info, long contentLength, HttpServletResponse response, Supplier<Optional<String>> md5Provider) {
        response.setContentType(info.contentType());
        response.setContentLengthLong(contentLength);
        response.setHeader(CONTENT_DISPOSITION, info.contentDisposition());
        Optional.ofNullable(info.eTag()).ifPresent(eTag -> response.setHeader(E_TAG, eTag));
        md5Provider.get().ifPresent(md5 -> response.setHeader(CONTENT_MD5, md5));
    }

    private static <T extends FileInfo.Server> void setupRandomAccessResponseHeader(T info, FileInfo.Range range, HttpServletResponse response, ICallback<T> callback) {
        // 公共设置
        applyInfo(info, range.end - range.start, response, () -> callback.onCalculateMd5(info, range));
        response.setHeader(ACCEPT_RANGES, BYTES);
        // 完整下载与部分下载差异化设置
        long contentLength = callback.onGetContentMaxLength();
        boolean completeDownload = (range.start == 0) && (range.end == contentLength);
        if (completeDownload) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            String contentRange = BYTES + " " + range.start + "-" + range.end + "/" + contentLength;
            response.setHeader(CONTENT_RANGE, contentRange);
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        }
    }

    private static FileInfo.Range getRange(HttpServletRequest request, long maxLength, FileInfo.Server info) {
        // 读取请求中的数值
        String range = request.getHeader(RANGE);
        String ifRange = request.getHeader(IF_RANGE);
        // 若不满足要求，返回全量范围
        boolean needFull = (!info.enableRandomAccess())
                || (null == range)
                || (info.needCheckIfRange() && (null == ifRange || !ifRange.equals(info.eTag())));
        if (needFull) {
            return FileInfo.Range.getDefault(maxLength);
        }
        try {
            long end = maxLength;
            String[] rangeArr = range.replaceAll(BYTES + "=", "").split("-");
            long start = Long.parseLong(rangeArr[0]);
            if (rangeArr.length > 1 && !rangeArr[1].isEmpty()) {
                end = Long.parseLong(rangeArr[1]);
            }
            if (start < 0 || end > maxLength) {
                throw new BdDownloadException("非法的续传范围:" + start + "~" + end);
            }
            return new FileInfo.Range(start, end);
        } catch (NumberFormatException ignore) {
            return FileInfo.Range.getDefault(maxLength);
        }
    }

    // ***********************内部类****************************

    /**
     * 由文件本身决定，不应由外界提供信息的场景，则在此新增方法
     */
    public interface ICallback<T extends FileInfo.Server> {
        default Optional<String> onCalculateMd5(T info, FileInfo.Range range) {
            return Optional.empty();
        }

        T onGetFileInfo();

        long onGetContentMaxLength();

        void onWrite(T info, OutputStream out, FileInfo.Range range);
    }

}
