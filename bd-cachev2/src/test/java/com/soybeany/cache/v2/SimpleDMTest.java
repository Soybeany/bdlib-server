package com.soybeany.cache.v2;

import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.core.DataManager;
import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.model.DataPack;
import com.soybeany.cache.v2.strategy.LruMemCacheStrategy;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * <br>Created by Soybeany on 2020/10/11.
 */
public class SimpleDMTest {

    private final IDatasource<String, String> datasource = new IDatasource<String, String>() {
        @Override
        public String onGetData(String s) {
            System.out.println(s + "访问了数据源");
            return UUID.randomUUID().toString();
        }
    };

    private final ICacheStrategy<String, String> lruStrategy = new LruMemCacheStrategy<String, String>();

    private final DataManager<String, String> dataManager = DataManager.Builder
            .get(datasource)
            .withCache(lruStrategy)
            .build();

    @Test
    public void sequenceTest() throws Exception {
        String key = "key";
        // 第一次将访问数据源
        DataPack<String> data = dataManager.getDataPack(key);
        assert datasource.equals(data.producer);
        // 第二次将读取lru
        data = dataManager.getDataPack(key);
        assert lruStrategy.equals(data.producer);
    }

    @Test
    public void concurrentTest() throws Exception {
        for (int i = 0; i < 10; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String data = null;
                    try {
                        data = dataManager.getData(null);
                    } catch (DataException e) {
                        e.printStackTrace();
                    }
                    System.out.println("data:" + data);
                }
            }).start();
        }
        Thread.sleep(500);
    }

}
