package com.behase.relumin.config;

import lombok.Data;

@Data
public class AuthConfig {
    public static final String DEFAULT_ENABLED = "false";
    public static final String DEFAULT_ALLOW_ANONYMOUS = "false";
    private String enabled;
    private String allowAnonymous;
}
