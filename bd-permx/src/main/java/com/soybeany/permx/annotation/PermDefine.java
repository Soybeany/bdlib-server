package com.soybeany.permx.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Soybeany
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PermDefine {
    /**
     * 权限的名称
     */
    String name();

    /**
     * 描述权限
     */
    String desc() default "";
}
