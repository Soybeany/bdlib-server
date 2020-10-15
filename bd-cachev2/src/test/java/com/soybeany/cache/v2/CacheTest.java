package com.soybeany.cache.v2;

import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.core.DataManager;
import com.soybeany.cache.v2.model.DataPack;
import com.soybeany.cache.v2.strategy.LruMemCacheStrategy;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * <br>Created by Soybeany on 2020/10/11.
 */
public class CacheTest {

    private final DataManager<String, String> dataManager = DataManager.Builder
            .get(new IDatasource<String, String>() {
                @Override
                public String onGetData(String s) {
                    System.out.println("访问了数据源");
                    return UUID.randomUUID().toString();
                }
            })
            .withCache(new LruMemCacheStrategy<String, String>())
            .build();

    @Test
    public void test() throws Exception {
        DataPack<String> data = dataManager.getDataPack(null);
        System.out.println("data:" + data);
        data = dataManager.getDataPack(null);
        System.out.println("data:" + data);
    }

}
