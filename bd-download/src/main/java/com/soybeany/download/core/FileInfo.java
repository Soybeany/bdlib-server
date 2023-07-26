package com.soybeany.download.core;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author Soybeany
 * @date 2022/2/14
 */
public class FileInfo {
    private String contentType = "application/octet-stream";
    private Boolean needCheck = true;
    private final String contentDisposition;
    private final long contentLength;
    private final String eTag;

    public FileInfo(String contentDisposition, long contentLength, String eTag) {
        this.contentDisposition = contentDisposition;
        this.contentLength = contentLength;
        this.eTag = eTag;
    }

    /**
     * 获取用于下载的“Content-Disposition”响应头
     */
    public static String getAttachmentContentDisposition(String fileName, String enc) {
        try {
            return "attachment; filename=\"" + URLEncoder.encode(fileName, enc) + "\"";
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("使用了不支持的编码“" + enc + "+”");
        }
    }

    public static FileInfo getNewAttachment(String fileName, long contentLength, String eTag) {
        return new FileInfo(getAttachmentContentDisposition(fileName, "UTF-8"), contentLength, eTag);
    }

    public static FileInfo getNewAttachment(File file) {
        return getNewAttachment(file.getName(), file.length(), String.valueOf(file.lastModified()));
    }

    public String contentType() {
        return contentType;
    }

    public FileInfo contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public boolean needCheck() {
        return needCheck;
    }

    public FileInfo needCheck(boolean needCheck) {
        this.needCheck = needCheck;
        return this;
    }

    public String contentDisposition() {
        return contentDisposition;
    }

    public long contentLength() {
        return contentLength;
    }

    public String eTag() {
        return eTag;
    }

}
