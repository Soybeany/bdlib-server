package com.soybeany.util.transfer.to;

import com.soybeany.exception.BdIoException;
import com.soybeany.util.file.BdFileUtils;
import com.soybeany.util.transfer.core.DataRange;
import com.soybeany.util.transfer.core.IDataTo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Optional;

public class DataToFileInStream implements IDataTo.WithRandomAccess<OutputStream> {
    private final File target;
    private File tempFile;
    private Runnable onCheck;
    private boolean deleteOnFailure;

    public DataToFileInStream(File target) {
        this.target = target;
    }

    public DataToFileInStream useTempFile(File tempFile, Runnable onCheck, boolean deleteOnFailure) {
        this.tempFile = tempFile;
        this.onCheck = onCheck;
        this.deleteOnFailure = deleteOnFailure;
        return this;
    }

    @Override
    public OutputStream onGetOutput() throws IOException {
        File file = null != tempFile ? tempFile : target;
        BdFileUtils.mkParentDirs(file);
        return new BufferedOutputStream(Files.newOutputStream(file.toPath()));
    }

    @Override
    public void onSuccess() {
        if (null == tempFile) {
            return;
        }
        onCheck.run();
        // 先删除旧的目标文件
        if (target.exists() && !target.delete()) {
            throw new BdIoException("目标文件删除失败: " + target.getAbsolutePath());
        }
        // 将临时文件移至目标位置
        if (!tempFile.renameTo(target)) {
            throw new BdIoException("文件重命名失败: " + tempFile.getAbsolutePath() + " -> " + target.getAbsolutePath());
        }
        // todo 放在第一个前置，由datasource设置新数据检测逻辑，以及检查间隔；到点有更新，invalid旧缓存再执行原后续逻辑
    }

    @Override
    public void onFailure(Exception e) {
        String msg = "[" + e.getClass().getName() + "]" + e.getMessage();
        if (deleteOnFailure && !tempFile.delete()) {
            msg += " + 临时文件删除失败: " + tempFile.getAbsolutePath();
        }
        throw new BdIoException(msg);
    }

    @Override
    public Optional<DataRange> onGetRange() {
        if (null == tempFile) {
            return Optional.empty();
        }
        return Optional.of(DataRange.from(tempFile.length()));
    }
}
