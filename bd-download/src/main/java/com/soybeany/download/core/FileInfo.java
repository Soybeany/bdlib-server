package com.soybeany.download.core;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author Soybeany
 * @date 2022/2/14
 */
@Data
@Accessors(fluent = true, chain = true)
public class FileInfo {
    private String contentType = "application/octet-stream";
    private final String contentDisposition;
    private final long contentLength;
    private final String eTag;

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
        return getNewAttachment(file.getName(), file.length(), file.lastModified() + "");
    }

}
