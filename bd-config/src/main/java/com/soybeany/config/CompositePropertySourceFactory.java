package com.soybeany.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

/**
 * 支持自定义yml文件的加载
 *
 * @author Soybeany
 * @date 2020/11/30
 */
public class CompositePropertySourceFactory extends DefaultPropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        String sourceName = Optional.ofNullable(name).orElse(resource.getResource().getFilename());
        if (null != sourceName) {
            if (!resource.getResource().exists()) {
                // return an empty Properties
                return new PropertiesPropertySource(sourceName, new Properties());
            } else if (sourceName.endsWith(".yml") || sourceName.endsWith(".yaml")) {
                Properties propertiesFromYaml = loadYaml(resource);
                onLoadYml(propertiesFromYaml);
                return new PropertiesPropertySource(sourceName, propertiesFromYaml);
            }
        }
        return super.createPropertySource(name, resource);
    }

    protected void onLoadYml(Properties yml) {
        // 子类实现
    }

    /**
     * load yaml file to properties
     */
    private Properties loadYaml(EncodedResource resource) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(resource.getResource());
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}
