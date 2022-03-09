package com.soybeany.permx.annotation;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author Soybeany
 * @date 2022/3/9
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(PermxImportSelectorImpl.class)
public @interface EnablePermx {
}
