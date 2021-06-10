package com.soybeany.cache.v2.log;

import com.soybeany.cache.v2.contract.ICacheStorage;
import com.soybeany.cache.v2.contract.IDatasource;
import com.soybeany.cache.v2.contract.ILogger;
import com.soybeany.cache.v2.model.DataContext;
import com.soybeany.cache.v2.model.DataPack;

import java.util.ArrayList;
import java.util.List;

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
        if (pack.provider instanceof ICacheStorage) {
            from = "缓存(" + ((ICacheStorage<?, ?>) pack.provider).desc() + ")";
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
    public void onRemoveCache(DataContext<Param> context, int... storageIndexes) {
        mWriter.onWriteInfo("“" + context.dataDesc + "”移除了" + getIndexMsg(storageIndexes) + "中“" + context.paramDesc + "”的缓存“");
    }

    @Override
    public void onRemoveOldCache(DataContext<Param> context, int removeLevel) {
        String content;
        if (removeLevel <= 0) {
            content = "没有移除任何旧缓存";
        } else {
            content = "移除了下标为“0~" + (removeLevel - 1) + "”的存储器中“" + context.paramDesc + "”的旧缓存";
        }
        mWriter.onWriteInfo("“" + context.dataDesc + "”" + content);
    }

    @Override
    public void onClearCache(String dataDesc, int... storageIndexes) {
        mWriter.onWriteInfo("“" + dataDesc + "”清空了" + getIndexMsg(storageIndexes) + "的缓存“");
    }

    private String getIndexMsg(int... storageIndexes) {
        if (null == storageIndexes || 0 == storageIndexes.length) {
            return "全部存储器";
        }
        List<Integer> list = new ArrayList<>();
        for (int index : storageIndexes) {
            list.add(index);
        }
        return "下标为" + list + "的存储器";
    }
}
