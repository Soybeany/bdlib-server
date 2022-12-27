package com.soybeany.config.handler;

/**
 * @author Soybeany
 * @date 2022/12/27
 */
public class JasyptDecryptHandler extends StdDecryptHandler {
    public JasyptDecryptHandler() {
        super("ENC(", ")");
    }
}
