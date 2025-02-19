package com.soybeany.download;

//public class DataToWeb implements IDataTo.WithRandomAccess<OutputStream> {

//    private final String contentDisposition;
//    private final long contentLength;
//
//    private String contentType = "application/octet-stream";
//    private String eTag;
//    private String age;
//
//    private String consumerETag;
//
//    private boolean completeDownload = true;
//    private DataRange range;
//
//    public static String toDisposition(String fileName) {
//        try {
//            return "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"";
//        } catch (UnsupportedEncodingException e) {
//            throw new BdRtException("使用了不支持的编码“utf-8”");
//        }
//    }
//
//    public static DataToWeb createByFileName(String fileName, long contentLength) {
//        return createByContentDisposition(toDisposition(fileName), contentLength);
//    }
//
//    public static DataToWeb createByContentDisposition(String contentDisposition, long contentLength) {
//        return new DataToWeb(contentDisposition, contentLength);
//    }
//
//    public static DataToWeb createByFile(File file) {
//        return createByFileName(file.getName(), file.length());
//    }
//
//    private DataToWeb(String contentDisposition, long contentLength) {
//        this.contentDisposition = contentDisposition;
//        this.contentLength = contentLength;
//    }
//
//    public DataToWeb enableRandomAccess(HttpServletRequest request, boolean needCheckIfRange) {
//
//    }
//
//    @Override
//    public Optional<DataRange> onGetRange() {
//        return Optional.ofNullable(range);
//    }
//
//    @Override
//    public OutputStream onGetOutput() throws IOException {
//        return null;
//    }
//
//
//    private Range getRange(HttpServletRequest request, boolean needCheckIfRange) {
//        // 读取请求中的数值
//        String rRange = request.getHeader(RANGE);
//        String rIfRange = request.getHeader(IF_RANGE);
//        long maxLength = part1.contentLength;
//        // 若不满足要求，返回全量范围
//        completeDownload = (!part2.supportRandomAccess)
//                || (null == rRange)
//                || (needCheckIfRange && (null == rIfRange || !rIfRange.equals(eTag)));
//        if (completeDownload) {
//            return Range.getDefault(maxLength);
//        }
//        try {
//            long end = maxLength;
//            String[] rangeArr = rRange.replaceAll(BYTES + "=", "").split("-");
//            long start = Long.parseLong(rangeArr[0]);
//            if (rangeArr.length > 1 && !rangeArr[1].isEmpty()) {
//                end = Long.parseLong(rangeArr[1]);
//            }
//            if (start < 0 || end > maxLength) {
//                throw new BdDownloadException("非法的续传范围:" + start + "~" + end);
//            }
//            return new Range(start, end);
//        } catch (NumberFormatException ignore) {
//            return Range.getDefault(maxLength);
//        }
//    }
//
//    public static class Part1 {
//
//    }

//}
