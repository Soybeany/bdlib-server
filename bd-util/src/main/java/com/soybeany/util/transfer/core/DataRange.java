package com.soybeany.util.transfer.core;

public class DataRange {
    public final Long start;
    public final Long end;

    public static DataRange from(long start) {
        return new DataRange(start, null);
    }

    public static DataRange to(long end) {
        return new DataRange(null, end);
    }

    public DataRange(Long start, Long end) {
        this.start = start;
        this.end = end;
    }
}
