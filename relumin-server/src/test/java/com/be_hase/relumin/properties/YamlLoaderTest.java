package com.be_hase.relumin.properties;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.Map;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YamlLoaderTest {

    @Test
    public void load() {
        final YamlLoader target = new YamlLoader(new ClassPathResource("yaml-load-test.yml"));
        Map<String, Object> result = target.load();
        assertThat(result).containsEntry("string", "string")
                          .containsEntry("int", 1)
                          .containsEntry("map.key1", "val1")
                          .containsEntry("map.key2", 2)
                          .containsEntry("map.nest-map.key1", "val1")
                          .containsEntry("map.nest-map.key2", 2)
                          .containsEntry("hoge.bar", "hogeBar")
        ;
        log.debug("result={}", result);
    }
}