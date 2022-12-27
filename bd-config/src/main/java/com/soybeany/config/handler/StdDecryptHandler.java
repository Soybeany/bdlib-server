package com.soybeany.config.handler;

import com.soybeany.config.BDCipherUtils;

/**
 * @author Soybeany
 * @date 2022/12/27
 */
public class StdDecryptHandler implements DecryptHandler {

    private final String prefix;
    private final String suffix;

    public StdDecryptHandler() {
        this("bdEnc://", "");
    }

    public StdDecryptHandler(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public String encrypt(String key, String msg) throws Exception {
        return prefix + BDCipherUtils.encrypt(key, msg) + suffix;
    }

    @Override
    public boolean shouldDecrypt(Object value) {
        if (null == value) {
            return false;
        }
        String str = value.toString();
        return str.startsWith(prefix) && str.endsWith(suffix);
    }

    @Override
    public String onDecrypt(String key, String rawMsg) throws Exception {
        String msg = rawMsg.substring(prefix.length(), rawMsg.length() - suffix.length());
        return BDCipherUtils.decrypt(key, msg);
    }

}
