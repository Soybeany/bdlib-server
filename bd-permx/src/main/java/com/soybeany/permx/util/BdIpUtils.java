package com.soybeany.permx.util;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Soybeany
 */
public class BdIpUtils {

    public static final String LOCAL_IPV4 = "127.0.0.1";
    public static final String LOCAL_IPV6 = "0:0:0:0:0:0:0:1";

    public static String getRemoteIp(HttpServletRequest request) throws UnknownHostException {
        String ipAddress = request.getHeader("x-forwarded-for");
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            if (LOCAL_IPV4.equals(ipAddress) || LOCAL_IPV6.equals(ipAddress)) {
                InetAddress inet = InetAddress.getLocalHost();
                ipAddress = inet.getHostAddress();
            }
        }
        //多层代理情况下，取最右边的ip，如：1.2.3.4, 1.5.6.7
        if (ipAddress != null && !"".equals(ipAddress)) {
            String[] parts = ipAddress.split("\\s*,\\s*");
            ipAddress = parts[parts.length - 1];
        }
        return ipAddress;
    }

    public static String getRemoteIp2(HttpServletRequest request) {
        try {
            return getRemoteIp(request);
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    public static boolean isInRange(String ipStart, String ipEnd, String ipTest) {
        long start = parse(ipStart);
        long end = parse(ipEnd);
        long test = parse(ipTest);
        return test >= start && test <= end;
    }

    public static long parse(String ip) {
        StringBuilder builder = new StringBuilder();
        for (String part : ip.split("\\.")) {
            int partValue = Integer.parseInt(part);
            if (partValue < 0 || partValue > 255) {
                throw new RuntimeException("ip地址(" + ip + ")解析异常");
            }
            builder.append(String.format("%03d", partValue));
        }
        return Long.parseLong(builder.toString());
    }

}
