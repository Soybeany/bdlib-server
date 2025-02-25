package com.soybeany.util.transfer;

import com.soybeany.util.BdStreamUtils;
import com.soybeany.util.transfer.core.DataRange;
import com.soybeany.util.transfer.core.IDataFrom;
import com.soybeany.util.transfer.core.IDataTo;

import java.io.Closeable;
import java.util.Optional;

public class BdDataTransferUtils {

    public static <T> void transfer(IDataFrom<T> from, IDataTo<T> to) {
        try {
            onTransfer(from, to);
            to.onSuccess();
        } catch (Exception e) {
            to.onFailure(e);
        }
    }

    private static <T> void onTransfer(IDataFrom<T> from, IDataTo<T> to) throws Exception {
        // 通知初始化
        from.onInit();
        to.onInit();

        // 若from或to不支持随机读写，则使用普通传输
        if (!(to instanceof IDataTo.WithRandomAccess) || !(from instanceof IDataFrom.WithRandomAccess)) {
            autoClose(to, from::onTransfer);
            return;
        }

        // 启用随机读写
        IDataTo.WithRandomAccess<T> aTo = (IDataTo.WithRandomAccess<T>) to;
        Optional<DataRange> rangeOpt = aTo.onGetRange();
        // 若配置了范围，则使用范围传输
        if (rangeOpt.isPresent()) {
            autoClose(to, os -> ((IDataFrom.WithRandomAccess<T>) from).onTransfer(rangeOpt.get(), os));
        }
        // 否则使用普通传输
        else {
            autoClose(to, from::onTransfer);
        }
    }

    private static <T> void autoClose(IDataTo<T> to, ICallback<T> consumer) throws Exception {
        T out = to.onGetOutput();
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
