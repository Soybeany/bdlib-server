package com.soybeany.permx.core;

import com.soybeany.util.cache.AutoUpdateMemDataHolder;
import com.soybeany.util.cache.IDataHolder;
import com.soybeany.util.file.BdFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@Slf4j
@SuppressWarnings("UnusedReturnValue")
public class SessionManager<T> {

    private final IDataHolder<T> sessionStorage;
    private final String sessionIdKey;

    public SessionManager(int maxSessionCount, String sessionIdKey) {
        sessionStorage = new AutoUpdateMemDataHolder<>(maxSessionCount);
        this.sessionIdKey = sessionIdKey;
    }

    public Optional<T> get(String sessionId) {
        if (null == sessionId) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessionStorage.get(sessionId));
    }

    public Optional<T> get(HttpServletRequest request) {
        String sessionId = getSessionId(request);
        return get(sessionId);
    }

    /**
     * @return sessionId
     */
    public String set(T value, int expiryInSec) {
        String token = BdFileUtils.getUuid();
        sessionStorage.put(token, value, expiryInSec);
        return token;
    }

    /**
     * 是否移除成功
     */
    public boolean remove(HttpServletRequest request) {
        String sessionId = getSessionId(request);
        return remove(sessionId);
    }

    /**
     * 是否移除成功
     */
    public boolean remove(String sessionId) {
        if (null == sessionId) {
            return false;
        }
        return (null != sessionStorage.remove(sessionId));
    }

    public void clear() {
        sessionStorage.clear();
    }

    // ********************Cookie********************

    public void setToCookie(HttpServletRequest request, HttpServletResponse response, T value, int expiryInSec) {
        String sessionId = set(value, expiryInSec);
        setupCookie(request, response, sessionId, null);
    }

    public boolean removeFromCookie(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = getSessionId(request);
        boolean removed = remove(sessionId);
        if (!removed) {
            return false;
        }
        setupCookie(request, response, sessionId, 0);
        return true;
    }

    // ********************Header********************

    public void setToHeader(HttpServletResponse response, T value, int expiryInSec) {
        String sessionId = set(value, expiryInSec);
        response.addHeader(sessionIdKey, sessionId);
    }

    // ********************内部方法********************

    private void setupCookie(HttpServletRequest request, HttpServletResponse response, String sessionId, Integer maxAge) {
        Cookie cookie = new Cookie(sessionIdKey, sessionId);
        cookie.setPath(request.getContextPath());
        if (null != maxAge) {
            cookie.setMaxAge(maxAge);
        }
        // 添加到cookie
        response.addCookie(cookie);
    }

    @Nullable
    private String getSessionId(HttpServletRequest request) {
        // 先尝试从header中获取
        String sessionId = request.getHeader(sessionIdKey);
        if (null != sessionId) {
            return sessionId;
        }
        // 没有则从cookie中获取
        Cookie[] cookies = request.getCookies();
        if (null != cookies) {
            for (Cookie cookie : cookies) {
                if (sessionIdKey.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}
