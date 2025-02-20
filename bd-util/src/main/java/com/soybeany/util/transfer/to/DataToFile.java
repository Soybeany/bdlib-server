package com.soybeany.util.transfer.to;

import com.soybeany.exception.BdIoException;
import com.soybeany.util.file.BdFileUtils;
import com.soybeany.util.transfer.core.DataRange;
import com.soybeany.util.transfer.core.IDataTo;

import java.io.*;
import java.util.Map;
import java.util.Optional;

public class DataToFile implements IDataTo.WithRandomAccess<OutputStream> {
    private static final String TARGET_FILE = "targetFile";
    private static final String TEMP_FILE = "tempFile";

    private final ICallback callback;
    private TempFileCallback tempFileCallback;

    public DataToFile(ICallback callback) {
        this.callback = callback;
    }

    public DataToFile useTempFile(TempFileCallback tempFileCallback) {
        this.tempFileCallback = tempFileCallback;
        return this;
    }

    @Override
    public OutputStream onGetOutput(Map<String, Object> context) throws IOException {
        File target = callback.onGetTarget();
        context.put(TARGET_FILE, target);

        if (null != tempFileCallback) {
            File temp = tempFileCallback.onGetTempFile();
            context.put(TEMP_FILE, temp);
        }

        File file = getFileToWrite(context);
        BdFileUtils.mkParentDirs(file);
        return new BufferedOutputStream(new FileOutputStream(file, callback.append()));
    }

    @Override
    public void onSuccess(Map<String, Object> context) {
        File tempFile = (File) context.get(TEMP_FILE);
        if (null == tempFileCallback || null == tempFile) {
            return;
        }
        tempFileCallback.onCheck();
        // 先删除旧的目标文件
        File target = (File) context.get(TARGET_FILE);
        if (target.exists() && !target.delete()) {
            throw new BdIoException("目标文件删除失败: " + target.getAbsolutePath());
        }
        // 将临时文件移至目标位置
        if (!tempFile.renameTo(target)) {
            throw new BdIoException("文件重命名失败: " + tempFile.getAbsolutePath() + " -> " + target.getAbsolutePath());
        }
    }

    @Override
    public void onFailure(Map<String, Object> context, Exception e) {
        String msg = IDataTo.toErrMsg(e);
        File tempFile = (File) context.get(TEMP_FILE);
        if (null != tempFile && tempFileCallback.deleteOnFailure() && !tempFile.delete()) {
            msg += " + 临时文件删除失败: " + tempFile.getAbsolutePath();
        }
        throw new BdIoException(msg);
    }

    @Override
    public Optional<DataRange> onGetRange(Map<String, Object> context) {
        if (!callback.append()) {
            return Optional.empty();
        }
        File file = getFileToWrite(context);
        if (!file.exists()) {
            return Optional.empty();
        }
        return Optional.of(file).map(f -> DataRange.from(f.length()));
    }

    private File getFileToWrite(Map<String, Object> context) {
        return (File) Optional.ofNullable(context.get(TEMP_FILE)).orElseGet(() -> context.get(TARGET_FILE));
    }

    // ***********************内部类****************************

    public interface ICallback {
        default boolean append() {
            return true;
        }

        File onGetTarget();
    }

    public interface TempFileCallback {

        default void onCheck() {
        }

        default boolean deleteOnFailure() {
            return true;
        }

        File onGetTempFile();
    }

}
