package com.be_hase.relumin.model;

import org.apache.commons.lang3.EnumUtils;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.be_hase.relumin.type.AuthProvider;
import com.be_hase.relumin.type.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;

import jetbrains.exodus.entitystore.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    public static final String ENTITY_TYPE = "user";

    private String id;
    private String userId;
    private String displayName;
    private AuthProvider authProvider;
    private Password password; // enc
    private Role role;

    public User(final Entity entity) {
        id = entity.getId().toString();
        userId = (String) entity.getProperty("userId");
        displayName = (String) entity.getProperty("displayName");
        authProvider = EnumUtils.getEnum(AuthProvider.class, (String) entity.getProperty("authProvider"));
        password = new Password((String) entity.getProperty("password"));
        role = EnumUtils.getEnum(Role.class, (String) entity.getProperty("role"));
    }

    public User(
            final String userId,
            final String displayName,
            final String rawPassword,
            final AuthProvider authProvider,
            final Role role
    ) {
        id = null;
        this.userId = userId;
        this.displayName = displayName;
        this.authProvider = authProvider;
        password = Password.of(rawPassword);
        this.role = role;
    }

    @JsonIgnore
    public org.springframework.security.core.userdetails.User getSpringUser() {
        return new org.springframework.security.core.userdetails.User(
                userId, password.getValue(), Lists.newArrayList(role));
    }

    @Value
    public static class Password {
        private final String value;

        public static Password of(String rawPassword) {
            return new Password(new StandardPasswordEncoder().encode(rawPassword));
        }
    }
}
