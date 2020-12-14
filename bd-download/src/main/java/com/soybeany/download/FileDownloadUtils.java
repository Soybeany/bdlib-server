package com.soybeany.download;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

/**
 * @author Soybeany
 * @date 2020/12/9
 */
public class FileDownloadUtils {

    /**
     * 使用随机访问下载指定文件(断点续传)
     */
    public static <T extends InfoProvider> void randomAccessDownloadFile(T info, HttpServletRequest request, HttpServletResponse response, ICallback<T> callback) throws IOException {
        // 获取待读取的范围
        Range range = getRange(request, info.getContentLength() - 1, info.getEtag());
        // 设置断点续传的响应头
        setupResponseHeader(range, info, response);
        // 将内容写入到响应
        callback.onWriteResponse(info, response.getOutputStream(), range.start, range.end);
    }

    /**
     * 获取用于下载的“Content-Disposition”响应头
     */
    public static String getAttachmentContentDisposition(String fileName) throws IOException {
        return "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"";
    }

    private static <T extends InfoProvider> void setupResponseHeader(Range range, T info, HttpServletResponse response) throws IOException {
        // 公共设置
        response.setContentLength((int) (range.end - range.start + 1));
        response.setContentType(info.getContentType());
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Disposition", info.getContentDisposition());
        // 完整下载与部分下载差异化设置
        long contentLength = info.getContentLength();
        boolean completeDownload = (range.start == 0) && (range.end + 1 == contentLength);
        if (completeDownload) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            String contentRange = "bytes " + range.start + "-" + range.end + "/" + contentLength;
            response.setHeader("Content-Range", contentRange);
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        }
        response.setHeader("ETag", info.getEtag());
    }

    private static Range getRange(HttpServletRequest request, long maxSize, String eTag) throws IOException {
        // 读取请求中的数值
        String range = request.getHeader("Range");
        String ifRange = request.getHeader("If-Range");
        // 若不满足要求，返回全量范围
        if (null == range || null == ifRange || !ifRange.equals(eTag)) {
            return Range.getDefault(maxSize);
        }
        try {
            long end = maxSize;
            String[] rangeArr = range.replaceAll("bytes=", "").split("-");
            long start = Long.parseLong(rangeArr[0]);
            if (rangeArr.length > 1 && !rangeArr[1].isEmpty()) {
                end = Long.parseLong(rangeArr[1]);
            }
            if (start < 0 || end > maxSize) {
                throw new IOException("非法的续传范围:" + start + "~" + end);
            }
            return new Range(start, end);
        } catch (NumberFormatException ignore) {
            return Range.getDefault(maxSize);
        }
    }

    public interface InfoProvider {
        String getContentType();

        long getContentLength();

        String getContentDisposition() throws IOException;

        String getEtag();
    }

    public interface ICallback<T extends InfoProvider> {
        void onWriteResponse(T info, OutputStream os, long start, long end) throws IOException;
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
