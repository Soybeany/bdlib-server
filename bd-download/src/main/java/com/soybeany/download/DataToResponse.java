package com.soybeany.download;

import com.soybeany.download.core.BdDownloadException;
import com.soybeany.exception.BdRtException;
import com.soybeany.util.transfer.core.DataRange;
import com.soybeany.util.transfer.core.IDataTo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.soybeany.download.core.BdDownloadHeaders.*;

public class DataToResponse implements IDataTo.WithRandomAccess<OutputStream> {

    private static final String RANGE = "range";

    private final HttpServletResponse response;
    private Supplier<String> contentDispositionSupplier;
    private Supplier<Long> contentLengthSupplier = () -> {
        throw new BdDownloadException("未指定contentLength");
    };
    private Supplier<String> contentTypeSupplier = () -> "application/octet-stream";
    private Supplier<String> eTagSupplier;
    private Supplier<String> ageSupplier;
    private Function<DataRange, String> md5Supplier;

    private Supplier<String> consumerETagSupplier;

    private Function<Map<String, Object>, Optional<DataRange>> rangeSupplier = context -> Optional.empty();

    public static String toDisposition(String fileName) {
        try {
            return "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"";
        } catch (UnsupportedEncodingException e) {
            throw new BdRtException("使用了不支持的编码“utf-8”");
        }
    }

    public DataToResponse(HttpServletResponse response) {
        this.response = response;
    }

    public DataToResponse fileName(Supplier<String> supplier) {
        return contentDisposition(() -> toDisposition(supplier.get()));
    }

    public DataToResponse contentDisposition(Supplier<String> supplier) {
        this.contentDispositionSupplier = supplier;
        return this;
    }

    public DataToResponse contentLength(Supplier<Long> supplier) {
        this.contentLengthSupplier = supplier;
        return this;
    }

    public DataToResponse contentType(Supplier<String> supplier) {
        this.contentTypeSupplier = supplier;
        return this;
    }

    public DataToResponse eTag(Supplier<String> supplier) {
        this.eTagSupplier = supplier;
        return this;
    }

    public DataToResponse age(Supplier<String> supplier) {
        this.ageSupplier = supplier;
        return this;
    }

    public DataToResponse md5(Function<DataRange, String> supplier) {
        this.md5Supplier = supplier;
        return this;
    }

    public DataToResponse consumeETag(Supplier<String> supplier) {
        this.consumerETagSupplier = supplier;
        return this;
    }

    public DataToResponse enableRandomAccess(Supplier<HttpServletRequest> supplier) {
        return enableRandomAccess(() -> supplier.get().getHeader(IF_RANGE), () -> toRange(supplier.get().getHeader(RANGE)));
    }

    public DataToResponse enableRandomAccess(Supplier<String> eTagSupplier, Supplier<DataRange> rangeSupplier) {
        this.rangeSupplier = context -> {
            DataRange range = rangeSupplier.get();
            if (null == range) {
                return Optional.empty();
            }
            if (range.start < 0 || range.end > contentLengthSupplier.get()) {
                throw new BdDownloadException("非法的续传范围:" + range.start + "~" + range.end);
            }
            String eTag = this.eTagSupplier.get();
            String pETag = eTagSupplier.get();
            if (null != eTag && null != pETag && (!eTag.equals(pETag))) {
                return Optional.empty();
            }
            context.put(RANGE, range);
            return Optional.of(range);
        };
        return this;
    }

    @Override
    public Optional<DataRange> onGetRange(Map<String, Object> context) {
        // 比对内容变更
        if (null != eTagSupplier && null != consumerETagSupplier
                && eTagSupplier.get().equals(consumerETagSupplier.get())
        ) {
            throw new DataNotModifiedException();
        }
        // 返回范围
        return rangeSupplier.apply(context);
    }

    @Override
    public OutputStream onGetOutput(Map<String, Object> context) throws IOException {
        // 设置响应头
        long contentLength = contentLengthSupplier.get();
        DataRange range = (DataRange) context.get(RANGE);
        boolean supportRandomAccess = true;
        if (null == range) {
            range = DataRange.to(contentLength);
            supportRandomAccess = false;
        }
        setupResponseHeader(supportRandomAccess, range, contentLength);
        // 返回流
        return response.getOutputStream();
    }

    @Override
    public void onFailure(Map<String, Object> context, Exception e) {
        // 已发送则不作处理
        if (response.isCommitted()) {
            return;
        }
        // 无修改则返回304
        if (e instanceof DataNotModifiedException) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        }
        // 重置响应并返回异常信息
        response.reset();
        response.setContentType("text/plain;charset=utf-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.print(IDataTo.toErrMsg(e));
        } catch (IOException ex) {
            throw new BdDownloadException("响应写入异常:" + ex.getMessage());
        }
    }

    private DataRange toRange(String range) {
        if (null == range) {
            return null;
        }
        long end = contentLengthSupplier.get();
        String[] rangeArr = range.replaceAll(BYTES + "=", "").split("-");
        long start = Long.parseLong(rangeArr[0]);
        if (rangeArr.length > 1 && !rangeArr[1].isEmpty()) {
            end = Long.parseLong(rangeArr[1]);
        }
        return new DataRange(start, end);
    }

    private void setupResponseHeader(boolean supportRandomAccess, DataRange range, long contentLength) {
        long rangeLength = range.end - range.start;
        // 常规响应头
        response.setContentType(contentTypeSupplier.get());
        response.setContentLengthLong(rangeLength);
        response.setHeader(CONTENT_DISPOSITION, contentDispositionSupplier.get());
        // 可选响应头
        Optional.ofNullable(eTagSupplier)
                .map(Supplier::get)
                .ifPresent(eTag -> response.setHeader(E_TAG, eTag));
        Optional.ofNullable(ageSupplier)
                .map(Supplier::get)
                .ifPresent(age -> response.setHeader(AGE, age));
        Optional.ofNullable(md5Supplier)
                .map(s -> s.apply(range))
                .ifPresent(md5 -> response.setHeader(CONTENT_MD5, md5));
        // 支持断点续传的标识
        if (supportRandomAccess) {
            response.setHeader(ACCEPT_RANGES, BYTES);
        }
        // 完整下载与部分下载差异化设置
        if (rangeLength == contentLength) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            String contentRange = BYTES + " " + range.start + "-" + range.end + "/" + contentLength;
            response.setHeader(CONTENT_RANGE, contentRange);
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        }
    }

    private static class DataNotModifiedException extends BdDownloadException {
        public DataNotModifiedException() {
            super("数据无变更");
        }
    }
}
