package org.relumin;

import java.io.FileNotFoundException;
import java.nio.file.Paths;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.relumin.properties.YamlLoader;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class Application {
    private static final String CONFIG_LOCATION = "config";

    public static void main(String[] args) throws FileNotFoundException {
        log.info("Starting Relumin...");

        final SpringApplication app = new SpringApplication(Application.class);

        final String configLocation = System.getProperty(CONFIG_LOCATION);
        if (configLocation != null && Paths.get(configLocation).toFile().exists()) {
            final YamlLoader yamlLoader = new YamlLoader(configLocation);
            app.setDefaultProperties(yamlLoader.load());
        }

        app.run(args);
    }
}
