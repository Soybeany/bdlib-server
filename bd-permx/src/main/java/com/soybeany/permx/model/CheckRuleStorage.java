package com.soybeany.permx.model;

import com.soybeany.permx.exception.BdPermxRtException;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class CheckRuleStorage {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final List<CheckRule> ALL_RULES = new ArrayList<>();
    private static final List<CheckRule.WithPermission> PERMISSION_RULES = new ArrayList<>();
    private static final List<CheckRule.WithAnonymity> ANONYMITY_RULES = new ArrayList<>();

    public static synchronized void addRules(List<? extends CheckRule> list) {
        for (CheckRule restrict : list) {
            addRule(restrict);
        }
    }

    public static synchronized void addRule(CheckRule rule) {
        if (rule instanceof CheckRule.WithPermission) {
            PERMISSION_RULES.add((CheckRule.WithPermission) rule);
        } else if (rule instanceof CheckRule.WithAnonymity) {
            ANONYMITY_RULES.add((CheckRule.WithAnonymity) rule);
        } else {
            throw new BdPermxRtException("无法匹配指定的约束");
        }
    }

    /**
     * 更新规则:
     * <br>先精准，后通配
     * <br>先权限，后匿名
     * <br>其余情况保持原有顺序
     */
    public static void updateAllRules() {
        List<CheckRule> fix = new ArrayList<>();
        List<CheckRule> dynamic = new ArrayList<>();

        sort(PERMISSION_RULES, fix, dynamic);
        sort(ANONYMITY_RULES, fix, dynamic);

        ALL_RULES.clear();
        ALL_RULES.addAll(fix);
        ALL_RULES.addAll(dynamic);
    }

    public static CheckRule getMatchedRule(String path) {
        for (CheckRule rule : ALL_RULES) {
            if (MATCHER.match(rule.getPattern(), path)) {
                return rule;
            }
        }
        return null;
    }

    public static boolean canAccess(CheckRule.WithPermission rule, Iterable<PermissionParts> provided) {
        return PermissionParts.hasPermissions(provided, rule.getRequiredPermissions());
    }

    public static boolean canAccess(String path, Iterable<PermissionParts> provided) {
        CheckRule rule = getMatchedRule(path);
        // 没有匹配到规则/匿名规则，则通行
        if (null == rule || rule instanceof CheckRule.WithAnonymity) {
            return true;
        }
        // 权限规则，则校验是否匹配
        if (rule instanceof CheckRule.WithPermission) {
            return canAccess((CheckRule.WithPermission) rule, provided);
        }
        throw new BdPermxRtException("使用了未知的CheckRule类型");
    }

    public static List<CheckRule> getAllRules() {
        return Collections.unmodifiableList(ALL_RULES);
    }

    private static void sort(List<? extends CheckRule> source, List<CheckRule> fix, List<CheckRule> dynamic) {
        for (CheckRule rule : source) {
            String url = rule.getPattern();
            if (url.contains("*") || url.contains("?")) {
                dynamic.add(rule);
            } else {
                fix.add(rule);
            }
        }
    }

}
