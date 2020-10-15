package com.soybeany.cache.v2;

import com.soybeany.cache.v2.core.DataManager;
import com.soybeany.cache.v2.core.IDatasource;
import com.soybeany.cache.v2.service.LruMemCacheService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * <br>Created by Soybeany on 2020/10/11.
 */
public class CacheTest {

    private final DataManager<String, String> dataManager = DataManager.Builder
            .get("测试", new IDatasource<String, String>() {
                @Override
                public String onGetData(String s) {
                    System.out.println("访问了数据源");
                    return UUID.randomUUID().toString();
                }
            })
            .withCache(new LruMemCacheService<String, String>())
            .build();

    @Test
    public void test() throws Exception {
        String data = dataManager.getData(null);
        System.out.println("data:" + data);
        data = dataManager.getData(null);
        System.out.println("data:" + data);
    }

}
