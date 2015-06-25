package com.behase.relumin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.behase.relumin.config.SchedulerConfig;
import com.behase.relumin.model.LoginUser;
import com.behase.relumin.service.UserService;

@Controller
public class WebController {
	@Autowired
	UserService userService;

	@Value("${build.number}")
	private String buildNumber;

	@Value("${scheduler.collectStaticsInfoIntervalMillis:"
		+ SchedulerConfig.DEFAULT_COLLECT_STATICS_INFO_INTERVAL_MILLIS + "}")
	private String collectStaticsInfoIntervalMillis;

	@Value("${auth.enabled}")
	private boolean authEnabled;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String getClusterList(
			Authentication authentication,
			Model model
			) throws Exception {
		model.addAttribute("buildNumber", buildNumber);
		model.addAttribute("collectStaticsInfoIntervalMillis", collectStaticsInfoIntervalMillis);
		model.addAttribute("authEnabled", authEnabled);
		if (authEnabled && authentication != null) {
			LoginUser loginUser = userService.getUser(authentication.getName());
			model.addAttribute("loginUser", loginUser);
		}
		return "/index";
	}
}
