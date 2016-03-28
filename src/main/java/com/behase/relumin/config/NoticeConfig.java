package com.behase.relumin.config;

import lombok.Data;

@Data
public class NoticeConfig {
    private NoticeMailConfig mail = new NoticeMailConfig();

    @Data
    public static class NoticeMailConfig {
        public static final String DEFAULT_HOST = "";
        public static final String DEFAULT_PORT = "0";
        public static final String DEFAULT_USER = "";
        public static final String DEFAULT_PASSWORD = "";
        public static final String DEFAULT_CHARSET = "UTF-8";
        public static final String DEFAULT_FROM = "";

        private String host;
        private String port;
        private String user;
        private String password;
        private String charset;

        private String from;
    }
}
