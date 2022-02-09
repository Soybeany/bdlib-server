package com.soybeany.permx.model;

import com.soybeany.permx.exception.BdPermxRtException;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.*;

@Data
public abstract class CheckRule {

    private String pattern;

    // ********************内部类********************

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class WithPermission extends CheckRule {
        private final Set<String> requiredPermissions = new HashSet<>();

        public static List<WithPermission> fromEntityMap(Set<String> permissionDefines, Map<String, String> map) {
            List<WithPermission> result = new ArrayList<>();
            map.forEach((pattern, permissions) -> {
                WithPermission restrict = new WithPermission();
                restrict.setPattern(pattern);
                Set<String> requiredPermissions = restrict.getRequiredPermissions();
                for (String permission : permissions.split("\\s*,\\s*")) {
                    if (!permissionDefines.contains(permission)) {
                        throw new BdPermxRtException("使用了未定义的权限“" + permission + "”，请先在permDefine中定义该权限");
                    }
                    requiredPermissions.add(permission);
                }
                result.add(restrict);
            });
            return result;
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class WithAnonymity extends CheckRule {

        public static List<WithAnonymity> fromPatternList(List<String> list) {
            List<WithAnonymity> result = new ArrayList<>();
            for (String pattern : list) {
                WithAnonymity restrict = new WithAnonymity();
                restrict.setPattern(pattern);
                result.add(restrict);
            }
            return result;
        }
    }

}
