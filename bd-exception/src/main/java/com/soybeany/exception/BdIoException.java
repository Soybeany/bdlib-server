package com.soybeany.exception;

import java.io.IOException;

/**
 * @author Soybeany
 * @date 2022/1/6
 */
public class BdIoException extends IOException {

    public BdIoException(String message) {
        super(message);
    }

}
