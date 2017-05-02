package com.be_hase.relumin.service;

import org.junit.Test;

import jetbrains.exodus.entitystore.Entity;
import jetbrains.exodus.entitystore.PersistentEntityStore;
import jetbrains.exodus.entitystore.PersistentEntityStores;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserServiceTest {

    @Test
    public void test() {
        PersistentEntityStore entityStore = PersistentEntityStores.newInstance(
                "/Users/ryosukehasebe/Desktop/reluminMeta");

        try {
            entityStore.executeInTransaction(txn -> {
                final Entity user = txn.newEntity("User");
                user.setProperty("username", "JP11644");
            });
        } catch (Exception e) {

        }

        entityStore.executeInTransaction(txn -> {
            txn.getAll("User").forEach(
                    v -> {
                        String id = v.getId().toString();
                        String username = (String) v.getProperty("username");
                        log.info("id={}, username={}", id, username);
                    });
        });
    }
}