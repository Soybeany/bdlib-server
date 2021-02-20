package com.soybeany.cache.v2.dm;

import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.core.DataManager;
import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.log.ConsoleLogger;
import com.soybeany.cache.v2.strategy.LruMemCacheStrategy;
import com.soybeany.cache.v2.strategy.LruMemTimerCacheStrategy;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

/**
 * @author Soybeany
 * @date 2021/2/20
 */
public class LruMemTimerStrategyTest {

    IDatasource<String, String> datasource = s -> {
        System.out.println("“" + s + "”access datasource");
        return UUID.randomUUID().toString();
    };

    LruMemCacheStrategy<String, String> cacheStrategy = (LruMemCacheStrategy<String, String>) new LruMemTimerCacheStrategy<String, String>()
            .expiry(500);

    private final DataManager<String, String> dataManager = DataManager.Builder
            .get("LRU定时器策略测试", datasource)
            .withCache(cacheStrategy)
            .logger(new ConsoleLogger<>())
            .build();

    @BeforeClass
    public static void beforeTest() {
        LruMemTimerCacheStrategy.createTimer();
    }

    @AfterClass
    public static void afterTest() {
        LruMemTimerCacheStrategy.destroyTimer();
    }

    @Test
    public void test() throws Exception {
        String key1 = "key1";
        String key2 = "key2";
        // 缓存数据
        dataManager.getData("序列1", key1);
        dataManager.getData("序列2", key2);
        // 延长数据失效时间
        Thread.sleep(250);
        dataManager.getData("序列1", key1);
        // 检验
        Thread.sleep(300);
        try {
            cacheStrategy.onGetCache(null, key2);
            throw new Exception("不允许还持有缓存");
        } catch (NoCacheException ignore) {
        }
        assert cacheStrategy.size() == 1;
        Thread.sleep(250);
        assert cacheStrategy.size() == 0;
    }

}
