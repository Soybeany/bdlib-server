package com.soybeany.cache.v2.dm;

import com.soybeany.cache.v2.component.DBSimulationStorage;
import com.soybeany.cache.v2.contract.ICacheStorage;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.core.DataManager;
import com.soybeany.cache.v2.log.ConsoleLogger;
import com.soybeany.cache.v2.model.DataPack;
import com.soybeany.cache.v2.storage.LruMemCacheStorage;
import org.junit.Test;

/**
 * @author Soybeany
 * @date 2021/6/10
 */
public class OldCacheTest {

    private final IDatasource<String, String> datasource = s -> s;

    private final ICacheStorage<String, String> lruStorage = new LruMemCacheStorage<String, String>().expiry(500);
    private final ICacheStorage<String, String> dbStorage = new DBSimulationStorage<String, String>().expiry(1000);

    private final DataManager<String, String> dataManager = DataManager.Builder
            .get("OldCache", datasource)
            .withCache(lruStorage)
            .withCache(dbStorage)
            .logger(new ConsoleLogger<>())
            .build();

    @Test
    public void test() throws Exception {
        String key = "key";
        // 初始化数据
        dataManager.getDataPack(key);
        // 直接移除旧缓存，不会受影响
        dataManager.removeOldCache(key, 400);
        DataPack<String> dataPack2 = dataManager.getDataPack(key);
        assert dataPack2.provider == lruStorage;
        // 延时移除旧缓存，移除lru缓存
        Thread.sleep(100);
        dataManager.removeOldCache(key, 400);
        DataPack<String> dataPack3 = dataManager.getDataPack(key);
        assert dataPack3.provider == dbStorage;
        // 再次延时移除旧缓存，移除全部缓存
        Thread.sleep(100);
        dataManager.removeOldCache(key, 800);
        DataPack<String> dataPack4 = dataManager.getDataPack(key);
        assert dataPack4.provider == datasource;
    }

}
