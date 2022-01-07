package com.soybeany.permx.core;

import com.soybeany.util.cache.IDataHolder;
import com.soybeany.util.cache.StdMemDataHolder;
import com.soybeany.util.file.BdFileUtils;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;
import java.util.Optional;

@Slf4j
@SuppressWarnings("UnusedReturnValue")
public class SessionManager<T> {

    private final IDataHolder<T> sessionStorage;
    private final String sessionIdKey;

    @Setter
    @Accessors(fluent = true, chain = true)
    private int defaultExpiryInSec = 10;

    @Nullable
    public static String getSessionId(HttpServletRequest request, String sessionIdKey) {
        // 先尝试从header中获取
        String sessionId = request.getHeader(sessionIdKey);
        if (null != sessionId) {
            return sessionId;
        }
        // 没有则从cookie中获取
        Cookie[] cookies = request.getCookies();
        if (null == cookies) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (sessionIdKey.equals(cookie.getName())) {
                return cookie.getValue();
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
        response.addCookie(cookie);
    }

    // ***********************成员区****************************

    public SessionManager(int maxSessionCount, String sessionIdKey) {
        this(new StdMemDataHolder<>(maxSessionCount), sessionIdKey);
    }

    public SessionManager(IDataHolder<T> sessionStorage, String sessionIdKey) {
        this.sessionStorage = sessionStorage;
        this.sessionIdKey = sessionIdKey;
    }

    public Optional<T> get(HttpServletRequest request) {
        String sessionId = getSessionId(request, sessionIdKey);
        return get(sessionId);
    }

    public Optional<T> get(String sessionId) {
        return get(sessionId, null);
    }

    public Optional<T> get(String sessionId, DataProvider<T> dataProvider) {
        return get(sessionId, dataProvider, null);
    }

    public Optional<T> get(String sessionId, DataProvider<T> dataProvider, DataProvider<Integer> expiryProvider) {
        // 没有指定sessionId，则直接返回
        if (null == sessionId) {
            return Optional.empty();
        }
        // 若有数据，或没有指定数据提供者，则直接返回
        T result = sessionStorage.get(sessionId);
        if (null != result || null == dataProvider) {
            return Optional.ofNullable(result);
        }
        // 若没有获取到数据，则直接返回
        result = dataProvider.onGet(sessionId);
        if (null == result) {
            return Optional.empty();
        }
        // 保存数据并返回
        Integer expiry = null;
        if (null != expiryProvider) {
            expiry = expiryProvider.onGet(sessionId);
        }
        if (null == expiry) {
            expiry = defaultExpiryInSec;
        }
        set(sessionId, result, expiry);
        return Optional.of(result);
    }

    /**
     * @return sessionId
     */
    public String set(T value, int expiryInSec) {
        String sessionId = BdFileUtils.getUuid();
        set(sessionId, value, expiryInSec);
        return sessionId;
    }

    public void set(String sessionId, T value, int expiryInSec) {
        sessionStorage.put(sessionId, value, expiryInSec);
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

    public Collection<T> getAll() {
        return sessionStorage.getAll();
    }

    public void clear() {
        sessionStorage.clear();
    }

    // ***********************Header****************************

    public void setToHeader(HttpServletResponse response, T value, int expiryInSec) {
        String sessionId = set(value, expiryInSec);
        setupHeader(response, sessionIdKey, sessionId);
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

    // ***********************内部类****************************

    public interface DataProvider<T> {
        T onGet(String sessionId);
    }

}
