package com.soybeany.cache.v2.module;

/**
 * @author Soybeany
 * @date 2020/7/15
 */
public enum DataGetMode {
    // 常规
    NORM,
    // 直接访问数据源
    DIRECT,
    // 只访问缓存
    CACHE
}
