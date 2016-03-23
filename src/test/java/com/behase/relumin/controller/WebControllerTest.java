package com.behase.relumin.controller;

import com.behase.relumin.model.LoginUser;
import com.behase.relumin.model.Role;
import com.behase.relumin.service.UserService;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;

public class WebControllerTest {
	private WebController controller;
	private UserService userService;

	@Before
	public void init() {
		controller = new WebController();
		userService = mock(UserService.class);

		inject();
	}

	private void inject() {
		Whitebox.setInternalState(controller, "userService", userService);
		Whitebox.setInternalState(controller, "buildNumber", "1");
		Whitebox.setInternalState(controller, "collectStaticsInfoIntervalMillis", "1");
		Whitebox.setInternalState(controller, "authEnabled", true);
		Whitebox.setInternalState(controller, "authAllowAnonymous", true);
	}

	@Test
	public void index_with_auth() throws Exception {
		ExtendedModelMap model = new ExtendedModelMap();
		Authentication authentication = mock(Authentication.class);

		doReturn(new LoginUser("username", "displayName", "rawPassword", Role.VIEWER.getAuthority())).when(userService).getUser(anyString());

		controller.index(authentication, model);
		assertThat(model.get("buildNumber"), is("1"));
		assertThat(model.get("collectStaticsInfoIntervalMillis"), is("1"));
		assertThat(model.get("authEnabled"), is(true));
		assertThat(model.get("authAllowAnonymous"), is(true));
		assertThat(model.get("loginUsername"), is("username"));
		assertThat(model.get("login"), is(true));
	}

	@Test
	public void index() throws Exception {
		ExtendedModelMap model = new ExtendedModelMap();
		Authentication authentication = null;

		doReturn(new LoginUser("username", "displayName", "rawPassword", Role.VIEWER.getAuthority())).when(userService).getUser(anyString());

		controller.index(authentication, model);
		assertThat(model.get("buildNumber"), is("1"));
		assertThat(model.get("collectStaticsInfoIntervalMillis"), is("1"));
		assertThat(model.get("authEnabled"), is(true));
		assertThat(model.get("authAllowAnonymous"), is(true));
		assertThat(model.get("loginUsername"), is(""));
		assertThat(model.get("login"), is(false));
	}
}
