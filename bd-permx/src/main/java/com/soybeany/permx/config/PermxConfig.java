package com.soybeany.permx.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * @author Soybeany
 */
@Component
@Setter
@Getter
@ConfigurationProperties(prefix = "permx")
public class PermxConfig {

    /**
     * 定义权限
     */
    private Map<String, String> permDefine;

    /**
     * 权限url列表
     */
    private Map<String, String> perm;

    /**
     * 匿名url列表
     */
    private List<String> anon;

}
