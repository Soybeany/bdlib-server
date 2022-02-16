package com.soybeany.download;

import com.soybeany.download.core.BdDownloadException;
import com.soybeany.download.core.FileInfo;
import com.soybeany.util.file.BdFileUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.soybeany.download.core.BdDownloadHeaders.*;

/**
 * @author Soybeany
 * @date 2020/12/9
 */
public class FileServerUtils {

    /**
     * 使用随机访问下载指定文件(断点续传)
     */
    public static <T extends FileInfo> void randomAccessDownloadFile(T info, HttpServletRequest request, HttpServletResponse response, File file) throws IOException {
        randomAccessDownload(info, request, response, (fi, out, start, end) -> BdFileUtils.randomRead(file, out, start, end));
    }

    /**
     * 使用随机访问下载(断点续传)
     */
    public static <T extends FileInfo> void randomAccessDownload(T info, HttpServletRequest request, HttpServletResponse response, ICallback<T> callback) throws IOException {
        // 获取待读取的范围
        Range range = getRange(request, info.contentLength(), info.eTag());
        // 设置断点续传的响应头
        setupRandomAccessResponseHeader(range, info, response);
        // 将内容写入到响应
        callback.onWrite(info, response.getOutputStream(), range.start, range.end);
    }

    /**
     * 下载流(不支持断点续传)
     */
    public static void downloadStream(FileInfo info, InputStream is, HttpServletResponse response) throws IOException {
        applyInfo(info, info.contentLength(), response);
        BdFileUtils.readWriteStream(is, response.getOutputStream());
    }

    // ***********************内部方法****************************

    private static void applyInfo(FileInfo info, long contentLength, HttpServletResponse response) {
        response.setContentType(info.contentType());
        response.setContentLengthLong(contentLength);
        response.setHeader(CONTENT_DISPOSITION, info.contentDisposition());
        response.setHeader(E_TAG, info.eTag());
    }

    private static void setupRandomAccessResponseHeader(Range range, FileInfo info, HttpServletResponse response) {
        // 公共设置
        applyInfo(info, range.end - range.start, response);
        response.setHeader(ACCEPT_RANGES, BYTES);
        // 完整下载与部分下载差异化设置
        long contentLength = info.contentLength();
        boolean completeDownload = (range.start == 0) && (range.end == contentLength);
        if (completeDownload) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            String contentRange = BYTES + " " + range.start + "-" + range.end + "/" + contentLength;
            response.setHeader(CONTENT_RANGE, contentRange);
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        }
    }

    private static Range getRange(HttpServletRequest request, long maxSize, String eTag) throws IOException {
        // 读取请求中的数值
        String range = request.getHeader(RANGE);
        String ifRange = request.getHeader(IF_RANGE);
        // 若不满足要求，返回全量范围b
        if (null == range || null == ifRange || !ifRange.equals(eTag)) {
            return Range.getDefault(maxSize);
        }
        try {
            long end = maxSize;
            String[] rangeArr = range.replaceAll(BYTES + "=", "").split("-");
            long start = Long.parseLong(rangeArr[0]);
            if (rangeArr.length > 1 && !rangeArr[1].isEmpty()) {
                end = Long.parseLong(rangeArr[1]);
            }
            if (start < 0 || end > maxSize) {
                throw new BdDownloadException("非法的续传范围:" + start + "~" + end);
            }
            return new Range(start, end);
        } catch (NumberFormatException ignore) {
            return Range.getDefault(maxSize);
        }
    }

    // ***********************内部类****************************

    public interface ICallback<T extends FileInfo> {
        void onWrite(T info, OutputStream out, long start, long end) throws IOException;
    }

    private static class Range {
        long start;
        long end;

        static Range getDefault(long end) {
            return new Range(0, end);
        }

        public Range(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }

}
