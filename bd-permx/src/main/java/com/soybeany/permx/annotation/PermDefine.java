package com.soybeany.permx.annotation;

import org.springframework.web.bind.annotation.RequestBody;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限定义
 * <br>使用jackson对字段(int、long、string)自动加解密，一般为ID,防横向越权
 * <br>* deserialize时必须使用{@link RequestBody}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PermDefine {
    /**
     * 权限的名称
     */
    String value();

    /**
     * 描述权限
     */
    String desc() default "";
}
