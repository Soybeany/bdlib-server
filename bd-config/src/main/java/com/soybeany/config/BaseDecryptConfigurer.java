package com.soybeany.config;

import org.springframework.boot.env.OriginTrackedMapPropertySource;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginTrackedValue;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.lang.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 适配至spring-boot 2.4.2
 *
 * @author Soybeany
 * @date 2020/6/3
 */
public abstract class BaseDecryptConfigurer extends PropertySourcesPlaceholderConfigurer {

    @Override
    @NonNull
    public PropertySources getAppliedPropertySources() throws IllegalStateException {
        PropertySources sources = super.getAppliedPropertySources();
        Optional.ofNullable(sources.get("environmentProperties"))
                .map(PropertySource::getSource)
                .map(environment -> ((ConfigurableEnvironment) environment).getPropertySources())
                // 筛选出应用配置
                .map(mps -> mps.stream().filter(ps -> ps instanceof OriginTrackedMapPropertySource))
                // 对应用配置进行解密
                .ifPresent(stream -> stream.forEach(this::decrypt));
        return sources;
    }

    private void decrypt(PropertySource<?> applicationConfig) {
        try {
            Map<Object, Object> map = new HashMap<>();
            Class<?> clazz = Class.forName("org.springframework.boot.origin.OriginTrackedValue$OriginTrackedCharSequence");
            Map<?, ?> source2 = (Map<?, ?>) applicationConfig.getSource();
            String key = setupKey();
            for (Map.Entry<?, ?> entry : source2.entrySet()) {
                Object value = entry.getValue();
                if (clazz.isInstance(value) && BDCipherUtils.isWithProtocol(value)) {
                    OriginTrackedValue v = (OriginTrackedValue) value;
                    String dMsg = BDCipherUtils.decryptIfWithProtocol(key, value.toString());
                    Constructor<?> constructor = clazz.getDeclaredConstructor(CharSequence.class, Origin.class);
                    constructor.setAccessible(true);
                    value = constructor.newInstance(dMsg, v.getOrigin());
                }
                map.put(entry.getKey(), value);
            }
            Field field = PropertySource.class.getDeclaredField("source");
            field.setAccessible(true);
            field.set(applicationConfig, Collections.unmodifiableMap(map));
        } catch (Exception e) {
            throw new RuntimeException("配置解密异常", e);
        }
    }

    protected abstract String setupKey();

}
