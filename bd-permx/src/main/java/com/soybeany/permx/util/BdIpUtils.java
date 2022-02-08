package com.soybeany.permx.util;

import com.soybeany.permx.exception.BdPermxRtException;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Soybeany
 */
public class BdIpUtils {

    public static final String LOCAL_IPV4 = "127.0.0.1";
    public static final String LOCAL_IPV6 = "0:0:0:0:0:0:0:1";

    public static String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    public static String getRemoteIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("x-forwarded-for");
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            if (LOCAL_IPV4.equals(ipAddress) || LOCAL_IPV6.equals(ipAddress)) {
                ipAddress = getLocalIp();
            }
        }
        //多层代理情况下，取最右边的ip，如：1.2.3.4, 1.5.6.7
        if (ipAddress != null && !"".equals(ipAddress)) {
            String[] parts = ipAddress.split("\\s*,\\s*");
            ipAddress = parts[parts.length - 1];
        }
        return null != ipAddress ? ipAddress.trim() : null;
    }

    public static boolean isInRange(String ipStart, String ipEnd, String ipTest) {
        return isInRange(ipStart, ipEnd, parse(ipTest));
    }

    public static boolean isInRange(String ipStart, String ipEnd, long ipTest) {
        long start = parse(ipStart);
        long end = parse(ipEnd);
        return ipTest >= start && ipTest <= end;
    }

    public static long parse(String ip) {
        StringBuilder builder = new StringBuilder();
        for (String part : ip.split("\\.")) {
            int partValue = Integer.parseInt(part.trim());
            if (partValue < 0 || partValue > 255) {
                throw new BdPermxRtException("ip地址(" + ip + ")解析异常");
            }
            builder.append(String.format("%03d", partValue));
        }
        return Long.parseLong(builder.toString());
    }

}
