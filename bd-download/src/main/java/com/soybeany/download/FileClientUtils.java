package com.soybeany.download;

import com.soybeany.download.core.BdDownloadException;
import com.soybeany.download.core.DownloadConfig;
import com.soybeany.download.core.FileInfo;
import com.soybeany.download.core.TempFileInfo;
import com.soybeany.util.file.BdFileUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.soybeany.download.core.BdDownloadHeaders.*;

/**
 * @author Soybeany
 * @date 2022/1/27
 */
@Slf4j
public class FileClientUtils {

    private static final Pattern RANGE_PATTERN = Pattern.compile(BYTES + " (\\d+)-(\\d*)/(\\d+)");

    public static void downloadFile(DownloadConfig downloadConfig, TempFileInfo tempFileInfo, ICallback callback) throws IOException {
        // 入参校验
        Optional.ofNullable(downloadConfig).orElseThrow(() -> new BdDownloadException("downloadConfig不能为null"));
        Optional.ofNullable(tempFileInfo).orElseThrow(() -> new BdDownloadException("tempFileInfo不能为null"));
        Optional.ofNullable(callback).orElseThrow(() -> new BdDownloadException("callback不能为null"));
        // 网络请求
        OkHttpClient client = getClient(downloadConfig.timeout());
        Request request = getRequest(downloadConfig, tempFileInfo);
        try (Response response = client.newCall(request).execute()) {
            FileInfo info = getFileInfo(response);
            try {
                handleResponse(tempFileInfo, response);
                callback.onSuccess(response, info, tempFileInfo.getTempFile());
            } catch (IOException e) {
                callback.onFailure(response, info, e);
            } finally {
                callback.onFinal(response, info);
            }
        }
    }

    // ***********************内部方法****************************

    private static OkHttpClient getClient(int timeout) {
        return new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();
    }

    private static Request getRequest(DownloadConfig config, TempFileInfo tempFileInfo) {
        Request.Builder builder = new Request.Builder()
                .url(config.url());
        // 配置断点续传header
        if (null != tempFileInfo.getETag() && tempFileInfo.getTempFile().exists()) {
            builder.header(IF_RANGE, tempFileInfo.getETag());
            builder.header(RANGE, BYTES + "=" + tempFileInfo.getTempFile().length() + "-");
        }
        // 配置自定义header
        config.headers().forEach(builder::header);
        return builder.build();
    }

    private static FileInfo getFileInfo(Response response) {
        return new FileInfo(
                response.header(CONTENT_DISPOSITION),
                Long.parseLong(Optional.ofNullable(response.header(CONTENT_LENGTH)).orElse("-1")),
                response.header(E_TAG)
        );
    }

    private static void handleResponse(TempFileInfo tempFileInfo, Response response) throws IOException {
        // 下载前校验与准备
        Result checkResult = canRandomAccess(response, tempFileInfo);
        File tempFile = tempFileInfo.getTempFile();
        if (checkResult.notSupport) {
            recreateFile(tempFile);
        }
        // 下载数据到临时文件
        InputStream is = Optional.ofNullable(response.body())
                .orElseThrow(() -> new BdDownloadException("body为null")).byteStream();
        try (FileOutputStream os = new FileOutputStream(tempFile, true)) {
            BdFileUtils.readWriteStream(is, os);
        }
    }

    private static Result canRandomAccess(Response response, TempFileInfo info) throws BdDownloadException {
        // 状态码检查
        if (response.code() != 206) {
            return new Result(true, "不是断点续传的响应");
        }
        // 临时文件检查
        File tempFile = info.getTempFile();
        if (!tempFile.exists()) {
            throw new BdDownloadException("临时文件不存在");
        }
        // eTag检查
        if (!Objects.equals(info.getETag(), response.header(E_TAG))) {
            throw new BdDownloadException("eTag不相等");
        }
        // 内容范围检查-响应头
        String acceptRanges = response.header(ACCEPT_RANGES);
        String contentRange = response.header(CONTENT_RANGE);
        if (!BYTES.equals(acceptRanges) || null == contentRange) {
            throw new BdDownloadException("accept-ranges不支持，或缺失content-range");
        }
        // 内容范围检查-范围格式
        Matcher matcher = RANGE_PATTERN.matcher(contentRange);
        if (!matcher.find()) {
            throw new BdDownloadException("无法解析范围格式");
        }
        // 内容范围检查-范围值
        long rangeStart = Long.parseLong(matcher.group(1));
        if (rangeStart != tempFile.length()) {
            throw new BdDownloadException("范围值不对应");
        }
        // 支持断点续传
        return new Result(false, "支持断点续传");
    }

    private static void recreateFile(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        boolean deleted = file.delete();
        if (!deleted) {
            throw new BdDownloadException("无法删除失效的断点续传临时文件“" + file.getName() + "”");
        }
        BdFileUtils.mkParentDirs(file);
    }

    // ***********************内部类****************************

    public interface ICallback {
        void onSuccess(Response response, FileInfo info, File tempFile);

        void onFailure(Response response, FileInfo info, IOException e);

        default void onFinal(Response response, FileInfo info) {
        }
    }

    @Data
    private static class Result {
        private final boolean notSupport;
        private final String msg;
    }

}
