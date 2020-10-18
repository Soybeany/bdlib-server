package com.soybeany.cache.v2;

import com.soybeany.cache.v2.model.DataHolder;
import com.soybeany.cache.v2.model.DataPack;
import org.junit.Test;

/**
 * <br>Created by Soybeany on 2020/10/18.
 */
public class DataHolderTest {

    @Test
    public void test() throws Exception {
        String data = "sdf";
        String errMsg = "测试异常";
        DataHolder<String> holder = DataHolder.get(DataPack.newSourceDataPack(null, data), 1234);
        DataHolder<String> holder2 = DataHolder.get(new RuntimeException(errMsg), 1234);

        String json1 = DataHolder.toJson(holder);
        String json2 = DataHolder.toJson(holder2);

        DataHolder<String> holder3 = DataHolder.fromJson(json1);
        DataHolder<String> holder4 = DataHolder.fromJson(json2);

        assert data.equals(holder3.getData()) && errMsg.equals(holder4.getException().getMessage());
    }

}
