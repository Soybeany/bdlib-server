package com.soybeany.download.core;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Soybeany
 * @date 2022/2/15
 */
@Data
@Accessors(fluent = true, chain = true)
public class DownloadConfig {

    private final String url;
    private final Map<String, String> headers = new HashMap<>();
    /**
     * 请求超时（单位：秒）
     */
    private int timeout = 10;

    public DownloadConfig header(String key, String value) {
        headers.put(key, value);
        return this;
    }

}
