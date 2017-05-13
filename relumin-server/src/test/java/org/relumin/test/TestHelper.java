package org.relumin.test;

import org.relumin.support.redis.RedisSupport;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
public class TestHelper {
    private final TestRedisProperties testRedisProperties;
    private final RedisSupport redisSupport;

    public void resetAllRedis() {
        testRedisProperties.getClusterUris().forEach(v -> {
            redisSupport.executeCommands(v.toRedisURI(), commands -> {
                log.info("Reset {}", v);
                try {
                    commands.flushall();
                } catch (Exception ignored) {
                }
                commands.clusterReset(true);
            });
        });
        testRedisProperties.getStandaloneUris().forEach(v -> {
            redisSupport.executeCommands(v.toRedisURI(), commands -> {
                log.info("Reset {}", v);
                commands.flushall();
            });
        });
    }

}
