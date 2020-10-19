package com.soybeany.util;

/**
 * 16进制工具类
 * <br>Created by Soybeany on 2020/10/19.
 */
public class HexUtils {

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte aByte : bytes) {
            String hex = Integer.toHexString(aByte & 0xFF);
            if (hex.length() < 2) {
                sb.append(0);
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public static byte[] hexToByteArray(String inHex) {
        int hexLen = inHex.length();
        byte[] result;
        if (hexLen % 2 == 1) {
            //奇数
            hexLen++;
            result = new byte[(hexLen / 2)];
            inHex = "0" + inHex;
        } else {
            //偶数
            result = new byte[(hexLen / 2)];
        }
        int j = 0;
        for (int i = 0; i < hexLen; i += 2) {
            result[j] = hexToByte(inHex.substring(i, i + 2));
            j++;
        }
        return result;
    }

    public static String byteToHex(byte b) {
        String hex = Integer.toHexString(b & 0xFF);
        if (hex.length() < 2) {
            hex = "0" + hex;
        }
        return hex;
    }

    public static byte hexToByte(String inHex) {
        return (byte) Integer.parseInt(inHex, 16);
    }

}
