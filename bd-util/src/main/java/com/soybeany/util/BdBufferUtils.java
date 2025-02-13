package com.soybeany.util;

import com.soybeany.exception.BdRtException;

import java.io.IOException;

public abstract class BdBufferUtils {

    public static <T extends Buffer> long dataCopy(long dataLength, T buffer, DataSupplier supplier, DataConsumer<T> consumer) {
        try {
            int curRead, written = 0;
            long totalRead = 0, remaining;
            while ((remaining = dataLength - totalRead) > 0) {
                // 读取数据
                curRead = supplier.onHandle(buffer.buffer, buffer.offset + written, (int) Math.min(buffer.length - written, remaining));
                // 已到文件末尾，则提前结束
                if (curRead < 0) {
                    break;
                }
                // 累计数据
                totalRead += curRead;
                written += curRead;
                // 累积足够数据，则回调业务层
                if (buffer.length == written) {
                    consumer.onHandle(buffer);
                    written = 0;
                }
            }
            // 将缓存的数据也返回业务层
            if (written > 0) {
                buffer.length = written;
                consumer.onHandle(buffer);
            }
            return totalRead;
        } catch (IOException e) {
            throw new BdRtException(e.getMessage());
        }
    }

    public interface DataSupplier {
        int onHandle(byte[] buffer, int offset, int length) throws IOException;
    }

    public interface DataConsumer<T extends Buffer> {
        void onHandle(T buffer) throws IOException;
    }

    public static class Buffer {
        public byte[] buffer;
        public int offset;
        public int length;

        public Buffer(int offset, int length) {
            this.offset = offset;
            this.length = length;
            this.buffer = new byte[offset + length];
        }
    }
}
