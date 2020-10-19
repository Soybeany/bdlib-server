package com.soybeany.cache.v2.log;

import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.contract.ILogger;
import com.soybeany.cache.v2.model.DataPack;

/**
 * @author Soybeany
 * @date 2020/10/19
 */
public class ConsoleLogger<Param, Data> implements ILogger<Param, Data> {

    @Override
    public void onGetData(String desc, Param param, DataPack<Data> pack) {
        String from;
        switch (pack.from) {
            case CACHE:
                from = "缓存(" + ((ICacheStrategy<?, ?>) pack.provider).desc() + ")";
                break;
            case SOURCE:
                from = "数据源";
                break;
            default:
                from = "未知来源";
        }
        System.out.println("“" + desc + "”从“" + from + "”获取了数据");
    }

    @Override
    public void onCacheData(String desc, Param param, DataPack<Data> pack) {
        System.out.println("“" + desc + "”缓存了数据“");
    }

    @Override
    public void onCacheException(String desc, Param param, Exception e) {
        System.out.println("“" + desc + "”缓存了异常(" + e.getClass().getSimpleName() + ")“");
    }

    @Override
    public void onRemoveCache(String desc, Param param) {
        System.out.println("“" + desc + "”移除了数据“");
    }

    @Override
    public void onClearCache(String desc) {
        System.out.println("“" + desc + "”清空了数据“");
    }
}
