package com.soybeany.config;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * @author Soybeany
 * @date 2020/6/2
 */
public class BDCipherUtils {

    private static final String DEFAULT_PROTOCOL = "bdEnc";
    private static final String PROTOCOL_SEPARATOR = "://";

    private static final Base64.Encoder ENCODER = Base64.getEncoder();
    private static final Base64.Decoder DECODER = Base64.getDecoder();

    private static String PROTOCOL;
    private static ICipher CIPHER = new AesCipherImpl();

    static {
        setupProtocol(DEFAULT_PROTOCOL);
    }

    // ****************************************配置****************************************

    public static void setupProtocol(String protocol) {
        if (null != protocol) {
            PROTOCOL = protocol + PROTOCOL_SEPARATOR;
        }
    }

    public static void setupCipher(ICipher cipher) {
        if (null != cipher) {
            CIPHER = cipher;
        }
    }

    // ****************************************加解密API****************************************

    /**
     * 加密，并附带协议头
     */
    public static String encryptWithProtocol(String key, String rawMsg) throws Exception {
        return PROTOCOL + encrypt(key, rawMsg);
    }

    /**
     * 是否有使用指定协议
     */
    public static boolean isWithProtocol(Object msg) {
        return msg.toString().startsWith(PROTOCOL);
    }

    /**
     * 解密，如果附带协议头
     */
    public static String decryptIfWithProtocol(String key, String msg) throws Exception {
        if (!isWithProtocol(msg)) {
            return msg;
        }
        return decrypt(key, msg.substring(PROTOCOL.length()));
    }

    /**
     * 使用指定密钥加密
     */
    public static String encrypt(String key, String rawMsg) throws Exception {
        return CIPHER.onEncrypt(key, rawMsg);
    }

    /**
     * 使用指定密钥解密
     */
    public static String decrypt(String key, String encryptedMsg) throws Exception {
        return CIPHER.onDecrypt(key, encryptedMsg);
    }

    // ****************************************BASE64API****************************************

    public static String encodeWithBase64(String msg) {
        return encodeWithBase64(msg.getBytes(StandardCharsets.UTF_8));
    }

    public static String encodeWithBase64(byte[] bytes) {
        return ENCODER.encodeToString(bytes);
    }

    public static String decodeWithBase64(String msg) {
        return new String(decodeWithBase64Raw(msg), StandardCharsets.UTF_8);
    }

    public static byte[] decodeWithBase64Raw(String msg) {
        return DECODER.decode(msg);
    }

    // ****************************************MD5API****************************************

    public static String calculateMd5(String msg) throws Exception {
        return calculateMd5(msg, "");
    }

    public static String calculateMd5(String msg, String salt) throws Exception {
        String str = msg + salt;
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(str.getBytes(StandardCharsets.UTF_8));
        byte[] s = digest.digest();
        StringBuilder result = new StringBuilder();
        for (byte b : s) {
            result.append(Integer.toHexString((0x000000FF & b) | 0xFFFFFF00).substring(6));
        }
        return result.toString();
    }

    // ****************************************接口****************************************

    public interface ICipher {
        String onEncrypt(String key, String rawMsg) throws Exception;

        String onDecrypt(String key, String encryptedMsg) throws Exception;
    }

    // ****************************************内部实现****************************************

    private static class AesCipherImpl implements ICipher {
        private static final String AES = "AES";

        @Override
        public String onEncrypt(String key, String rawMsg) throws Exception {
            // 根据指定算法AES自成密码器
            Cipher cipher = Cipher.getInstance(AES);
            // 初始化密码器，第一个参数为加密(Encrypt_mode)或者解密解密(Decrypt_mode)操作，第二个参数为使用的KEY
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(key));
            // 获取加密内容的字节数组(这里要设置为utf-8)不然内容中如果有中文和英文混合中文就会解密为乱码
            byte[] byteEncode = rawMsg.getBytes(StandardCharsets.UTF_8);
            // 根据密码器的初始化方式--加密：将数据加密
            byte[] byteAes = cipher.doFinal(byteEncode);
            // 将加密后的数据转换为字符串
            return encodeWithBase64(byteAes);
        }

        @Override
        public String onDecrypt(String key, String encryptedMsg) throws Exception {
            // 根据指定算法AES自成密码器
            Cipher cipher = Cipher.getInstance(AES);
            // 初始化密码器，第一个参数为加密(Encrypt_mode)或者解密(Decrypt_mode)操作，第二个参数为使用的KEY
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(key));
            // 将加密并编码后的内容解码成字节数组
            byte[] byteContent = decodeWithBase64Raw(encryptedMsg);
            byte[] byteDecode = cipher.doFinal(byteContent);
            return new String(byteDecode, StandardCharsets.UTF_8);
        }

        private SecretKey getSecretKey(String key) throws Exception {
            // 根据字节数组生成AES密钥
            return new SecretKeySpec(calculateMd5(key).getBytes(), AES);
        }
    }

}
