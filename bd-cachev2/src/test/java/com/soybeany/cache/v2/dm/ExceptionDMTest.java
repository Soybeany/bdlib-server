package com.soybeany.cache.v2.dm;

import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.core.DataManager;
import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.log.ConsoleLogger;
import com.soybeany.cache.v2.model.DataPack;
import com.soybeany.cache.v2.strategy.LruMemCacheStrategy;
import org.junit.Test;

/**
 * <br>Created by Soybeany on 2020/10/16.
 */
public class ExceptionDMTest {

    IDatasource<String, String> datasource = s -> {
        throw new Exception("测试");
    };

    ICacheStrategy<String, String> cacheStrategy = new LruMemCacheStrategy<>();

    private final DataManager<String, String> dataManager = DataManager.Builder
            .get("异常测试", datasource)
            .withCache(cacheStrategy)
            .logger(new ConsoleLogger<>())
            .build();

    @Test
    public void test() {
        DataPack<String> data;
        // 从数据源抛出异常
        try {
            data = dataManager.getDataPack("数据源", null);
            System.out.println("data:" + data);
        } catch (DataException e) {
            assert datasource == e.provider;
        }
        // 抛出的是缓存了的异常
        try {
            data = dataManager.getDataPack("缓存", null);
            System.out.println("data:" + data);
        } catch (DataException e) {
            assert cacheStrategy == e.provider;
        }
    }

}
