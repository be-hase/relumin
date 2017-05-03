package com.be_hase.relumin.configuration;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
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
        final String dir = StringUtils.defaultIfBlank(
                reluminProperties.getMetaDataDir(),
                System.getProperty("user.home") + File.separator + ".reluminMetaData");
        PersistentEntityStore entityStore = PersistentEntityStores.newInstance(dir);
        return entityStore;
    }
}
