package com.soybeany.download.core;

import com.soybeany.exception.BdRtException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author Soybeany
 * @date 2022/2/14
 */
public abstract class FileInfo {
    protected final String contentDisposition;
    protected final String eTag;

    protected String contentType;

    public FileInfo(String contentDisposition, String eTag) {
        this.contentDisposition = contentDisposition;
        this.eTag = eTag;
    }

    public String contentDisposition() {
        return contentDisposition;
    }

    public String eTag() {
        return eTag;
    }

    public String contentType() {
        return contentType;
    }

    // ***********************内部类****************************

    public static class Client extends FileInfo {
        private final long contentLength;
        private final String md5;

        public Client(String contentDisposition, String eTag, String contentType, long contentLength, String md5) {
            super(contentDisposition, eTag);
            this.contentType = contentType;
            this.contentLength = contentLength;
            this.md5 = md5;
        }

        public long getContentLength() {
            return contentLength;
        }

        public String getMd5() {
            return md5;
        }
    }

    public static class Server extends FileInfo {
        private boolean needCheckIfRange = true;
        private boolean enableRandomAccess = true;

        public static String toDisposition(String fileName) {
            try {
                return "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"";
            } catch (UnsupportedEncodingException e) {
                throw new BdRtException("使用了不支持的编码“utf-8”");
            }
        }

        public Server(String contentDisposition, String eTag) {
            super(contentDisposition, eTag);
            contentType = "application/octet-stream";
        }

        public FileInfo contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public boolean needCheckIfRange() {
            return needCheckIfRange;
        }

        public FileInfo needCheckIfRange(boolean needCheckIfRange) {
            this.needCheckIfRange = needCheckIfRange;
            return this;
        }

        public Boolean enableRandomAccess() {
            return enableRandomAccess;
        }

        public void enableRandomAccess(boolean enableRandomAccess) {
            this.enableRandomAccess = enableRandomAccess;
        }
    }

    public static class Range {
        public final long start;
        public final long end;

        public static Range getDefault(long end) {
            return new Range(0, end);
        }

        public Range(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }
}
