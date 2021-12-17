package com.soybeany.cipher;

import com.fasterxml.jackson.databind.util.StdConverter;

class Converter {

    private static final String INT = "int";
    private static final String LONG = "lng";
    private static final String STRING = "str";

    // ********************内部类********************

    static class Encrypt extends StdConverter<Object, String> {
        @Override
        public String convert(Object value) {
            String type;
            if (value instanceof String) {
                type = STRING;
            } else if (value instanceof Long) {
                type = LONG;
            } else if (value instanceof Integer) {
                type = INT;
            } else {
                throw new RuntimeException("@Encrypt/@ContentEncrypt序列化时使用了暂不支持的类型" + "“" + value.getClass() + "”");
            }
            return CipherHandler.encrypt("(" + type + ")" + value);
        }
    }

    static class Decrypt extends StdConverter<String, Object> {
        @Override
        public Object convert(String value) {
            String decrypted = CipherHandler.decrypt(value);
            if (decrypted.length() < 5) {
                throw new RuntimeException("@Encrypt/@ContentEncrypt反序列化时使用了不正确的格式" + "“" + decrypted + "”");
            }
            String type = decrypted.substring(1, 4);
            String oValue = decrypted.substring(5);
            switch (type) {
                case STRING:
                    return oValue;
                case LONG:
                    return Long.parseLong(oValue);
                case INT:
                    return Integer.parseInt(oValue);
                default:
                    throw new RuntimeException("@Encrypt/@ContentEncrypt反序列化时使用了暂不支持的类型" + "“" + type + "”");
            }
        }
    }

}
