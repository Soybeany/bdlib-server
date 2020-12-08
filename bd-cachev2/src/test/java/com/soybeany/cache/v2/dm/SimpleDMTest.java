package com.soybeany.cache.v2.dm;

import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.core.DataManager;
import com.soybeany.cache.v2.exception.NoDataSourceException;
import com.soybeany.cache.v2.log.ConsoleLogger;
import com.soybeany.cache.v2.model.DataPack;
import com.soybeany.cache.v2.strategy.LruMemCacheStrategy;
import org.junit.Test;

import java.util.UUID;

/**
 * <br>Created by Soybeany on 2020/10/11.
 */
public class SimpleDMTest {

    private final IDatasource<String, String> datasource = s -> {
        System.out.println("“" + s + "”access datasource");
        return UUID.randomUUID().toString();
    };

    private final ICacheStrategy<String, String> lruStrategy = new LruMemCacheStrategy<String, String>().expiry(200);

    private final DataManager<String, String> dataManager = DataManager.Builder
            .get("简单测试", datasource)
            .withCache(lruStrategy)
            .logger(new ConsoleLogger<>())
            .build();

    @Test
    public void sequenceTest() {
        String key = "key";
        // 第一次将访问数据源
        DataPack<String> data = dataManager.getDataPack("序列1", key);
        assert datasource.equals(data.provider);
        // 第二次将读取lru
        data = dataManager.getDataPack("序列2", key);
        assert lruStrategy.equals(data.provider);
    }

    @Test
    public void concurrentTest() throws Exception {
        int count = 10;
        final Object[] providers = new Object[count];
        Thread[] threads = new Thread[count];
        for (int i = 0; i < count; i++) {
            final int finalI = i;
            threads[i] = new Thread(() -> {
                DataPack<String> pack = dataManager.getDataPack("并发", null);
                providers[finalI] = pack.provider;
            });
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        // 并发限制
        int accessCount = 0;
        for (Object provider : providers) {
            if (datasource == provider) {
                accessCount++;
            }
        }
        // 单发限制
        System.out.println("accessCount:" + accessCount);
        assert accessCount == 1;
        DataPack<String> pack1 = dataManager.getDataPack("单发LRU", null);
        assert lruStrategy == pack1.provider;
        Thread.sleep(200);
        DataPack<String> pack2 = dataManager.getDataPack("单发源", null);
        assert datasource == pack2.provider;
    }

    @Test
    public void specifyDatasourceTest() throws Exception {
        final String source = "新数据源";
        String data = dataManager.getData("特定数据源", null, s -> source);
        assert source.equals(data);
    }

    @Test
    public void noDatasourceTest() {
        try {
            dataManager.getData("无数据源", null, null);
            throw new Exception("不允许不抛出异常");
        } catch (Exception e) {
            assert e instanceof NoDataSourceException;
        }
    }

    @Test
    public void removeKeyTest() {
        String key1 = "key1";
        String key2 = "key2";
        // 第一次将访问1数据源
        DataPack<String> data = dataManager.getDataPack(key1, key1);
        assert datasource.equals(data.provider);
        // 第一次将访问2数据源
        DataPack<String> data2 = dataManager.getDataPack(key2, key2);
        assert datasource.equals(data2.provider);
        // 移除1的缓存，1将重新从数据源加载
        dataManager.removeCache(key1, key1, 0);
        data = dataManager.getDataPack(key1, key1);
        assert datasource.equals(data.provider);
        // 2不受影响
        data2 = dataManager.getDataPack(key2, key2);
        assert lruStrategy.equals(data2.provider);
    }

}
