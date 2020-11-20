package com.soybeany.cache.v2.dm;

import com.soybeany.cache.v2.component.DBSimulationStrategy;
import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.core.DataManager;
import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.log.ConsoleLogger;
import com.soybeany.cache.v2.model.DataPack;
import com.soybeany.cache.v2.strategy.LruMemCacheStrategy;
import org.junit.Test;

import java.util.UUID;

/**
 * 只查询缓存的测试
 *
 * @author Soybeany
 * @date 2020/11/4
 */
public class MultiOnlyCacheDMTest {

    private final IDatasource<String, String> datasource = s -> {
        System.out.println("“" + s + "”access datasource");
        return UUID.randomUUID().toString();
    };

    private final ICacheStrategy<String, String> lruStrategy = new LruMemCacheStrategy<String, String>().tempExpiry(50).expiry(100);
    private final ICacheStrategy<String, String> dbStrategy = new DBSimulationStrategy<String, String>().expiry(200);

    private final DataManager<String, String> dataManager = DataManager.Builder
            .get("只查缓存测试", datasource)
            .withCache(lruStrategy)
            .withCache(dbStrategy)
            .logger(new ConsoleLogger<>())
            .build();

    @Test
    public void test() throws Exception {
        String key = "testKey";
        // 没有缓存
        try {
            dataManager.getCacheDataPack("1", key);
            throw new Exception("不允许获取得到数据");
        } catch (DataException e) {
            assert NoCacheException.class.equals(e.getOriginExceptionClass());
        }
        // 从数据源获取数据
        DataPack<String> dataPack1 = dataManager.getDataPack("2", key);
        assert datasource == dataPack1.provider;
        // 从lru缓存中获取缓存
        DataPack<String> dataPack2 = dataManager.getCacheDataPack("3", key);
        assert lruStrategy == dataPack2.provider;
        // 从lru缓存中获取数据
        DataPack<String> dataPack3 = dataManager.getDataPack("4", key);
        assert lruStrategy == dataPack3.provider;
        Thread.sleep(100);
        // 从db缓存中获取缓存
        DataPack<String> dataPack4 = dataManager.getCacheDataPack("5", key);
        assert dbStrategy == dataPack4.provider;
        // 从lru缓存中获取数据
        DataPack<String> dataPack5 = dataManager.getDataPack("6", key);
        assert lruStrategy == dataPack5.provider;
        Thread.sleep(100);
        // 没有缓存
        try {
            dataManager.getCacheDataPack("7", key);
            throw new Exception("不允许获取得到数据");
        } catch (DataException e) {
            assert NoCacheException.class.equals(e.getOriginExceptionClass());
        }
    }

}
