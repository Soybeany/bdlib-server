package com.soybeany.util;

import com.soybeany.exception.BdRtException;

import java.io.Closeable;
import java.io.IOException;

public abstract class BdBufferUtils {

    public static <T extends Buffer> long dataCopy(long dataLength, T buffer, DataSupplier supplier, DataConsumer<T> consumer) {
        try {
            long remaining;
            Calculator<T> calculator = new Calculator<>(buffer, consumer);
            while ((remaining = dataLength - calculator.getTotalRead()) > 0) {
                int written = calculator.getWritten();
                int toCopy = supplier.onHandle(buffer.buffer, buffer.offset + written, (int) Math.min(buffer.length - written, remaining));
                calculator.flush(toCopy);
            }
            // 将缓存的数据也返回业务层
            calculator.close();
            return calculator.getTotalRead();
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

    public static class Calculator<T extends Buffer> implements Closeable {

        private final T buffer;
        private final DataConsumer<T> consumer;

        private long totalRead;
        private int written;

        public Calculator(T buffer, DataConsumer<T> consumer) {
            this.buffer = buffer;
            this.consumer = consumer;
        }

        public long getTotalRead() {
            return totalRead;
        }

        public int getWritten() {
            return written;
        }

        public void update(byte[] input, int offset, int len) throws IOException {
            // 若已无新数据，则结束
            if (len < 0) {
                return;
            }
            // 精确统计数据缓冲
            int copied = 0;
            while (copied < len) {
                int toCopy = Math.min(buffer.length - written, len - copied);
                System.arraycopy(input, offset + copied, buffer.buffer, buffer.offset + written, toCopy);
                copied += toCopy;
                flush(toCopy);
            }
        }

        public void flush(int toCopy) throws IOException {
            // 计数累加
            written += toCopy;
            totalRead += toCopy;
            // 到点触发回调
            if (buffer.length == written) {
                callConsumer();
            }
        }

        @Override
        public void close() throws IOException {
            // 剩余数据写入回调
            if (written > 0) {
                callConsumer();
            }
        }

        private void callConsumer() throws IOException {
            buffer.length = written;
            consumer.onHandle(buffer);
            written = 0;
        }
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
