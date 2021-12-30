package com.soybeany.permx.core;

import com.soybeany.permx.model.CheckRule;
import com.soybeany.permx.model.CheckRuleStorage;
import com.soybeany.permx.model.PermissionParts;
import lombok.AllArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * 可使用{@link WebMvcConfigurer#addInterceptors}添加拦截器，如
 * <br/>registry.addInterceptor(new AuthInterceptor(callback)).addPathPatterns("/**").order(-1);
 */
public class AuthInterceptor implements HandlerInterceptor {

    private final Callback mCallback;

    public AuthInterceptor(Callback callback) {
        mCallback = callback;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            mCallback.onStartRequest(request);

            String path = request.getServletPath();
            CheckRule rule = CheckRuleStorage.getMatchedRule(path);
            // 若没有匹配到pattern，则特殊处理
            if (null == rule) {
                // 404的响应不处理
                if (HttpServletResponse.SC_NOT_FOUND == response.getStatus()) {
                    return true;
                }
                // 只要是已登录用户，即可访问
                checkIsLogin();
                return true;
            }
            // 若允许匿名访问，则放行
            if (rule instanceof CheckRule.WithAnonymity) {
                return true;
            }
            // 先检查是否已登录
            checkIsLogin();
            // 再检查是否有权限
            Collection<PermissionParts> providedPermissions = Optional.ofNullable(mCallback.onGetPermissions()).orElseGet(Collections::emptyList);
            if (!CheckRuleStorage.canAccess((CheckRule.WithPermission) rule, providedPermissions)) {
                throw new InnerException(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
            }
        } catch (Exception e) {
            response.setContentType("text/plain; charset=utf-8");
            if (e instanceof InnerException) {
                InnerException innerException = (InnerException) e;
                setupResponse(response, innerException.code, innerException.msg);
            } else {
                setupResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            }
            mCallback.onFinishRequest();
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 只有preHandle返回true时，才会执行此回调
        mCallback.onFinishRequest();
    }

    // ********************内部方法********************

    private void checkIsLogin() throws InnerException {
        if (!mCallback.isLogin()) {
            throw new InnerException(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        }
    }

    private void setupResponse(HttpServletResponse response, int status, String msg) throws Exception {
        response.setStatus(status);
        response.getWriter().print(msg);
    }

    // ********************内部类********************

    public interface Callback {

        /**
         * 请求开始时执行的回调
         */
        default void onStartRequest(HttpServletRequest request) {
        }

        /**
         * 请求结束时执行的回调
         */
        default void onFinishRequest() {
        }

        /**
         * 需要提供当前用户全部权限时的回调
         */
        @Nullable
        Collection<PermissionParts> onGetPermissions();

        /**
         * 当前是否已登录
         */
        boolean isLogin();
    }

    @AllArgsConstructor
    private static class InnerException extends Exception {
        int code;
        String msg;
    }

}
