package com.soybeany.util.transfer.to;

import com.soybeany.exception.BdIoException;
import com.soybeany.util.file.BdFileUtils;
import com.soybeany.util.transfer.core.DataRange;
import com.soybeany.util.transfer.core.IDataTo;

import java.io.*;
import java.util.Optional;

public class DataToFile implements IDataTo.WithRandomAccess<OutputStream> {
    private final File target;
    private File tempFile;
    private boolean append;
    private Runnable onCheck;
    private boolean deleteOnFailure;

    public DataToFile(File target) {
        this.target = target;
    }

    public DataToFile useTempFile(File tempFile, boolean append, Runnable onCheck, boolean deleteOnFailure) {
        this.tempFile = tempFile;
        this.append = append;
        this.onCheck = onCheck;
        this.deleteOnFailure = deleteOnFailure;
        return this;
    }

    @Override
    public OutputStream onGetOutput() throws IOException {
        File file = null != tempFile ? tempFile : target;
        BdFileUtils.mkParentDirs(file);
        return new BufferedOutputStream(new FileOutputStream(file, append));
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
        if (!append) {
            return Optional.empty();
        }
        return Optional.ofNullable(tempFile).map(file -> DataRange.from(file.length()));
    }
}
