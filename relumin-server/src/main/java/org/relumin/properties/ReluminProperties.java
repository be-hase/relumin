package org.relumin.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Component
@ConfigurationProperties(prefix = "relumin")
@Validated
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReluminProperties {
    private String metaDataDir;
}
