package com.soybeany.cache.v2.dm;

import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.core.DataManager;
import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.exception.NoDataSourceException;
import com.soybeany.cache.v2.log.ConsoleLogger;
import com.soybeany.cache.v2.model.DataFrom;
import com.soybeany.cache.v2.model.DataPack;
import com.soybeany.cache.v2.strategy.LruMemCacheStrategy;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * <br>Created by Soybeany on 2020/10/11.
 */
public class SimpleDMTest {

    private final IDatasource<String, String> datasource = new IDatasource<String, String>() {
        @Override
        public String onGetData(String s) {
            System.out.println(s + "(key)access datasource");
            return UUID.randomUUID().toString();
        }
    };

    private final ICacheStrategy<String, String> lruStrategy = new LruMemCacheStrategy<String, String>();

    private final DataManager<String, String> dataManager = DataManager.Builder
            .get("简单测试", datasource)
            .withCache(lruStrategy)
            .logger(new ConsoleLogger<String, String>())
            .build();

    @Test
    public void sequenceTest() throws Exception {
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
        final DataFrom[] froms = new DataFrom[count];
        final CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            final int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        DataPack<String> pack = dataManager.getDataPack("并发", null);
                        froms[finalI] = pack.from;
                    } catch (DataException e) {
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                }
            }).start();
        }
        latch.await();
        dataManager.getDataPack("单发", null);
        int accessCount = 0;
        for (DataFrom from : froms) {
            if (DataFrom.SOURCE == from) {
                accessCount++;
            }
        }
        assert accessCount == 1;
    }

    @Test
    public void specifyDatasourceTest() throws Exception {
        final String source = "新数据源";
        String data = dataManager.getData("特定数据源", null, new IDatasource<String, String>() {
            @Override
            public String onGetData(String s) {
                return source;
            }
        });
        assert source.equals(data);
    }

    @Test
    public void noDatasourceTest() throws Exception {
        try {
            dataManager.getData("无数据源", null, null);
            throw new Exception("不允许不抛出异常");
        } catch (DataException e) {
            Exception originException = e.getOriginException();
            assert originException instanceof NoDataSourceException;
        }
    }

}
