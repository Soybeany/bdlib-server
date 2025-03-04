package com.soybeany.download;

import com.soybeany.download.core.Md5Type;
import com.soybeany.exception.BdRtException;
import com.soybeany.util.BdBufferUtils;
import com.soybeany.util.HexUtils;
import com.soybeany.util.Md5Utils;
import com.soybeany.util.file.BdFileUtils;
import com.soybeany.util.transfer.BdDataTransferUtils;
import com.soybeany.util.transfer.core.DataRange;
import com.soybeany.util.transfer.core.IDataFrom;
import com.soybeany.util.transfer.from.DataFromFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.function.Function;

public abstract class DataSupplier {

    public static Part0 builder() {
        return new Part0();
    }

    public static String calMd5Old(File file) {
        return BdFileUtils.randomRead(file, 0, file.length(), (raf, len) -> calMd5Old(len, raf::read, b -> {
        }));
    }

    private static String calMd5Old(long dataLength, BdBufferUtils.DataSupplier supplier, BdBufferUtils.DataConsumer<BdBufferUtils.Buffer> consumer) {
        MessageDigest digest = Md5Utils.getDigest();
        BufferWithMd5 buffer = new BufferWithMd5();
        long l = BdBufferUtils.dataCopy(dataLength, buffer, supplier, b -> {
            // 更新md5
            // 将上一次的结果与新数据混合
            System.arraycopy(b.md5, 0, b.buffer, 0, BufferWithMd5.MD5_SIZE);
            // 开始计算混合后数据的md5
            digest.update(b.buffer, 0, BufferWithMd5.MD5_SIZE + b.length);
            b.md5 = digest.digest();
            // 输出
            consumer.onHandle(b);
        });
        return HexUtils.bytesToHex(buffer.md5);
    }

    // ***********************内部类****************************

    public static class Part0 {
        public Part1 fileName(String fileName) {
            return contentDisposition(DataToResponse.toDisposition(fileName));
        }

        public Part1 contentDisposition(String contentDisposition) {
            return new Part1(contentDisposition);
        }

        public Part3 file(File file, Md5Type md5Type) {
            return fileName(file.getName())
                    .contentLength(file.length())
                    .dataFrom(file, md5Type)
                    .eTag(String.valueOf(file.lastModified()));
        }
    }

    public static class Part1 {
        private final String contentDisposition;
        private long contentLength;

        Part1(String contentDisposition) {
            this.contentDisposition = contentDisposition;
        }

        public Part2 contentLength(long contentLength) {
            this.contentLength = contentLength;
            return new Part2(this);
        }
    }

    /**
     * 文件静态属性
     */
    public static class Part2 {
        private final Part1 part1;

        Part2(Part1 part1) {
            this.part1 = part1;
        }

        public Part3 dataFrom(File file) {
            return dataFrom(file, Md5Type.STD_PARTIAL);
        }

        public Part3 dataFrom(File file, Md5Type md5Type) {
            return dataFrom(new DataFromFile(file)).md5(range -> {
                String md5;
                switch (md5Type) {
                    case OLD:
                        md5 = calMd5Old(file);
                        break;
                    case STD:
                        md5 = BdFileUtils.md5(file);
                        break;
                    case STD_PARTIAL:
                        md5 = BdFileUtils.md5(file, range.start, range.end);
                        break;
                    default:
                        throw new BdRtException("使用了不支持的md5类型(" + md5Type + ")");
                }
                return md5;
            });
        }

        public Part3 dataFrom(IDataFrom<OutputStream> callback) {
            return new Part3(part1, callback);
        }

        public Part3 dataFrom(IDataFrom.WithRandomAccess<OutputStream> callback) {
            return new Part3(part1, callback);
        }
    }

    /**
     * 配置数据消费
     */
    public static class Part3 {
        private final IDataFrom<OutputStream> callback;

        private HttpServletResponse response;
        private final DataToResponse dataToResponse = new DataToResponse(() -> response);

        Part3(Part1 part1, IDataFrom<OutputStream> callback) {
            dataToResponse.contentDisposition(() -> part1.contentDisposition);
            dataToResponse.contentLength(() -> part1.contentLength);
            this.callback = callback;
        }

        public Part3 enableRandomAccess(HttpServletRequest request) {
            dataToResponse.enableRandomAccess(() -> request);
            return this;
        }

        public Part3 contentType(String contentType) {
            dataToResponse.contentType(() -> contentType);
            return this;
        }

        public Part3 eTag(String eTag) {
            dataToResponse.eTag(() -> eTag);
            return this;
        }

        public Part3 age(long age) {
            dataToResponse.age(() -> age);
            return this;
        }

        public Part3 md5(Function<DataRange, String> supplier) {
            dataToResponse.md5(supplier);
            return this;
        }

        public Part3 checkModified(String eTag) {
            dataToResponse.consumeETag(() -> eTag);
            return this;
        }

        public void start(HttpServletResponse response) {
            this.response = response;
            BdDataTransferUtils.transfer(callback, dataToResponse);
        }
    }

    private static class BufferWithMd5 extends BdBufferUtils.Buffer {
        private static final int MD5_SIZE = 16;
        private static final int DEFAULT_SECTION_LENGTH = 10 * 1024 * 1024 - MD5_SIZE;

        byte[] md5 = new byte[MD5_SIZE];

        public BufferWithMd5() {
            super(MD5_SIZE, DEFAULT_SECTION_LENGTH);
        }
    }
}
