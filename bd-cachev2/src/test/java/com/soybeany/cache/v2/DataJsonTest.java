package com.soybeany.cache.v2;

import com.google.gson.reflect.TypeToken;
import com.soybeany.cache.v2.model.DataJson;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

/**
 * <br>Created by Soybeany on 2020/10/18.
 */
public class DataJsonTest {

    @Test
    public void test() throws Exception {
        String data = "sdf";
        String errMsg = "测试异常";

        DataJson<String> holder = DataJson.fromData(data);
        DataJson<String> holder2 = DataJson.fromException(new RuntimeException(errMsg));

        String json1 = DataJson.toJson(holder);
        String json2 = DataJson.toJson(holder2);

        DataJson<String> holder4 = DataJson.fromJson(json1, String.class);
        DataJson<String> holder5 = DataJson.fromJson(json2, String.class);

        assert data.equals(holder4.getData()) && errMsg.equals(holder5.getException().getMessage());
    }

    @Test
    public void test2() throws Exception {
        List<Vo> list = new LinkedList<>();
        list.add(new Vo("字段"));

        DataJson<List<Vo>> holder = DataJson.fromData(list);

        String json = DataJson.toJson(holder);
        String json2 = DataJson.toJson(holder);

        DataJson<String> holder3 = DataJson.fromJson(json, List.class);
        DataJson<String> holder4 = DataJson.fromJson(json2, new TypeToken<List<Vo>>() {
        }.getType());
    }

    private static class Vo {
        public final String field;

        public Vo(String field) {
            this.field = field;
        }
    }

}
