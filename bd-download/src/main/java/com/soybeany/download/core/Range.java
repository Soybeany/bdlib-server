package com.soybeany.download.core;

public class Range {
    public final long start;
    public final long end;

    public static Range getDefault(long end) {
        return new Range(0, end);
    }

    public Range(long start, long end) {
        this.start = start;
        this.end = end;
    }
}