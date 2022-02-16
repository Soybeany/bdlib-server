package com.soybeany.download.core;

import com.soybeany.util.file.BdFileUtils;
import lombok.Data;

import java.io.File;

/**
 * @author Soybeany
 * @date 2022/2/15
 */
@Data
public class TempFileInfo {

    private final File tempFile;
    private final String eTag;

    public static TempFileInfo getNew(String tempFileDir) {
        return new TempFileInfo(new File(tempFileDir, BdFileUtils.getUuid()), null);
    }

}
