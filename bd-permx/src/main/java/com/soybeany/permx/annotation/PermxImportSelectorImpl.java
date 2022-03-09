package com.soybeany.permx.annotation;

import com.soybeany.permx.config.PermxConfig;
import com.soybeany.permx.core.CheckRuleHandler;
import com.soybeany.permx.core.PermDefineConsumerImpl;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import javax.annotation.Nonnull;

/**
 * @author Soybeany
 * @date 2022/3/9
 */
class PermxImportSelectorImpl implements ImportSelector {
    @Nonnull
    @Override
    public String[] selectImports(@Nonnull AnnotationMetadata importingClassMetadata) {
        return new String[]{
                PermxConfig.class.getName(),
                CheckRuleHandler.class.getName(),
                PermDefineConsumerImpl.class.getName(),
        };
    }
}
