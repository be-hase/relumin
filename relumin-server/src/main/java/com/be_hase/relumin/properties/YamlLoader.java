package com.be_hase.relumin.properties;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.config.YamlProcessor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class YamlLoader extends YamlProcessor {
    public YamlLoader(String configLocation) {
        this(new FileSystemResource(configLocation));
    }

    public YamlLoader(Resource resource) {
        setResources(resource);
    }

    public Map<String, Object> load() {
        final Map<String, Object> result = new LinkedHashMap<>();
        process((properties, map) -> result.putAll(getFlattenedMap(map)));
        return result;
    }
}
