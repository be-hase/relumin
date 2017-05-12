package org.relumin.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.relumin.model.User;
import org.relumin.type.AuthProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import jetbrains.exodus.entitystore.Entity;
import jetbrains.exodus.entitystore.EntityIterable;
import jetbrains.exodus.entitystore.PersistentEntityStore;
import jetbrains.exodus.entitystore.StoreTransaction;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {
    private final PersistentEntityStore entityStore;

    @Override
    public UserDetails loadUserByUsername(String username) {
        final User user = get(username).orElseThrow(() -> new UsernameNotFoundException("Not found."));
        return user.getSpringUser();
    }

    public Optional<User> get(final String userId) {
        return entityStore.computeInReadonlyTransaction(
                txn -> getUserEntity(txn, userId).map(User::new));
    }

    public List<User> getAll() {
        return entityStore.computeInReadonlyTransaction(
                txn -> Lists.newArrayList(txn.getAll(User.ENTITY_TYPE))
                            .stream()
                            .map(User::new)
                            .sorted(Comparator.comparing(User::getId))
                            .collect(Collectors.toList())
        );
    }

    public User add(final User user) {
        return entityStore.computeInTransaction(txn -> {
            final Entity entity = txn.newEntity(User.ENTITY_TYPE);
            entity.setProperty("userId", user.getUserId());
            entity.setProperty("displayName", user.getDisplayName());
            entity.setProperty("authProvider", user.getAuthProvider().name());
            entity.setProperty("password", user.getPassword().getValue());
            entity.setProperty("role", user.getRole().name());

            final EntityIterable candidates = txn.find(User.ENTITY_TYPE, "userId", user.getUserId());
            if (candidates.size() > 1) {
                throw new IllegalArgumentException("Same userId exists.");
            }

            return new User(entity);
        });
    }

    public User update(final User user) {
        return entityStore.computeInTransaction(txn -> {
            final Entity entity = getUserEntity(txn, user.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Not found."));
            entity.setProperty("displayName", user.getDisplayName());
            entity.setProperty("role", user.getRole().name());

            return new User(entity);
        });
    }

    public User changeAuthProvider(final String userId, final AuthProvider authProvider,
                                   final String rawPassword) {
        return entityStore.computeInTransaction(txn -> {
            final Entity entity = getUserEntity(txn, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Not found."));

            final String encodedPassword;
            if (authProvider == AuthProvider.PASSWORD) {
                final StandardPasswordEncoder encoder = new StandardPasswordEncoder();
                encodedPassword = encoder.encode(rawPassword);
            } else {
                encodedPassword = "";
            }

            // update
            entity.setProperty("authProvider", authProvider.name());
            entity.setProperty("password", encodedPassword);

            return new User(entity);
        });
    }

    public User changePassword(final String userId, final String oldPassword, final String rawPassword) {
        return entityStore.computeInTransaction(txn -> {
            final Entity entity = getUserEntity(txn, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Not found."));
            final User user = new User(entity);

            if (user.getAuthProvider() != AuthProvider.PASSWORD) {
                throw new IllegalArgumentException("This user is not PASSWORD Auth.");
            }

            final StandardPasswordEncoder encoder = new StandardPasswordEncoder();
            if (!encoder.matches(oldPassword, user.getPassword().getValue())) {
                throw new IllegalArgumentException("Old password does not match.");
            }
            final String encodedPassword = encoder.encode(rawPassword);

            // update
            entity.setProperty("password", encodedPassword);

            return new User(entity);
        });
    }

    public void delete(String userId) {
        entityStore.executeInTransaction(txn -> {
            Entity entity = getUserEntity(txn, userId)
                    .orElseThrow(() -> new IllegalArgumentException("Not found."));
            entity.delete();
        });
    }

    private static Optional<Entity> getUserEntity(final StoreTransaction txn, final String userId) {
        return Optional.ofNullable(txn.find(User.ENTITY_TYPE, "userId", userId).getFirst());
    }
}

