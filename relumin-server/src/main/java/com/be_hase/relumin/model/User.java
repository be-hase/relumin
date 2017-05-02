package com.be_hase.relumin.model;

import org.apache.commons.lang3.EnumUtils;

import com.be_hase.relumin.type.AuthProvider;
import com.be_hase.relumin.type.Role;

import jetbrains.exodus.entitystore.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
public class User {
    private final String id;
    private final String login;
    private final String displayName;
    private final AuthProvider authProvider;
    private final String password;
    private final Role role;

    public User(Entity entity) {
        id = entity.getId().toString();
        login = (String) entity.getProperty("login");
        displayName = (String) entity.getProperty("displayName");
        authProvider = EnumUtils.getEnum(AuthProvider.class, "authProvider");
        password = (String) entity.getProperty("password");
        role = EnumUtils.getEnum(Role.class, "role");
    }
}
