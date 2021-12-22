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

    @Nullable
    public static String getSessionId(HttpServletRequest request, String sessionIdKey) {
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

    public static void setupHeader(HttpServletResponse response, String sessionIdKey, String sessionId) {
        response.setHeader(sessionIdKey, sessionId);
    }

    public static void setupCookie(HttpServletRequest request, HttpServletResponse response, String sessionIdKey, String sessionId, Integer maxAge) {
        Cookie cookie = new Cookie(sessionIdKey, sessionId);
        cookie.setPath(request.getContextPath());
        if (null != maxAge) {
            cookie.setMaxAge(maxAge);
        }
        // 添加到cookie
        response.addCookie(cookie);
    }

    // ***********************成员区****************************

    private final IDataHolder<T> sessionStorage;
    private final String sessionIdKey;

    public SessionManager(int maxSessionCount, String sessionIdKey) {
        this(new AutoUpdateMemDataHolder<>(maxSessionCount), sessionIdKey);
    }

    public SessionManager(IDataHolder<T> sessionStorage, String sessionIdKey) {
        this.sessionStorage = sessionStorage;
        this.sessionIdKey = sessionIdKey;
    }

    public Optional<T> get(String sessionId) {
        if (null == sessionId) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessionStorage.get(sessionId));
    }

    public Optional<T> get(HttpServletRequest request) {
        String sessionId = getSessionId(request, sessionIdKey);
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
        String sessionId = getSessionId(request, sessionIdKey);
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
        setupCookie(request, response, sessionIdKey, sessionId, null);
    }

    public boolean removeFromCookie(HttpServletRequest request, HttpServletResponse response) {
        String sessionId = getSessionId(request, sessionIdKey);
        boolean removed = remove(sessionId);
        if (!removed) {
            return false;
        }
        setupCookie(request, response, sessionIdKey, sessionId, 0);
        return true;
    }

}
