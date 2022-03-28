package com.soybeany.download.core;

import com.soybeany.exception.BdIoException;

/**
 * @author Soybeany
 * @date 2022/2/15
 */
public class BdDownloadException extends BdIoException {

    public BdDownloadException(String msg) {
        super(msg);
    }

}
