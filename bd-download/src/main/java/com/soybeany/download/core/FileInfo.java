package com.soybeany.download.core;

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
}
