package com.behase.relumin.config;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import lombok.Data;

@Data
public class ReluminConfig {
	private ServerConfig server = new ServerConfig();
	private RedisConfig redis = new RedisConfig();
	private SchedulerConfig scheduler = new SchedulerConfig();
	private Notice notice = new Notice();

	private List<String> errors = Lists.newArrayList();

	public static ReluminConfig create(String configLocation) throws JsonParseException, JsonMappingException,
			IOException {
		ClassPathResource configResource = new ClassPathResource(configLocation);
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		return mapper.readValue(configResource.getInputStream(), ReluminConfig.class);
	}

	public Properties getProperties() {
		validate();

		Properties prop = new Properties();

		// server
		prop.setProperty("server.port", StringUtils.defaultString(server.getPort(), ServerConfig.DEFAULT_PORT));
		prop.setProperty("management.port", StringUtils.defaultString(server.getMonitorPort(), ServerConfig.DEFAULT_MONITOR_PORT));

		// redis
		prop.setProperty("redis.host", redis.getHost());
		prop.setProperty("redis.port", redis.getPort());

		// scheduler
		prop.setProperty(
			"scheduler.refreshClustersIntervalMillis",
			StringUtils.defaultString(scheduler.getRefreshClustersIntervalMillis(), SchedulerConfig.DEFAULT_REFRESH_CLUSTERS_INTERVAL_MILLIS));
		prop.setProperty(
			"scheduler.collectStaticsInfoIntervalMillis",
			StringUtils.defaultString(scheduler.getCollectStaticsInfoIntervalMillis(), SchedulerConfig.DEFAULT_COLLECT_STATICS_INFO_INTERVAL_MILLIS));
		prop.setProperty(
			"scheduler.collectStaticsInfoMaxCount",
			StringUtils.defaultString(scheduler.getCollectStaticsInfoMaxCount(), SchedulerConfig.DEFAULT_COLLECT_STATICS_INFO_MAX_COUNT));

		// notice
		// notice email
		if (notice.getMail().getHost() != null) {
			prop.setProperty("notice.mail.host", notice.getMail().getHost());
		}
		if (notice.getMail().getPort() != null) {
			prop.setProperty("notice.mail.port", notice.getMail().getPort());
		}
		if (notice.getMail().getUser() != null) {
			prop.setProperty("notice.mail.user", notice.getMail().getUser());
		}
		if (notice.getMail().getPassword() != null) {
			prop.setProperty("notice.mail.password", notice.getMail().getPassword());
		}
		prop.setProperty("notice.mail.charset", StringUtils.defaultString(notice.getMail().getCharset(), NoticeMailConfig.DEFAULT_CHARSET));
		// notice http
		if (notice.getHttp().getHost() != null) {
			prop.setProperty("notice.http.host", notice.getHttp().getHost());
		}
		if (notice.getHttp().getPort() != null) {
			prop.setProperty("notice.http.port", notice.getHttp().getPort());
		}

		return prop;
	}

	private void validate() {
		List<String> errors = Lists.newArrayList();

		// server
		if (server.getPort() != null) {
			check(isNumber(server.getPort()), "'server.port' must be numeric.");
		}
		if (server.getMonitorPort() != null) {
			check(isNumber(server.getMonitorPort()), "'server.monitorPort' must be numeric.");
		}

		// redis
		check(StringUtils.isNotBlank(redis.getHost()), "'redis.host' is blank.");
		check(StringUtils.isNotBlank(redis.getPort()), "'redis.port' is blank.");
		check(isNumber(redis.getPort()), "'redis.port' must be numeric.");

		// scheduler
		if (scheduler.getRefreshClustersIntervalMillis() != null) {
			check(isNumber(scheduler.getRefreshClustersIntervalMillis()), "'scheduler.refreshClustersIntervalMillis' must be numeric.");
		}
		if (scheduler.getCollectStaticsInfoIntervalMillis() != null) {
			check(isNumber(scheduler.getCollectStaticsInfoIntervalMillis()), "'scheduler.collectStaticsInfoIntervalMillis' must be numeric.");
		}
		if (scheduler.getCollectStaticsInfoMaxCount() != null) {
			check(isNumber(scheduler.getCollectStaticsInfoMaxCount()), "'scheduler.collectStaticsInfoMaxCount' must be numeric.");
		}

		// notice
		// notice email
		if (notice.getMail().getPort() != null) {
			check(isNumber(notice.getMail().getPort()), "'notice.email.port' must be numeric.");
		}
		// notice http
		if (notice.getHttp().getPort() != null) {
			check(isNumber(notice.getHttp().getPort()), "'notice.http.port' must be numeric.");
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

	private boolean isNumber(String str) {
		try {
			Integer.valueOf(str);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
