package com.behase.relumin.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

@Slf4j
public class ReluminConfigTest {
	@Test
	public void check() {
		ReluminConfig config = new ReluminConfig();

		assertThat(config.getErrors().size(), is(0));
		config.check(false, "error");
		assertThat(config.getErrors().size(), is(1));
		assertThat(config.getErrors(), contains("error"));
		config.check(true, "error");
		assertThat(config.getErrors().size(), is(1));
	}

	@Test
	public void isInteger() {
		assertThat(ReluminConfig.isInteger(""), is(false));
		assertThat(ReluminConfig.isInteger("1"), is(true));
		assertThat(ReluminConfig.isInteger("0"), is(true));
		assertThat(ReluminConfig.isInteger("-1"), is(true));
		assertThat(ReluminConfig.isInteger("1.0"), is(false));
		assertThat(ReluminConfig.isInteger("hoge"), is(false));
	}

	@Test
	public void isBoolean() {
		assertThat(ReluminConfig.isBoolean(""), is(false));
		assertThat(ReluminConfig.isBoolean("true"), is(true));
		assertThat(ReluminConfig.isBoolean("TRue"), is(true));
		assertThat(ReluminConfig.isBoolean("false"), is(true));
		assertThat(ReluminConfig.isBoolean("falsE"), is(true));
		assertThat(ReluminConfig.isBoolean("hoge"), is(false));
	}


	@Test
	public void validate() {
		ReluminConfig config = new ReluminConfig();

		// server
		config.getServer().setPort("hoge");
		config.getServer().setMonitorPort("hoge");

		// auth
		config.getAuth().setEnabled("hoge");
		config.getAuth().setAllowAnonymous("hoge");

		// redis
		config.getRedis().setHost("");
		config.getRedis().setPort("hoge");

		// scheduler
		config.getScheduler().setRefreshClustersIntervalMillis("hoge");
		config.getScheduler().setCollectStaticsInfoIntervalMillis("hoge");
		config.getScheduler().setCollectStaticsInfoMaxCount("hoge");

		// notice
		config.getNotice().getMail().setPort("hoge");

		// fluentd
		config.getOutputMetrics().getFluentd().setEnabled("hoge");
		config.getOutputMetrics().getFluentd().setPort("hoge");
		config.getOutputMetrics().getFluentd().setTimeout("hoge");
		config.getOutputMetrics().getFluentd().setBufferCapacity("hoge");

		try {
			config.validate();
		} catch (Exception e) {
		}

		assertThat(config.getErrors().size(), is(14));
	}

	@Test
	public void getProperties() {
		ReluminConfig config = new ReluminConfig();
		config.getRedis().setHost("hoge");
		config.getRedis().setPort("9000");

		Properties props = config.getProperties();
		assertThat(props.getProperty("relumin.host"), is(""));
		assertThat(props.getProperty("auth.enabled"), is("false"));
		assertThat(props.getProperty("auth.allowAnonymous"), is("false"));
		assertThat(props.getProperty("server.port"), is("8080"));
		assertThat(props.getProperty("management.port"), is("20080"));
		assertThat(props.getProperty("redis.prefixKey"), is("_relumin"));
		assertThat(props.getProperty("redis.host"), is("hoge"));
		assertThat(props.getProperty("redis.port"), is("9000"));
		assertThat(props.getProperty("scheduler.refreshClustersIntervalMillis"), is("120000"));
		assertThat(props.getProperty("scheduler.collectStaticsInfoIntervalMillis"), is("120000"));
		assertThat(props.getProperty("scheduler.collectStaticsInfoMaxCount"), is("1500"));
		assertThat(props.getProperty("notice.mail.host"), is(""));
		assertThat(props.getProperty("notice.mail.port"), is("0"));
		assertThat(props.getProperty("notice.mail.user"), is(""));
		assertThat(props.getProperty("notice.mail.password"), is(""));
		assertThat(props.getProperty("notice.mail.from"), is(""));
		assertThat(props.getProperty("notice.mail.charset"), is("UTF-8"));
		assertThat(props.getProperty("outputMetrics.fluentd.enabled"), is("false"));
		assertThat(props.getProperty("outputMetrics.fluentd.host"), is(""));
		assertThat(props.getProperty("outputMetrics.fluentd.port"), is("0"));
		assertThat(props.getProperty("outputMetrics.fluentd.timeout"), is("3000"));
		assertThat(props.getProperty("outputMetrics.fluentd.bufferCapacity"), is("1048576"));
		assertThat(props.getProperty("outputMetrics.fluentd.tag"), is("relumin"));
		assertThat(props.getProperty("outputMetrics.fluentd.nodeTag"), is("node"));
	}
}
