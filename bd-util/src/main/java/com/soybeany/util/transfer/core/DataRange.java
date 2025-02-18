package com.soybeany.util.transfer.core;

public class DataRange {
    public final long start;
    public final long end;

    public static DataRange from(long start) {
        return new DataRange(start, Long.MAX_VALUE);
    }

    public static DataRange to(long end) {
        return new DataRange(0, end);
    }

    public DataRange(long start, long end) {
        this.start = start;
        this.end = end;
    }
}
