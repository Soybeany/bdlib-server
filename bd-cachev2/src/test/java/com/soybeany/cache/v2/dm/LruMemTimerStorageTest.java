package com.soybeany.cache.v2.dm;

import com.soybeany.cache.v2.contract.ICacheStorage;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.core.DataManager;
import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.log.ConsoleLogger;
import com.soybeany.cache.v2.storage.LruMemTimerCacheStorageBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

/**
 * @author Soybeany
 * @date 2021/2/20
 */
public class LruMemTimerStorageTest {

    IDatasource<String, String> datasource = s -> {
        System.out.println("“" + s + "”access datasource");
        return UUID.randomUUID().toString();
    };

    ICacheStorage<String, String> cacheStorage = new LruMemTimerCacheStorageBuilder<String, String>().pTtl(500).build();

    private final DataManager<String, String> dataManager = DataManager.Builder
            .get("LRU定时器存储器测试", datasource)
            .withCache(cacheStorage)
            .logger(new ConsoleLogger<>())
            .build();

    @BeforeClass
    public static void beforeTest() {
        LruMemTimerCacheStorageBuilder.createTimer();
    }

    @AfterClass
    public static void afterTest() {
        LruMemTimerCacheStorageBuilder.destroyTimer();
    }

    @Test
    public void test() throws Exception {
        String key1 = "key1";
        String key2 = "key2";
        // 缓存数据
        dataManager.getData(key1);
        dataManager.getData(key2);
        // 延长数据失效时间
        Thread.sleep(250);
        dataManager.getData(key1);
        // 检验
        Thread.sleep(300);
        try {
            cacheStorage.onGetCache(null, key2);
            throw new Exception("不允许还持有缓存");
        } catch (NoCacheException ignore) {
        }
        assert cacheStorage.size() == 1;
        Thread.sleep(250);
        assert cacheStorage.size() == 0;
    }

}
