package com.behase.relumin.config;

import com.behase.relumin.config.NoticeConfig.NoticeMailConfig;
import com.behase.relumin.config.OutputMetricsConfig.OutputMetricsFluentdConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import static org.apache.commons.lang3.StringUtils.*;

@Data
public class ReluminConfig {
    private String host;
    private AuthConfig auth = new AuthConfig();
    private ServerConfig server = new ServerConfig();
    private RedisConfig redis = new RedisConfig();
    private SchedulerConfig scheduler = new SchedulerConfig();
    private NoticeConfig notice = new NoticeConfig();
    private OutputMetricsConfig outputMetrics = new OutputMetricsConfig();

    private List<String> errors = Lists.newArrayList();

    public static ReluminConfig create(String configLocation) throws IOException {
        if (Paths.get(configLocation).toFile().exists()) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(Paths.get(configLocation).toFile(), ReluminConfig.class);
        } else {
            ClassPathResource configResource = new ClassPathResource(configLocation);
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(configResource.getInputStream(), ReluminConfig.class);
        }
    }

    public Properties getProperties() {
        validate();

        Properties prop = new Properties();

        //relumin
        prop.setProperty(
                "relumin.host",
                defaultString(host));

        // auth
        prop.setProperty(
                "auth.enabled",
                defaultIfBlank(
                        auth.getEnabled(),
                        AuthConfig.DEFAULT_ENABLED));
        prop.setProperty(
                "auth.allowAnonymous",
                defaultIfBlank(
                        auth.getAllowAnonymous(),
                        AuthConfig.DEFAULT_ALLOW_ANONYMOUS));

        // server
        prop.setProperty(
                "server.port",
                defaultIfBlank(
                        server.getPort(),
                        ServerConfig.DEFAULT_PORT));
        prop.setProperty(
                "management.port",
                defaultIfBlank(
                        server.getMonitorPort(),
                        ServerConfig.DEFAULT_MONITOR_PORT));

        // redis
        prop.setProperty(
                "redis.prefixKey",
                defaultIfBlank(
                        redis.getPrefixKey(),
                        RedisConfig.DEFAULT_PREFIX_KEY));
        prop.setProperty(
                "redis.host",
                redis.getHost());
        prop.setProperty(
                "redis.port",
                redis.getPort());

        // scheduler
        prop.setProperty(
                "scheduler.refreshClustersIntervalMillis",
                defaultIfBlank(
                        scheduler.getRefreshClustersIntervalMillis(),
                        SchedulerConfig.DEFAULT_REFRESH_CLUSTERS_INTERVAL_MILLIS));
        prop.setProperty(
                "scheduler.collectStaticsInfoIntervalMillis",
                defaultIfBlank(
                        scheduler.getCollectStaticsInfoIntervalMillis(),
                        SchedulerConfig.DEFAULT_COLLECT_STATICS_INFO_INTERVAL_MILLIS));
        prop.setProperty(
                "scheduler.collectStaticsInfoMaxCount",
                defaultIfBlank(
                        scheduler.getCollectStaticsInfoMaxCount(),
                        SchedulerConfig.DEFAULT_COLLECT_STATICS_INFO_MAX_COUNT));

        // notice email
        prop.setProperty(
                "notice.mail.host",
                defaultIfBlank(
                        notice.getMail().getHost(),
                        NoticeMailConfig.DEFAULT_HOST));
        prop.setProperty(
                "notice.mail.port",
                defaultIfBlank(
                        notice.getMail().getPort(),
                        NoticeMailConfig.DEFAULT_PORT));
        prop.setProperty(
                "notice.mail.user",
                defaultIfBlank(
                        notice.getMail().getUser(),
                        NoticeMailConfig.DEFAULT_USER));
        prop.setProperty(
                "notice.mail.password",
                defaultIfBlank(
                        notice.getMail().getPassword(),
                        NoticeMailConfig.DEFAULT_PASSWORD));
        prop.setProperty(
                "notice.mail.from",
                defaultIfBlank(
                        notice.getMail().getFrom(),
                        NoticeMailConfig.DEFAULT_FROM));
        prop.setProperty(
                "notice.mail.charset",
                defaultIfBlank(
                        notice.getMail().getCharset(),
                        NoticeMailConfig.DEFAULT_CHARSET));

        // fluentd
        prop.setProperty(
                "outputMetrics.fluentd.enabled",
                defaultIfBlank(
                        outputMetrics.getFluentd().getEnabled(),
                        OutputMetricsFluentdConfig.DEFAULT_ENABLED));
        prop.setProperty(
                "outputMetrics.fluentd.host",
                defaultIfBlank(
                        outputMetrics.getFluentd().getHost(),
                        OutputMetricsFluentdConfig.DEFAULT_HOST));
        prop.setProperty(
                "outputMetrics.fluentd.port",
                defaultIfBlank(
                        outputMetrics.getFluentd().getPort(),
                        OutputMetricsFluentdConfig.DEFAULT_PORT));
        prop.setProperty(
                "outputMetrics.fluentd.timeout",
                defaultIfBlank(
                        outputMetrics.getFluentd().getTimeout(),
                        OutputMetricsFluentdConfig.DEFAULT_TIMEOUT));
        prop.setProperty(
                "outputMetrics.fluentd.bufferCapacity",
                defaultIfBlank(
                        outputMetrics.getFluentd().getBufferCapacity(),
                        OutputMetricsFluentdConfig.DEFAULT_BUFFER_CAPACITY));
        prop.setProperty(
                "outputMetrics.fluentd.tag",
                defaultIfBlank(
                        outputMetrics.getFluentd().getTag(),
                        OutputMetricsFluentdConfig.DEFAULT_TAG));
        prop.setProperty(
                "outputMetrics.fluentd.nodeTag",
                defaultIfBlank(
                        outputMetrics.getFluentd().getNodeTag(),
                        OutputMetricsFluentdConfig.DEFAULT_NODE_TAG));

        return prop;
    }

    void validate() {
        List<String> errors = Lists.newArrayList();

        // server
        if (server.getPort() != null) {
            check(isInteger(server.getPort()), "'server.port' must be integer.");
        }
        if (server.getMonitorPort() != null) {
            check(isInteger(server.getMonitorPort()), "'server.monitorPort' must be integer.");
        }

        // auth
        if (auth.getEnabled() != null) {
            check(isBoolean(auth.getEnabled()), "'auth.enabled' must be boolean.");
        }
        if (auth.getAllowAnonymous() != null) {
            check(isBoolean(auth.getAllowAnonymous()), "'auth.allowAnonymous' must be boolean.");
        }

        // redis
        check(isNotBlank(redis.getHost()), "'redis.host' is blank.");
        check(isInteger(redis.getPort()), "'redis.port' must be integer.");

        // scheduler
        if (scheduler.getRefreshClustersIntervalMillis() != null) {
            check(isInteger(scheduler.getRefreshClustersIntervalMillis()), "'scheduler.refreshClustersIntervalMillis' must be integer.");
        }
        if (scheduler.getCollectStaticsInfoIntervalMillis() != null) {
            check(isInteger(scheduler.getCollectStaticsInfoIntervalMillis()), "'scheduler.collectStaticsInfoIntervalMillis' must be integer.");
        }
        if (scheduler.getCollectStaticsInfoMaxCount() != null) {
            check(isInteger(scheduler.getCollectStaticsInfoMaxCount()), "'scheduler.collectStaticsInfoMaxCount' must be integer.");
        }

        // notice
        // notice email
        if (notice.getMail().getPort() != null) {
            check(isInteger(notice.getMail().getPort()), "'notice.email.port' must be integer.");
        }

        // output metrics
        // fluentd
        if (outputMetrics.getFluentd().getEnabled() != null) {
            check(isInteger(outputMetrics.getFluentd().getEnabled()), "'outputMetcis.fluentd.enabled' must be boolean.");
        }
        if (outputMetrics.getFluentd().getPort() != null) {
            check(isInteger(outputMetrics.getFluentd().getPort()), "'outputMetcis.fluentd.port' must be integer.");
        }
        if (outputMetrics.getFluentd().getTimeout() != null) {
            check(isInteger(outputMetrics.getFluentd().getTimeout()), "'outputMetcis.fluentd.timeout' must be integer.");
        }
        if (outputMetrics.getFluentd().getBufferCapacity() != null) {
            check(isInteger(outputMetrics.getFluentd().getBufferCapacity()), "'outputMetcis.fluentd.bufferCapacity' must be integer.");
        }

        if (errors.size() > 0) {
            throw new IllegalStateException(Joiner.on(",").join(errors));
        }
    }

    void check(boolean bool, String error) {
        if (!bool) {
            errors.add(error);
        }
    }

    static boolean isInteger(String str) {
        try {
            Integer.valueOf(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static boolean isBoolean(String str) {
        return StringUtils.equals(str, "true") || StringUtils.equals(str, "false");
    }
}
