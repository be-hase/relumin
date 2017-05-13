package org.relumin.test;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.lambdaworks.redis.RedisURI;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Component
@ConfigurationProperties(prefix = "test.redis")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestRedisProperties {
    private List<RedisUri> clusterUris;
    private List<RedisUri> standaloneUris;
    private List<RedisUri> standalonePasswordUris;

    @Data
    public static class RedisUri {
        private String host;
        private int port;
        private String password;

        public String toHostAndPort() {
            return host + ":" + port;
        }

        public RedisURI toRedisURI() {
            final RedisURI.Builder builder = RedisURI.builder().withHost(host).withPort(port);
            if (StringUtils.isNotBlank(password)) {
                builder.withPassword(password);
            }
            return builder.build();
        }
    }
}
