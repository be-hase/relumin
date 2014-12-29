package com.behase.relumin;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import redis.clients.jedis.JedisPool;

import com.behase.relumin.config.ReluminConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableAutoConfiguration
@ComponentScan
public class Application extends WebMvcConfigurerAdapter {
	private static final String CONFIG_LOCATION = "config";

	@Value("${redis.host}")
	private String redisHost;

	@Value("${redis.port}")
	private int redisPort;

	public static void main(String[] args) throws IOException {
		String configLocation = System.getProperty(CONFIG_LOCATION, "relumin-conf.yml");
		checkArgument(configLocation != null, "Specify config VM parameter.");

		ClassPathResource configResource = new ClassPathResource(configLocation);
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		ReluminConfig config = mapper.readValue(configResource.getInputStream(), ReluminConfig.class);
		log.info("config : {}", config);

		SpringApplication app = new SpringApplication(Application.class);
		app.setAddCommandLineProperties(false);
		app.setDefaultProperties(config.getProperties());
		app.run(args);
	}

	@Bean
	@Primary
	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();

		objectMapper.registerModule(new AfterburnerModule());
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		objectMapper.setDateFormat(format);

		return objectMapper;
	}

	@Bean
	public FilterRegistrationBean characterEncodingFilterRegistrationBean() {
		CharacterEncodingFilter filter = new CharacterEncodingFilter();
		filter.setEncoding("UTF-8");
		filter.setForceEncoding(true);

		FilterRegistrationBean registrationBean = new FilterRegistrationBean();
		registrationBean.setFilter(filter);
		registrationBean.addUrlPatterns("/*");
		registrationBean.setOrder(0);

		return registrationBean;
	}

	@Bean(name = "datastoreRedis", destroyMethod = "destroy")
	public JedisPool jedisPool() {
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(10);
		config.setMaxIdle(10);
		config.setMinIdle(5);
		config.setMaxWaitMillis(3000L);
		config.setTestOnBorrow(true);

		JedisPool pool = new JedisPool(config, redisHost, redisPort);
		return pool;
	}
}
