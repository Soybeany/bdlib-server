package com.soybeany.permx.core;

import com.soybeany.permx.annotation.RequireAnonymity;
import com.soybeany.permx.annotation.RequireLogin;
import com.soybeany.permx.annotation.RequirePermissions;
import com.soybeany.permx.config.PermxConfig;
import com.soybeany.permx.exception.BdPermxRtException;
import com.soybeany.permx.model.CheckRule;
import com.soybeany.permx.model.CheckRule.WithAnonymity;
import com.soybeany.permx.model.CheckRule.WithPermission;
import com.soybeany.permx.model.CheckRuleStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
public class CheckRuleHandler implements ServletContextListener {

    @Autowired
    private PermxConfig permxConfig;
    @Autowired
    private PermDefineConsumer permDefineConsumer;
    @Autowired
    private RequestMappingHandlerMapping mapping;

    private static final Map<Class<?>, CheckRuleProvider> RESTRICT_PROVIDER_MAPPING = new HashMap<Class<?>, CheckRuleProvider>() {{
        put(RequireAnonymity.class, (url, annotation, permDefines) -> {
            CheckRule.WithAnonymity restrict = new CheckRule.WithAnonymity();
            restrict.setPattern(url);
            return restrict;
        });
        put(RequirePermissions.class, (url, annotation, permDefines) -> {
            CheckRule.WithPermission restrict = new CheckRule.WithPermission();
            restrict.setPattern(url);
            RequirePermissions permissions = (RequirePermissions) annotation;
            Set<String> requiredPermissions = restrict.getRequiredPermissions();
            for (String permission : permissions.value()) {
                if (!permDefines.contains(permission)) {
                    throw new BdPermxRtException("使用了未定义的权限:" + permission);
                }
                requiredPermissions.add(permission);
            }
            return restrict;
        });
        put(RequireLogin.class, (url, annotation, permDefines) -> {
            CheckRule.WithPermission restrict = new CheckRule.WithPermission();
            restrict.setPattern(url);
            return restrict;
        });
    }};

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Set<String> permDefines = permDefineConsumer.getPermValueSet();
        // 先添加代码中指定的配置
        mapping.getHandlerMethods().forEach((info, method) -> onHandleMethod(info, method, permDefines));
        // 再添加yml中指定的配置
        CheckRuleStorage.addRules(WithPermission.fromEntityMap(permDefines, permxConfig.getPerm()));
        CheckRuleStorage.addRules(WithAnonymity.fromPatternList(permxConfig.getAnon()));
        // 更新全局配置
        CheckRuleStorage.updateAllRules();
    }

    // ********************内部方法********************

    private void onHandleMethod(RequestMappingInfo info, HandlerMethod method, Set<String> permDefines) {
        for (Annotation annotation : method.getMethod().getAnnotations()) {
            CheckRuleProvider provider = RESTRICT_PROVIDER_MAPPING.get(annotation.annotationType());
            if (null == provider) {
                continue;
            }
            for (String url : info.getPatternValues()) {
                CheckRule rule = provider.get(url, annotation, permDefines);
                CheckRuleStorage.addRule(rule);
            }
            return;
        }
    }

    // ****************************************内部类****************************************

    private interface CheckRuleProvider {
        CheckRule get(String url, Annotation annotation, Set<String> permDefines);
    }

}
