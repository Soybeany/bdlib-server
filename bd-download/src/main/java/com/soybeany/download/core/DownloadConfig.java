package com.soybeany.download.core;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Soybeany
 * @date 2022/2/15
 */
public class DownloadConfig {

    private final String url;
    private final Map<String, String> headers = new HashMap<>();
    /**
     * 请求超时（单位：秒）
     */
    private int timeout = 10;

    public DownloadConfig(String url) {
        this.url = url;
    }

    public DownloadConfig header(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public String url() {
        return url;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public int timeout() {
        return timeout;
    }

    public DownloadConfig timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }
}
