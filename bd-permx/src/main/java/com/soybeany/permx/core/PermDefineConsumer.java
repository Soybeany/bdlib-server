package com.soybeany.permx.core;

import com.soybeany.permx.model.PermissionDefine;

import java.util.Map;
import java.util.Set;

/**
 * @author Soybeany
 * @date 2021/5/26
 */
@SuppressWarnings("unused")
public interface PermDefineConsumer {

    String CONTENT_SEPARATOR = "@";

    Set<PermissionDefine> getPermDefines();

    /**
     * key为{@link PermissionDefine#getValue}
     */
    Map<String, PermissionDefine> getPermDefinesMap();

    Set<String> getPermValueSet();

}
