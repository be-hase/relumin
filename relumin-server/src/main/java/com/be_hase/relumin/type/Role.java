package com.be_hase.relumin.type;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    VIEWER,
    RELUMIN_ADMIN;

    @Override
    public String getAuthority() {
        return name();
    }
}
