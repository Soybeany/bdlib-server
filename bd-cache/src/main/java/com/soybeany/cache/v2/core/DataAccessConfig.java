package com.soybeany.cache.v2.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据获取配置
 *
 * @author Soybeany
 * @date 2020/7/15
 */
public class DataAccessConfig {

    /**
     * 是否允许访问数据源
     */
    public final boolean canAccessSource;

    /**
     * 临时排除使用的缓存服务id列表
     */
    public final Set<String> tempExcludedServiceIds = new HashSet<String>();

    public DataAccessConfig(boolean canAccessSource, String... tempExcludedServiceIds) {
        this.canAccessSource = canAccessSource;
        Collections.addAll(this.tempExcludedServiceIds, tempExcludedServiceIds);
    }
}
