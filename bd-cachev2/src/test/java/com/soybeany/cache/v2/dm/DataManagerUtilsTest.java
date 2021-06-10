package com.soybeany.cache.v2.dm;

import com.soybeany.cache.v2.contract.ICacheStorage;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.core.DataManager;
import com.soybeany.cache.v2.core.DataManagerUtils;
import com.soybeany.cache.v2.log.ConsoleLogger;
import com.soybeany.cache.v2.model.DataPack;
import com.soybeany.cache.v2.storage.LruMemCacheStorage;
import org.junit.Test;

/**
 * @author Soybeany
 * @date 2021/6/10
 */
public class DataManagerUtilsTest {

    private final int expiry = 2000;

    private final IDatasource<String, String> datasource = param -> param;
    private final ICacheStorage<String, String> cacheStorage = new LruMemCacheStorage<String, String>().expiry(expiry);

    private final DataManager<String, String> dataManager = DataManager.Builder
            .get("简单测试", datasource)
            .withCache(cacheStorage)
            .logger(new ConsoleLogger<>())
            .build();

    @Test
    public void getOrRefreshDataPackTest() throws Exception {
        String key = "key";
        DataPack<String> pack = getData(key);
        assert pack.provider == datasource;
        DataPack<String> pack2 = getData(key);
        assert pack2.provider == cacheStorage;
        Thread.sleep(1000);
        DataPack<String> pack3 = getData(key);
        assert pack3.provider == datasource;
    }

    private DataPack<String> getData(String key) {
        return DataManagerUtils.getOrRefreshDataPack(dataManager, key, 1000);
    }

}
