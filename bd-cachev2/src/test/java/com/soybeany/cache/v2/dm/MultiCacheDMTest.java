package com.soybeany.cache.v2.dm;

import com.soybeany.cache.v2.component.DBSimulationStrategy;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.core.DataManager;
import com.soybeany.cache.v2.log.ConsoleLogger;
import com.soybeany.cache.v2.model.DataPack;
import com.soybeany.cache.v2.strategy.LruMemCacheStrategy;
import com.soybeany.cache.v2.strategy.StdCacheStrategy;
import org.junit.Test;

import java.util.UUID;

/**
 * <br>Created by Soybeany on 2020/10/16.
 */
public class MultiCacheDMTest {

    private final IDatasource<String, String> datasource = new IDatasource<String, String>() {
        @Override
        public String onGetData(String s) {
            System.out.println(s + "(key)access datasource");
            return UUID.randomUUID().toString();
        }
    };

    private final StdCacheStrategy<String, String> lruStrategy = new LruMemCacheStrategy<String, String>();
    private final StdCacheStrategy<String, String> dbStrategy = new DBSimulationStrategy<String, String>();

    private final DataManager<String, String> dataManager = DataManager.Builder
            .get(datasource)
            .withCache(lruStrategy.expiry(500))
            .withCache(dbStrategy)
            .logger(new ConsoleLogger<String, String>())
            .build();

    @Test
    public void test() throws Exception {
        String key = "key";
        // 一开始没有数据，应该访问数据源
        DataPack<String> pack = dataManager.getDataPack("MultiCache1", key);
        assert datasource.equals(pack.provider);
        // 已经缓存了数据，应该访问lru
        pack = dataManager.getDataPack("MultiCache2", key);
        assert lruStrategy.equals(pack.provider);
        // 休眠一个比lru时间长的时间
        Thread.sleep(600);
        // lru缓存已失效，访问db
        pack = dataManager.getDataPack("MultiCache3", key);
        assert dbStrategy.equals(pack.provider);
        // 缓存重新建立
        pack = dataManager.getDataPack("MultiCache4", key);
        assert lruStrategy.equals(pack.provider);
    }

}
