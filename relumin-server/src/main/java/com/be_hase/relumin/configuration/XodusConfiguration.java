package com.be_hase.relumin.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.be_hase.relumin.properties.ReluminProperties;

import jetbrains.exodus.entitystore.PersistentEntityStore;
import jetbrains.exodus.entitystore.PersistentEntityStores;
import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class XodusConfiguration {
    private final ReluminProperties reluminProperties;

    @Bean
    public PersistentEntityStore metaDataPersistentEntityStore() {
        PersistentEntityStore entityStore = PersistentEntityStores.newInstance(
                reluminProperties.getMetaDataDir());
        return entityStore;
    }
}
