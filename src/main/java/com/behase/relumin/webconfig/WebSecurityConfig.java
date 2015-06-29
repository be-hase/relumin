package com.behase.relumin.webconfig;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.rememberme.InMemoryTokenRepositoryImpl;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;

import com.behase.relumin.model.Role;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	@Autowired
	private UserDetailsService userDetailsService;

	@Value("${auth.enabled}")
	private boolean authEnabled;

	@Value("${auth.allowAnonymous}")
	private boolean authAllowAnonymous;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable();
		if (authEnabled) {
			http
				.rememberMe()
				.tokenRepository(new InMemoryTokenRepositoryImpl())
				.tokenValiditySeconds(Integer.MAX_VALUE).and()

				.authorizeRequests()
				.antMatchers("/login", "/css/**", "/js/**", "/img/**", "**/favicon.ico", "/vendor/**")
				.permitAll()
				.antMatchers(HttpMethod.POST, "/api/cluster/*", "/api/cluster/*/delete", "/api/cluster/*/notice")
				.hasAuthority(Role.RELUMIN_ADMIN.getAuthority())
				.antMatchers(HttpMethod.POST, "/api/trib/**")
				.hasAuthority(Role.RELUMIN_ADMIN.getAuthority())
				.antMatchers(HttpMethod.POST, "/api/user/**")
				.hasAuthority(Role.RELUMIN_ADMIN.getAuthority())
				.and()

				.formLogin()
				.loginPage("/login")
				.defaultSuccessUrl("/")
				.and()

				.logout()
				.logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
				.logoutSuccessUrl("/")
				.and()

				.exceptionHandling()
				.accessDeniedHandler(new AccessDeniedHandler() {
					@Override
					public void handle(HttpServletRequest request, HttpServletResponse response,
							AccessDeniedException accessDeniedException) throws IOException, ServletException {
						response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					}
				})
				.defaultAuthenticationEntryPointFor(
					new AuthenticationEntryPoint() {
						@Override
						public void commence(HttpServletRequest request, HttpServletResponse response,
								AuthenticationException authException)
								throws IOException, ServletException {
							response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
						}
					},
					new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest")
				);

			if (!authAllowAnonymous) {
				http
					.authorizeRequests()
					.anyRequest()
					.authenticated();
			}
		}

	}

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService).passwordEncoder(new StandardPasswordEncoder());
	}
}
