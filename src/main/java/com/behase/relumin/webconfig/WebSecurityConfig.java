package com.behase.relumin.webconfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.web.authentication.rememberme.InMemoryTokenRepositoryImpl;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.behase.relumin.model.Role;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	@Autowired
	private UserDetailsService userDetailsService;

	@Value("${auth.enabled:false}")
	private boolean authEnabled;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable();
		if (authEnabled) {
			http
				.rememberMe()
				.tokenRepository(new InMemoryTokenRepositoryImpl())
				.tokenValiditySeconds(Integer.MAX_VALUE).and()

				.authorizeRequests()
				.antMatchers("/css/**", "/js/**", "/img/**", "**/favicon.ico", "/vendor/**")
				.permitAll()
				.antMatchers(HttpMethod.POST, "/api/cluster/*", "/api/cluster/*/delete", "/api/cluster/*/notice")
				.hasAnyAuthority(Role.REDIS_ADMIN.getAuthority(), Role.RELUMIN_ADMIN.getAuthority())
				.antMatchers(HttpMethod.POST, "/api/trib/**")
				.hasAnyAuthority(Role.REDIS_ADMIN.getAuthority(), Role.RELUMIN_ADMIN.getAuthority())
				.antMatchers(HttpMethod.POST, "/api/user/**")
				.hasAnyAuthority(Role.RELUMIN_ADMIN.getAuthority())
				.anyRequest()
				.authenticated()
				.and()

				.formLogin()
				.loginPage("/login")
				.defaultSuccessUrl("/")
				.permitAll()
				.and()

				.logout()
				.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
				.permitAll();
		}
	}

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService).passwordEncoder(new StandardPasswordEncoder());
	}
}
