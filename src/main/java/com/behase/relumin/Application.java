package com.behase.relumin;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import redis.clients.jedis.JedisPool;

import com.behase.relumin.config.NoticeMailConfig;
import com.behase.relumin.config.ReluminConfig;
import com.behase.relumin.interceptor.AddResponseHeaderInterceptor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableAutoConfiguration
@EnableScheduling
@ComponentScan
public class Application extends WebMvcConfigurerAdapter {
	private static final String CONFIG_LOCATION = "config";

	@Autowired
	private AddResponseHeaderInterceptor addResponseHeaderInterceptor;

	@Value("${redis.host}")
	private String redisHost;

	@Value("${redis.port}")
	private int redisPort;

	@Value("${notice.mail.host:}")
	private String noticeMailHost;

	@Value("${notice.mail.port:0}")
	private int noticeMailPort;

	@Value("${notice.mail.from:}")
	private String noticeMailFrom;

	@Value("${notice.mail.charset:}")
	private String noticeMailCharset;

	@Value("${notice.mail.user:}")
	private String noticeMailUser;

	@Value("${notice.mail.password:}")
	private String noticeMailPassword;

	public static void main(String[] args) throws IOException {
		String configLocation = System.getProperty(CONFIG_LOCATION, "relumin-local-conf.yml");
		checkArgument(configLocation != null, "Specify config VM parameter.");

		ReluminConfig config = ReluminConfig.create(configLocation);
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
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);

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

	@Bean
	public MailSender JavaMailSenderImpl() {
		boolean notNotifyByMail = StringUtils.isBlank(noticeMailHost) || noticeMailPort == 0;
		if (notNotifyByMail) {
			return null;
		}

		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost(noticeMailHost);
		mailSender.setPort(noticeMailPort);
		mailSender.setDefaultEncoding(StringUtils.defaultString(noticeMailCharset, NoticeMailConfig.DEFAULT_CHARSET));
		if (StringUtils.isNotBlank(noticeMailUser) && StringUtils.isNotBlank(noticeMailPassword)) {
			mailSender.setUsername(noticeMailUser);
			mailSender.setPassword(noticeMailPassword);
		}
		return mailSender;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(addResponseHeaderInterceptor);
	}
}
