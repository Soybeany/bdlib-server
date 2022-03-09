package com.soybeany.permx.core;

import com.soybeany.permx.config.PermxConfig;
import com.soybeany.permx.model.PermissionDefine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Soybeany
 * @date 2022/1/13
 */
@SuppressWarnings("SpringJavaAutowiredMembersInspection")
public class PermDefineConsumerImpl implements PermDefineConsumer {

    @Autowired
    private PermxConfig permxConfig;
    @Nullable
    @Autowired(required = false)
    private PermDefineProvider permDefineProvider;

    @Override
    public Set<PermissionDefine> getPermDefines() {
        Set<PermissionDefine> result = new HashSet<>();
        // 先读取yml
        permxConfig.getPermDefine().forEach((value, content) -> {
            String[] parts = content.split(CONTENT_SEPARATOR);
            String desc = parts.length > 1 ? parts[1] : "";
            result.add(new PermissionDefine(parts[0], value, desc));
        });
        // 再读取提供者，遇到相同定义则覆盖yml中的记录
        Set<PermissionDefine> additional;
        if (null != permDefineProvider && null != (additional = permDefineProvider.onGetPermDefines())) {
            result.addAll(additional);
        }
        return result;
    }

    @Override
    public Map<String, PermissionDefine> getPermDefinesMap() {
        Map<String, PermissionDefine> result = new HashMap<>();
        for (PermissionDefine define : getPermDefines()) {
            result.put(define.getValue(), define);
        }
        return result;
    }

    @Override
    public Set<String> getPermValueSet() {
        Set<String> result = new HashSet<>();
        for (PermissionDefine define : getPermDefines()) {
            result.add(define.getValue());
        }
        return result;
    }
}
