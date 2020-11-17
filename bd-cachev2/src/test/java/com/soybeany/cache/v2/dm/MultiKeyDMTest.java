package com.soybeany.cache.v2.dm;

import com.soybeany.cache.v2.core.DataManager;
import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.log.ConsoleLogger;
import com.soybeany.cache.v2.model.DataPack;
import com.soybeany.cache.v2.strategy.LruMemCacheStrategy;
import com.soybeany.cache.v2.strategy.StdCacheStrategy;
import org.junit.Test;

import java.util.UUID;

/**
 * <br>Created by Soybeany on 2020/11/17.
 */
public class MultiKeyDMTest {

    private final StdCacheStrategy<String, String> lruStrategy = new TestStrategy<>();

    private final DataManager<String, String> dataManager = DataManager.Builder
            .get("MultiExpiry", s -> s)
            .withCache(lruStrategy.expiry(800))
            .logger(new ConsoleLogger<>())
            .build();

    @Test
    public void test() throws Exception {
        int count = 10;
        Thread[] threads = new Thread[count];
        for (int i = 0; i < count; i++) {
            threads[i] = new Thread(() -> {
                try {
                    String key = UUID.randomUUID().toString();
                    dataManager.getDataPack(key, key);
                } catch (DataException e) {
                    throw new RuntimeException(e);
                }
            });
            threads[i].start();
        }
        long start = System.currentTimeMillis();
        for (Thread thread : threads) {
            thread.join();
        }
        long delta = System.currentTimeMillis() - start;
        System.out.println("时差:" + delta);
        assert delta > 3000;
    }

    private static class TestStrategy<Param, Data> extends LruMemCacheStrategy<Param, Data> {
        @Override
        public void onCacheData(Param param, String key, DataPack<Data> data) {
            System.out.println("存数据:" + key);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            super.onCacheData(param, key, data);
        }
    }

}
