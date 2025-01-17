package com.soybeany.download.core;

/**
 * @author Soybeany
 * @date 2022/2/15
 */
public interface BdDownloadHeaders {

    // *********************** client -> server ****************************

    String RANGE = "Range";
    String IF_RANGE = "If-Range";

    // *********************** server -> client ****************************

    String CONTENT_DISPOSITION = "Content-Disposition";
    String CONTENT_TYPE = "Content-Type";
    String CONTENT_MD5 = "Content-MD5";
    String ACCEPT_RANGES = "Accept-Ranges";
    String CONTENT_RANGE = "Content-Range";
    String CONTENT_LENGTH = "Content-Length";
    String E_TAG = "ETag";

    // *********************** value ****************************

    String BYTES = "bytes";

}


