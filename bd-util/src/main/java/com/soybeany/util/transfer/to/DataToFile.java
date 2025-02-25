package com.soybeany.util.transfer.to;

import com.soybeany.exception.BdIoException;
import com.soybeany.util.file.BdFileUtils;
import com.soybeany.util.transfer.core.DataRange;
import com.soybeany.util.transfer.core.IDataTo;

import java.io.*;
import java.util.Optional;

public class DataToFile implements IDataTo.WithRandomAccess<OutputStream> {
    private final ICallback callback;
    private TempFileCallback tempFileCallback;

    private File targetFile;
    private File tempFile;

    public DataToFile(ICallback callback) {
        this.callback = callback;
    }

    public DataToFile useTempFile(TempFileCallback tempFileCallback) {
        this.tempFileCallback = tempFileCallback;
        return this;
    }

    @Override
    public void onInit() {
        targetFile = callback.onGetTarget();
        if (null != tempFileCallback) {
            tempFile = tempFileCallback.onGetTempFile();
        }
    }

    @Override
    public OutputStream onGetOutput() throws IOException {
        File file = getFileToWrite();
        BdFileUtils.mkParentDirs(file);
        return new BufferedOutputStream(new FileOutputStream(file, callback.append()));
    }

    @Override
    public void onSuccess() {
        if (null == tempFileCallback || null == tempFile) {
            return;
        }
        tempFileCallback.onCheck();
        // 先删除旧的目标文件
        if (targetFile.exists() && !targetFile.delete()) {
            throw new BdIoException("目标文件删除失败: " + targetFile.getAbsolutePath());
        }
        // 将临时文件移至目标位置
        if (!tempFile.renameTo(targetFile)) {
            throw new BdIoException("文件重命名失败: " + tempFile.getAbsolutePath() + " -> " + targetFile.getAbsolutePath());
        }
    }

    @Override
    public void onFailure(Exception e) {
        String msg = IDataTo.toErrMsg(e);
        if (null != tempFile && tempFileCallback.deleteOnFailure() && !tempFile.delete()) {
            msg += " + 临时文件删除失败: " + tempFile.getAbsolutePath();
        }
        throw new BdIoException(msg);
    }

    @Override
    public Optional<DataRange> onGetRange() {
        if (!callback.append()) {
            return Optional.empty();
        }
        File file = getFileToWrite();
        if (!file.exists()) {
            return Optional.empty();
        }
        return Optional.of(file).map(f -> DataRange.from(f.length()));
    }

    private File getFileToWrite() {
        return Optional.ofNullable(tempFile).orElse(targetFile);
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
