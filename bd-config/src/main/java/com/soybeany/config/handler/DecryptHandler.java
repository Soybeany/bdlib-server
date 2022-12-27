package com.soybeany.config.handler;

/**
 * @author Soybeany
 * @date 2022/12/27
 */
public interface DecryptHandler {

    boolean shouldDecrypt(Object value);

    String onDecrypt(String key, String rawMsg) throws Exception;

}
