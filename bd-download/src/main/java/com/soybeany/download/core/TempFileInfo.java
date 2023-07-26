package com.soybeany.download.core;

import com.soybeany.util.file.BdFileUtils;

import java.io.File;

/**
 * @author Soybeany
 * @date 2022/2/15
 */
public class TempFileInfo {

    private final File tempFile;
    private final String eTag;

    public static TempFileInfo getNew(String tempFileDir) {
        return new TempFileInfo(new File(tempFileDir, BdFileUtils.getUuid()), null);
    }

    public TempFileInfo(File tempFile, String eTag) {
        this.tempFile = tempFile;
        this.eTag = eTag;
    }

    public File getTempFile() {
        return tempFile;
    }

    public String getETag() {
        return eTag;
    }
}
