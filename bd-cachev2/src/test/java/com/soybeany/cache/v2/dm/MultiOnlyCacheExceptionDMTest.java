package com.soybeany.cache.v2.dm;

import com.soybeany.cache.v2.component.DBSimulationStrategy;
import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.core.DataManager;
import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.log.ConsoleLogger;
import com.soybeany.cache.v2.strategy.LruMemCacheStrategy;
import org.junit.Test;

/**
 * 只查询缓存的测试
 *
 * @author Soybeany
 * @date 2020/11/4
 */
public class MultiOnlyCacheExceptionDMTest {

    private final IDatasource<String, String> datasource = s -> {
        System.out.println("“" + s + "”access datasource");
        throw new Exception("假设数据源异常");
    };

    private final ICacheStrategy<String, String> lruStrategy = new LruMemCacheStrategy<String, String>().tempExpiry(100).fastFailExpiry(150);
    private final ICacheStrategy<String, String> dbStrategy = new DBSimulationStrategy<String, String>().fastFailExpiry(200);

    private final DataManager<String, String> dataManager = DataManager.Builder
            .get("只查缓存测试", datasource)
            .withCache(lruStrategy)
            .withCache(dbStrategy)
            .logger(new ConsoleLogger<>())
            .build();

    @Test
    public void test() throws Exception {
        String key = "testKey";
        // 从数据源获取数据
        try {
            dataManager.getDataPack("1", key);
            throw new Exception("不允许获取得到数据");
        } catch (DataException e) {
            assert datasource == e.provider;
        }
        // 从lru缓存中获取数据
        try {
            dataManager.getDataPack("1", key);
            throw new Exception("不允许获取得到数据");
        } catch (DataException e) {
            assert lruStrategy == e.provider;
        }
        // 从lru缓存中获取缓存
        try {
            dataManager.getCacheDataPack("1", key);
            throw new Exception("不允许获取得到数据");
        } catch (DataException e) {
            assert lruStrategy == e.provider;
        }
        Thread.sleep(150);
        // 从db缓存中获取缓存
        try {
            dataManager.getCacheDataPack("1", key);
            throw new Exception("不允许获取得到数据");
        } catch (DataException e) {
            assert dbStrategy == e.provider;
        }
        // 从lru缓存中获取缓存
        try {
            dataManager.getCacheDataPack("1", key);
            throw new Exception("不允许获取得到数据");
        } catch (DataException e) {
            assert lruStrategy == e.provider;
        }
        Thread.sleep(100);
        // 全部缓存失效
        try {
            dataManager.getCacheDataPack("1", key);
            throw new Exception("不允许获取得到数据");
        } catch (DataException e) {
            assert NoCacheException.class.equals(e.getOriginExceptionClass());
        }
    }

    @Test
    public void testRemove() throws Exception {
        String key = "testKey";
        // 从db缓存中获取缓存
        try {
            dataManager.getCacheDataPack("1", key);
            throw new Exception("不允许获取得到数据");
        } catch (DataException e) {
            assert dbStrategy == e.provider;
        }
        dataManager.removeCache("2", key);
        // 重新从db缓存中获取缓存
        try {
            dataManager.getCacheDataPack("2", key);
            throw new Exception("不允许获取得到数据");
        } catch (DataException e) {
            assert dbStrategy == e.provider;
        }
    }
}
