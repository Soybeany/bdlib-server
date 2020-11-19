package com.soybeany.cache.v2.log;

import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.contract.ILogger;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataPack;

/**
 * @author Soybeany
 * @date 2020/10/19
 */
public class ConsoleLogger<Param, Data> implements ILogger<Param, Data> {

    @Override
    public void onGetData(DataContext<Param> context, DataPack<Data> pack) {
        String from;
        if (pack.provider instanceof ICacheStrategy) {
            from = "缓存(" + ((ICacheStrategy<?, ?>) pack.provider).desc() + ")";
        } else if (pack.provider instanceof IDatasource) {
            from = "数据源";
        } else {
            from = "其它来源(" + pack.provider + ")";
        }
        System.out.println("“" + context.dataDesc + "”从“" + from + "”获取了“" + context.paramDesc + "”的数据");
    }

    @Override
    public void onCacheData(DataContext<Param> context, DataPack<Data> pack) {
        System.out.println("“" + context.dataDesc + "”缓存了“" + context.paramDesc + "”的数据“");
    }

    @Override
    public void onCacheException(DataContext<Param> context, Exception e) {
        System.out.println("“" + context.dataDesc + "”缓存了“" + context.paramDesc + "”的异常(" + e.getClass().getSimpleName() + ")“");
    }

    @Override
    public void onRemoveCache(DataContext<Param> context) {
        System.out.println("“" + context.dataDesc + "”移除了“" + context.paramDesc + "”的数据“");
    }

    @Override
    public void onClearCache(String dataDesc) {
        System.out.println("“" + dataDesc + "”清空了数据“");
    }
}
