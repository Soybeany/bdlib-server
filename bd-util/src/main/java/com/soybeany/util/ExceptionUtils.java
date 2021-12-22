package com.soybeany.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author Soybeany
 * @date 2021/5/13
 */
public class ExceptionUtils {

    public static String getExceptionDetail(Throwable e) {
        try (StringWriter sw = new StringWriter();
             PrintWriter pw = new PrintWriter(sw)) {
            e.printStackTrace(pw);
            return sw.toString().trim();
        } catch (IOException ioE) {
            return e.getMessage() + "(详情提取失败，使用摘要)";
        }
    }

}
