package com.soybeany.cache.v2;

import com.soybeany.cache.v2.component.DBSimulationStrategy;
import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.core.DataManager;
import com.soybeany.cache.v2.model.DataPack;
import com.soybeany.cache.v2.strategy.LruMemCacheStrategy;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * <br>Created by Soybeany on 2020/10/16.
 */
public class MultiExpiryDMTest {

    private final IDatasource<String, String> datasource = new IDatasource<String, String>() {
        @Override
        public String onGetData(String s) {
            System.out.println(s + "访问了数据源");
            return UUID.randomUUID().toString();
        }
    };

    private final ICacheStrategy<String, String> lruStrategy = new LruMemCacheStrategy<String, String>();
    private final ICacheStrategy<String, String> dbStrategy = new DBSimulationStrategy<String, String>();

    private final DataManager<String, String> dataManager = DataManager.Builder
            .get(datasource)
            .withCache(lruStrategy.expiry(800))
            .withCache(dbStrategy.expiry(1000))
            .build();

    @Test
    public void test() throws Exception {
        String key = "key";
        // 一开始没有数据，应该访问数据源
        DataPack<String> pack = dataManager.getDataPack(key);
        System.out.println(pack.producer);
        assert datasource.equals(pack.producer);
        // 已经缓存了数据，应该访问lru
        pack = dataManager.getDataPack(key);
        System.out.println(pack.producer);
        assert lruStrategy.equals(pack.producer);
        // 休眠一个比lru时间长，但比db时间短的时间
        Thread.sleep(900);
        // lru已失效，但db未失效
        pack = dataManager.getDataPack(key);
        System.out.println(pack.producer);
        assert dbStrategy.equals(pack.producer);
        // lru仍生效
        pack = dataManager.getDataPack(key);
        System.out.println(pack.producer);
        assert lruStrategy.equals(pack.producer);
        // 休眠一个短时间，使db也失效
        Thread.sleep(100);
        // 全部缓存已失效，再次访问数据源
        pack = dataManager.getDataPack(key);
        System.out.println(pack.producer);
        assert datasource.equals(pack.producer);
    }

}
