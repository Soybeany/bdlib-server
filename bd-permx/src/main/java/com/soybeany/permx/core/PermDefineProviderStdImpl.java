package com.soybeany.permx.core;

import com.soybeany.permx.annotation.PermDefine;
import com.soybeany.permx.exception.BdPermxRtException;
import com.soybeany.permx.model.PermissionDefine;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Soybeany
 * @date 2021/5/26
 */
public abstract class PermDefineProviderStdImpl implements PermDefineProvider {
    @Override
    public Set<PermissionDefine> onGetPermDefines() {
        Set<PermissionDefine> result = new HashSet<>();
        try {
            for (Field field : onGetDefineClass().getFields()) {
                PermDefine permDefine = field.getAnnotation(PermDefine.class);
                if (null == permDefine) {
                    continue;
                }
                PermissionDefine define = new PermissionDefine(permDefine.name(), (String) field.get(null), permDefine.desc());
                result.add(define);
            }
        } catch (Exception e) {
            throw new BdPermxRtException("权限提取异常:" + "”" + e.getMessage() + "“");
        }
        return result;
    }

    protected abstract Class<?> onGetDefineClass();

}
