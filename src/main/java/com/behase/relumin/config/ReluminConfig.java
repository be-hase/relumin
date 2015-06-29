package com.behase.relumin.config;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;

import com.behase.relumin.config.NoticeConfig.NoticeMailConfig;
import com.behase.relumin.config.OutputMetricsConfig.OutputMetricsFluentdConfig;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import lombok.Data;

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

	public static ReluminConfig create(String configLocation) throws JsonParseException, JsonMappingException,
			IOException {
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
			StringUtils.defaultString(host));

		// auth
		prop.setProperty(
			"auth.enabled",
			StringUtils.defaultIfBlank(
				auth.getEnabled(),
				AuthConfig.DEFAULT_ENABLED));
		prop.setProperty(
			"auth.allowAnonymous",
			StringUtils.defaultIfBlank(
				auth.getAllowAnonymous(),
				AuthConfig.DEFAULT_ALLOW_ANONYMOUS));

		// server
		prop.setProperty(
			"server.port",
			StringUtils.defaultIfBlank(
				server.getPort(),
				ServerConfig.DEFAULT_PORT));
		prop.setProperty(
			"management.port",
			StringUtils.defaultIfBlank(
				server.getMonitorPort(),
				ServerConfig.DEFAULT_MONITOR_PORT));

		// redis
		prop.setProperty(
			"redis.prefixKey",
			StringUtils.defaultIfBlank(
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
			StringUtils.defaultIfBlank(
				scheduler.getRefreshClustersIntervalMillis(),
				SchedulerConfig.DEFAULT_REFRESH_CLUSTERS_INTERVAL_MILLIS));
		prop.setProperty(
			"scheduler.collectStaticsInfoIntervalMillis",
			StringUtils.defaultIfBlank(
				scheduler.getCollectStaticsInfoIntervalMillis(),
				SchedulerConfig.DEFAULT_COLLECT_STATICS_INFO_INTERVAL_MILLIS));
		prop.setProperty(
			"scheduler.collectStaticsInfoMaxCount",
			StringUtils.defaultIfBlank(
				scheduler.getCollectStaticsInfoMaxCount(),
				SchedulerConfig.DEFAULT_COLLECT_STATICS_INFO_MAX_COUNT));

		// notice email
		prop.setProperty(
			"notice.mail.host",
			StringUtils.defaultIfBlank(
				notice.getMail().getHost(),
				NoticeMailConfig.DEFAULT_HOST));
		prop.setProperty(
			"notice.mail.port",
			StringUtils.defaultIfBlank(
				notice.getMail().getPort(),
				NoticeMailConfig.DEFAULT_PORT));
		prop.setProperty(
			"notice.mail.user",
			StringUtils.defaultIfBlank(
				notice.getMail().getUser(),
				NoticeMailConfig.DEFAULT_USER));
		prop.setProperty(
			"notice.mail.password",
			StringUtils.defaultIfBlank(
				notice.getMail().getPassword(),
				NoticeMailConfig.DEFAULT_PASSWORD));
		prop.setProperty(
			"notice.mail.from",
			StringUtils.defaultIfBlank(
				notice.getMail().getFrom(),
				NoticeMailConfig.DEFAULT_FROM));
		prop.setProperty(
			"notice.mail.charset",
			StringUtils.defaultIfBlank(
				notice.getMail().getCharset(),
				NoticeMailConfig.DEFAULT_CHARSET));

		// output metrics
		// file
		//		prop.setProperty(
		//			"outputMetrics.file.enabled",
		//			StringUtils.defaultIfBlank(
		//				outputMetrics.getFile().getEnabled(),
		//				OutputMetricsFileConfig.DEFAULT_ENABLED));
		//		prop.setProperty(
		//			"outputMetrics.file.name",
		//			StringUtils.defaultIfBlank(
		//				outputMetrics.getFile().getName(),
		//				OutputMetricsFileConfig.DEFAULT_NAME));
		//		prop.setProperty(
		//			"outputMetrics.file.dir",
		//			StringUtils.defaultIfBlank(
		//				outputMetrics.getFile().getDir(),
		//				OutputMetricsFileConfig.DEFAULT_DIR));
		//		prop.setProperty(
		//			"outputMetrics.file.maxSize",
		//			StringUtils.defaultIfBlank(
		//				outputMetrics.getFile().getMaxSize(),
		//				OutputMetricsFileConfig.DEFAULT_MAX_SIZE));
		//		prop.setProperty(
		//			"outputMetrics.file.count",
		//			StringUtils.defaultIfBlank(
		//				outputMetrics.getFile().getCount(),
		//				OutputMetricsFileConfig.DEFAULT_COUNT));
		// fluentd
		prop.setProperty(
			"outputMetrics.fluentd.enabled",
			StringUtils.defaultIfBlank(
				outputMetrics.getFluentd().getEnabled(),
				OutputMetricsFluentdConfig.DEFAULT_ENABLED));
		prop.setProperty(
			"outputMetrics.fluentd.host",
			StringUtils.defaultIfBlank(
				outputMetrics.getFluentd().getHost(),
				OutputMetricsFluentdConfig.DEFAULT_HOST));
		prop.setProperty(
			"outputMetrics.fluentd.port",
			StringUtils.defaultIfBlank(
				outputMetrics.getFluentd().getPort(),
				OutputMetricsFluentdConfig.DEFAULT_PORT));
		prop.setProperty(
			"outputMetrics.fluentd.timeout",
			StringUtils.defaultIfBlank(
				outputMetrics.getFluentd().getTimeout(),
				OutputMetricsFluentdConfig.DEFAULT_TIMEOUT));
		prop.setProperty(
			"outputMetrics.fluentd.bufferCapacity",
			StringUtils.defaultIfBlank(
				outputMetrics.getFluentd().getBufferCapacity(),
				OutputMetricsFluentdConfig.DEFAULT_BUFFER_CAPACITY));
		prop.setProperty(
			"outputMetrics.fluentd.tag",
			StringUtils.defaultIfBlank(
				outputMetrics.getFluentd().getTag(),
				OutputMetricsFluentdConfig.DEFAULT_TAG));
		prop.setProperty(
			"outputMetrics.fluentd.nodeTag",
			StringUtils.defaultIfBlank(
				outputMetrics.getFluentd().getNodeTag(),
				OutputMetricsFluentdConfig.DEFAULT_NODE_TAG));

		return prop;
	}

	private void validate() {
		List<String> errors = Lists.newArrayList();

		// server
		if (server.getPort() != null) {
			check(isNumeric(server.getPort()), "'server.port' must be numeric.");
		}
		if (server.getMonitorPort() != null) {
			check(isNumeric(server.getMonitorPort()), "'server.monitorPort' must be numeric.");
		}

		// auth

		// redis
		check(StringUtils.isNotBlank(redis.getHost()), "'redis.host' is blank.");
		check(StringUtils.isNotBlank(redis.getPort()), "'redis.port' is blank.");
		check(isNumeric(redis.getPort()), "'redis.port' must be numeric.");

		// scheduler
		if (scheduler.getRefreshClustersIntervalMillis() != null) {
			check(isInteger(scheduler.getRefreshClustersIntervalMillis()), "'scheduler.refreshClustersIntervalMillis' must be integer.");
		}
		if (scheduler.getCollectStaticsInfoIntervalMillis() != null) {
			check(isInteger(scheduler.getCollectStaticsInfoIntervalMillis()), "'scheduler.collectStaticsInfoIntervalMillis' must be integer.");
		}
		if (scheduler.getCollectStaticsInfoMaxCount() != null) {
			check(isInteger(scheduler.getCollectStaticsInfoMaxCount()), "'scheduler.collectStaticsInfoMaxCount' must be numeric.");
		}

		// notice
		// notice email
		if (notice.getMail().getPort() != null) {
			check(isNumeric(notice.getMail().getPort()), "'notice.email.port' must be numeric.");
		}

		// output metrics
		// file
		//		if (outputMetrics.getFile().getCount() != null) {
		//			check(isNumeric(outputMetrics.getFile().getCount()), "'outputMetcis.file.count' must be numeric.");
		//		}
		// fluentd
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

	private void check(boolean bool, String error) {
		if (!bool) {
			errors.add(error);
		}
	}

	private boolean isInteger(String str) {
		try {
			Integer.valueOf(str);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private boolean isNumeric(String str) {
		return StringUtils.isNumeric(str);
	}
}
