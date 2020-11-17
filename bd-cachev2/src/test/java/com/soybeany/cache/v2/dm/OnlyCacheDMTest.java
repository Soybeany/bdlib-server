package com.soybeany.cache.v2.dm;

import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.core.DataManager;
import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.exception.NoCacheException;
import com.soybeany.cache.v2.log.ConsoleLogger;
import com.soybeany.cache.v2.strategy.LruMemCacheStrategy;
import com.soybeany.cache.v2.strategy.StdCacheStrategy;
import org.junit.Test;

import java.util.UUID;

/**
 * 只查询缓存的测试
 *
 * @author Soybeany
 * @date 2020/11/4
 */
public class OnlyCacheDMTest {

    private final IDatasource<String, String> datasource = s -> {
        System.out.println("“" + s + "”access datasource");
        return UUID.randomUUID().toString();
    };

    private final StdCacheStrategy<String, String> lruStrategy = new LruMemCacheStrategy<>();

    private final DataManager<String, String> dataManager = DataManager.Builder
            .get("只查缓存测试", datasource)
            .withCache(lruStrategy.expiry(1000))
            .logger(new ConsoleLogger<>())
            .build();

    @Test
    public void test() throws Exception {
        String key = "testKey";
        try {
            dataManager.getCacheDataPack("1", key);
            throw new Exception("不允许获取得到数据");
        } catch (DataException e) {
            assert NoCacheException.class.equals(e.getOriginExceptionClass());
        }
        dataManager.getData("2", key);
        dataManager.getCache("3", key);
        Thread.sleep(1100);
        try {
            dataManager.getCacheDataPack("4", key);
            throw new Exception("不允许获取得到数据");
        } catch (DataException e) {
            assert NoCacheException.class.equals(e.getOriginExceptionClass());
        }
    }

}
