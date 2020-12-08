package com.soybeany.cache.v2.log;

import com.soybeany.cache.v2.contract.ICacheStrategy;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.contract.ILogger;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataPack;

/**
 * @author Soybeany
 * @date 2020/12/8
 */
public class StdLogger<Param, Data> implements ILogger<Param, Data> {

    private final ILogWriter mWriter;

    public StdLogger(ILogWriter writer) {
        mWriter = writer;
    }

    @Override
    public void onGetData(DataContext<Param> context, DataPack<Data> pack) {
        // 非正常获取，不输出日志
        if (!pack.norm()) {
            return;
        }
        String from;
        if (pack.provider instanceof ICacheStrategy) {
            from = "缓存(" + ((ICacheStrategy<?, ?>) pack.provider).desc() + ")";
        } else if (pack.provider instanceof IDatasource) {
            from = "数据源";
        } else {
            from = "其它来源(" + pack.provider + ")";
        }
        mWriter.onWriteInfo("“" + context.dataDesc + "”从“" + from + "”获取了“" + context.paramDesc + "”的数据");
    }

    @Override
    public void onCacheData(DataContext<Param> context, DataPack<Data> pack) {
        if (pack.norm()) {
            mWriter.onWriteInfo("“" + context.dataDesc + "”缓存了“" + context.paramDesc + "”的数据“");
        } else {
            mWriter.onWriteWarn("“" + context.dataDesc + "”缓存了“" + context.paramDesc + "”的异常(" + pack.dataCore.exception.getClass().getSimpleName() + ")“");
        }
    }

    @Override
    public void onRemoveCache(DataContext<Param> context) {
        mWriter.onWriteInfo("“" + context.dataDesc + "”移除了“" + context.paramDesc + "”的数据“");
    }

    @Override
    public void onClearCache(String dataDesc) {
        mWriter.onWriteInfo("“" + dataDesc + "”清空了数据“");
    }

}
