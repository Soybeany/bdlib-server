package com.soybeany.cache.v2;

import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.core.DataManager;
import com.soybeany.cache.v2.exception.DataException;
import com.soybeany.cache.v2.model.DataFrom;
import com.soybeany.cache.v2.model.DataPack;
import com.soybeany.cache.v2.strategy.LruMemCacheStrategy;
import org.junit.jupiter.api.Test;

/**
 * <br>Created by Soybeany on 2020/10/16.
 */
public class ExceptionDMTest {

    private final DataManager<String, String> dataManager = DataManager.Builder
            .get(new IDatasource<String, String>() {
                @Override
                public String onGetData(String s) throws Exception {
                    throw new Exception("测试");
                }
            })
            .withCache(new LruMemCacheStrategy<String, String>())
            .build();

    @Test
    public void test() {
        DataPack<String> data;
        // 从数据源抛出异常
        try {
            data = dataManager.getDataPack(null);
            System.out.println("data:" + data);
        } catch (DataException e) {
            assert DataFrom.SOURCE == e.getDataFrom();
        }
        // 抛出的是缓存了的异常
        try {
            data = dataManager.getDataPack(null);
            System.out.println("data:" + data);
        } catch (DataException e) {
            assert DataFrom.CACHE == e.getDataFrom();
        }
    }

}
