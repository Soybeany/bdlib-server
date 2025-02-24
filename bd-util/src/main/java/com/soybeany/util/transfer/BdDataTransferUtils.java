package com.soybeany.util.transfer;

import com.soybeany.util.BdStreamUtils;
import com.soybeany.util.transfer.core.DataRange;
import com.soybeany.util.transfer.core.IDataFrom;
import com.soybeany.util.transfer.core.IDataTo;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BdDataTransferUtils {

    public static <T> void transfer(IDataFrom<T> from, IDataTo<T> to) {
        Map<String, Object> context = new HashMap<>();
        try {
            onTransfer(context, from, to);
            to.onSuccess(context);
        } catch (Exception e) {
            to.onFailure(context, e);
        }
    }

    private static <T> void onTransfer(Map<String, Object> context, IDataFrom<T> from, IDataTo<T> to) throws Exception {
        // 通知源进行初始化
        from.onInit();

        // 若from或to不支持随机读写，则使用普通传输
        if (!(to instanceof IDataTo.WithRandomAccess) || !(from instanceof IDataFrom.WithRandomAccess)) {
            autoClose(context, to, from::onTransfer);
            return;
        }

        // 启用随机读写
        IDataTo.WithRandomAccess<T> aTo = (IDataTo.WithRandomAccess<T>) to;
        Optional<DataRange> rangeOpt = aTo.onGetRange(context);
        // 若配置了范围，则使用范围传输
        if (rangeOpt.isPresent()) {
            autoClose(context, to, os -> ((IDataFrom.WithRandomAccess<T>) from).onTransfer(rangeOpt.get(), os));
        }
        // 否则使用普通传输
        else {
            autoClose(context, to, from::onTransfer);
        }
    }

    private static <T> void autoClose(Map<String, Object> context, IDataTo<T> to, ICallback<T> consumer) throws Exception {
        T out = to.onGetOutput(context);
        try {
            consumer.onInvoke(out);
        } finally {
            if (out instanceof Closeable) {
                BdStreamUtils.closeStream((Closeable) out);
            }
        }
    }

    private interface ICallback<T> {
        void onInvoke(T out) throws Exception;
    }
}
