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
    public void onGetData(String dataDesc, String paramDesc, Param param, DataPack<Data> pack) {
        String from;
        switch (pack.from) {
            case CACHE:
                from = "缓存(" + ((ICacheStrategy<?, ?>) pack.provider).desc() + ")";
                break;
            case TEMP_CACHE:
                from = "临时缓存";
                break;
            case SOURCE:
                from = "数据源";
                break;
            default:
                from = "未知来源";
        }
        System.out.println("“" + dataDesc + "”从“" + from + "”获取了“" + paramDesc + "”的数据");
    }

    @Override
    public void onCacheData(String dataDesc, String paramDesc, Param param, DataPack<Data> pack) {
        System.out.println("“" + dataDesc + "”缓存了“" + paramDesc + "”的数据“");
    }

    @Override
    public void onCacheException(String dataDesc, String paramDesc, Param param, Exception e) {
        System.out.println("“" + dataDesc + "”缓存了“" + paramDesc + "”的异常(" + e.getClass().getSimpleName() + ")“");
    }

    @Override
    public void onRemoveCache(String dataDesc, String paramDesc, Param param) {
        System.out.println("“" + dataDesc + "”移除了“" + paramDesc + "”的数据“");
    }

    @Override
    public void onClearCache(String dataDesc) {
        System.out.println("“" + dataDesc + "”清空了数据“");
    }
}
