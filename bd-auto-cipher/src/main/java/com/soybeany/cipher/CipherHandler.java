package com.soybeany.cipher;

/**
 * 对于不同的环境，可按需配置加解密方式，使用{@link #setupCallback}
 *
 * @author Soybeany
 * @date 2021/6/1
 */
public class CipherHandler {

    private static Callback INSTANCE = new DefaultCallback();

    public static void setupCallback(Callback callback) {
        INSTANCE = callback;
    }

    public static String encrypt(String info) {
        return INSTANCE.onEncrypt(info);
    }

    public static String decrypt(String info) {
        return INSTANCE.onDecrypt(info);
    }

    // ********************内部类********************

    public interface Callback {
        String onDecrypt(String info);

        String onEncrypt(String info);
    }

    public static class DefaultCallback implements Callback {
        @Override
        public String onDecrypt(String info) {
            return info;
        }

        @Override
        public String onEncrypt(String info) {
            return info;
        }
    }

}
