package com.soybeany.cipher;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 使用jackson对字段(int、long、string)自动加解密，一般为ID,防横向越权
 * <br>* deserialize时必须使用{@link RequestBody}
 *
 * @author Soybeany
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@JacksonAnnotationsInside
@JsonDeserialize(converter = Converter.Decrypt.class)
@JsonSerialize(converter = Converter.Encrypt.class)
public @interface Cipher {
}
