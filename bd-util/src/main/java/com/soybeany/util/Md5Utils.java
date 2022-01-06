package com.soybeany.util;

import com.soybeany.exception.BdException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Soybeany
 * @date 2018/9/11
 */
public class Md5Utils {

    public static String strToMd5(String msg) {
        return bytesToMd5(msg.getBytes(StandardCharsets.UTF_8));
    }

    public static String bytesToMd5(byte[] input) {
        return HexUtils.bytesToHex(bytesToMd5Bytes(input));
    }

    public static byte[] bytesToMd5Bytes(byte[] input) {
        try {
            return MessageDigest.getInstance("MD5").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new BdException(e.getMessage());
        }
    }

}
