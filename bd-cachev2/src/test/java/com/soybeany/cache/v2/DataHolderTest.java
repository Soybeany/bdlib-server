package com.soybeany.cache.v2;

import com.google.gson.reflect.TypeToken;
import com.soybeany.cache.v2.model.DataHolder;
import com.soybeany.cache.v2.model.DataPack;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

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

        DataHolder<String> holder4 = DataHolder.fromJson(json1, String.class);
        DataHolder<String> holder5 = DataHolder.fromJson(json2, String.class);

        assert data.equals(holder4.getData()) && errMsg.equals(holder5.getException().getMessage());
    }

    @Test
    public void test2() throws Exception {
        List<Vo> list = new LinkedList<Vo>();
        list.add(new Vo("字段"));

        DataHolder<List<Vo>> holder = DataHolder.get(DataPack.newSourceDataPack(null, list), 1234);

        String json = DataHolder.toJson(holder);
        String json2 = DataHolder.toJson(holder);

        DataHolder<String> holder3 = DataHolder.fromJson(json, List.class);
        DataHolder<String> holder4 = DataHolder.fromJson(json2, new TypeToken<List<Vo>>() {
        }.getType());
    }

    private static class Vo {
        public String field;

        public Vo(String field) {
            this.field = field;
        }
    }

}
