package com.soybeany.download;

import com.soybeany.download.core.BdDownloadException;
import com.soybeany.download.core.Range;
import com.soybeany.exception.BdRtException;
import com.soybeany.util.file.BdFileUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Optional;

import static com.soybeany.download.core.BdDownloadHeaders.*;

public class DataSupplier {

    public static String toDisposition(String fileName) {
        try {
            return "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"";
        } catch (UnsupportedEncodingException e) {
            throw new BdRtException("使用了不支持的编码“utf-8”");
        }
    }

    public Part1 fileName(String fileName, long contentLength) {
        return contentDisposition(toDisposition(fileName), contentLength);
    }

    public Part1 contentDisposition(String contentDisposition, long contentLength) {
        return new Part1(contentDisposition, contentLength);
    }

    public Part3 file(File file) {
        return fileName(file.getName(), file.length())
                .eTag(String.valueOf(file.lastModified()))
                .from()
                .file(file);
    }

    /**
     * 文件静态属性
     */
    public static class Part1 {
        private final String contentDisposition;
        private final long contentLength;
        private String contentType = "application/octet-stream";
        private String eTag;

        Part1(String contentDisposition, long contentLength) {
            this.contentDisposition = contentDisposition;
            this.contentLength = contentLength;
        }

        public Part1 contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Part1 eTag(String eTag) {
            this.eTag = eTag;
            return this;
        }

        public Part2 from() {
            return new Part2(this);
        }
    }

    /**
     * 文件数据以及动态配置
     */
    public static class Part2 {
        private final Part1 part1;
        private IRandomAccessCallback callback;
        private boolean supportRandomAccess;

        public Part2(Part1 part1) {
            this.part1 = part1;
        }

        public Part3 file(File file) {
            return file(file, true);
        }

        public Part3 file(File file, boolean useRangeMd5) {
            return callback(new IRandomAccessCallback() {
                @Override
                public Optional<String> onCalculateMd5(Range range) {
                    return Optional.of(useRangeMd5 ? BdFileUtils.md5(file, range.start, range.end) : BdFileUtils.md5(file));
                }

                @Override
                public void onWrite(Range range, OutputStream out) {
                    BdFileUtils.randomRead(file, range.start, range.end, out);
                }
            });
        }

        public Part3 callback(ICallback callback) {
            this.callback = new IRandomAccessCallback() {
                @Override
                public Optional<String> onCalculateMd5(Range range) {
                    return callback.onCalculateMd5();
                }

                @Override
                public void onWrite(Range range, OutputStream out) {
                    callback.onWrite(out);
                }
            };
            supportRandomAccess = false;
            return new Part3(this);
        }

        public Part3 callback(IRandomAccessCallback callback) {
            this.callback = callback;
            supportRandomAccess = true;
            return new Part3(this);
        }
    }

    /**
     * 配置数据消费
     */
    public static class Part3 {
        private final Part1 part1;
        private final Part2 part2;
        private boolean completeDownload = true;
        private String consumerETag;
        private Range range;

        Part3(Part2 part2) {
            this.part1 = part2.part1;
            this.part2 = part2;
            this.range = Range.getDefault(part1.contentLength);
        }

        public Part3 randomAccess(HttpServletRequest request, boolean needCheckIfRange) {
            range = getRange(request, needCheckIfRange);
            return this;
        }

        public Part3 checkModified(String eTag) {
            consumerETag = eTag;
            return this;
        }

        public void to(HttpServletResponse response) {
            // 比对eTag
            if (null != consumerETag && consumerETag.equals(part1.eTag)) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }
            // 设置响应头
            setupResponseHeader(range, response);
            // 将内容写入到响应
            try (ServletOutputStream os = response.getOutputStream()) {
                part2.callback.onWrite(range, os);
            } catch (IOException e) {
                throw new BdRtException(e.getMessage());
            }
        }

        // ***********************内部方法****************************

        private void setupResponseHeader(Range range, HttpServletResponse response) {
            // 常规响应头
            response.setContentType(part1.contentType);
            response.setContentLengthLong(part1.contentLength);
            response.setHeader(CONTENT_DISPOSITION, part1.contentDisposition);
            // 可选响应头
            Optional.ofNullable(part1.eTag).ifPresent(eTag -> response.setHeader(E_TAG, eTag));
            part2.callback.onCalculateMd5(range).ifPresent(md5 -> response.setHeader(CONTENT_MD5, md5));
            // 支持断点续传的标识
            if (part2.supportRandomAccess) {
                response.setHeader(ACCEPT_RANGES, BYTES);
            }
            // 完整下载与部分下载差异化设置
            if (completeDownload) {
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                String contentRange = BYTES + " " + range.start + "-" + range.end + "/" + part1.contentLength;
                response.setHeader(CONTENT_RANGE, contentRange);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            }
        }

        private Range getRange(HttpServletRequest request, boolean needCheckIfRange) {
            // 读取请求中的数值
            String rRange = request.getHeader(RANGE);
            String rIfRange = request.getHeader(IF_RANGE);
            long maxLength = part1.contentLength;
            // 若不满足要求，返回全量范围
            completeDownload = (!part2.supportRandomAccess)
                    || (null == rRange)
                    || (needCheckIfRange && (null == rIfRange || !rIfRange.equals(part1.eTag)));
            if (completeDownload) {
                return Range.getDefault(maxLength);
            }
            try {
                long end = maxLength;
                String[] rangeArr = rRange.replaceAll(BYTES + "=", "").split("-");
                long start = Long.parseLong(rangeArr[0]);
                if (rangeArr.length > 1 && !rangeArr[1].isEmpty()) {
                    end = Long.parseLong(rangeArr[1]);
                }
                if (start < 0 || end > maxLength) {
                    throw new BdDownloadException("非法的续传范围:" + start + "~" + end);
                }
                return new Range(start, end);
            } catch (NumberFormatException ignore) {
                return Range.getDefault(maxLength);
            }
        }
    }

    public interface ICallback {
        default Optional<String> onCalculateMd5() {
            return Optional.empty();
        }

        void onWrite(OutputStream out);
    }

    public interface IRandomAccessCallback {
        default Optional<String> onCalculateMd5(Range range) {
            return Optional.empty();
        }

        void onWrite(Range range, OutputStream out);
    }
}
