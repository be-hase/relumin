package com.behase.relumin;

import com.behase.relumin.config.ReluminConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Configuration
@EnableAutoConfiguration
@EnableScheduling
@EnableWebSecurity
@ComponentScan
public class Application extends WebMvcConfigurerAdapter {
    private static final String CONFIG_LOCATION = "config";

    public static void main(String[] args) throws IOException {
        log.info("Starting Relumin...");

        String configLocation = System.getProperty(CONFIG_LOCATION);
        checkArgument(configLocation != null, "Specify config VM parameter.");

        ReluminConfig config = ReluminConfig.create(configLocation);
        log.info("config : {}", config);

        SpringApplication app = new SpringApplication(Application.class);
        app.setDefaultProperties(config.getProperties());
        app.run(args);
    }
}
