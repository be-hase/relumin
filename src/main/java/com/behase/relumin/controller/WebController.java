package com.behase.relumin.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.behase.relumin.config.SchedulerConfig;

@Controller
public class WebController {
	@Value("${build.number}")
	private String buildNumber;

	@Value("${scheduler.collectStaticsInfoIntervalMillis:"
		+ SchedulerConfig.DEFAULT_COLLECT_STATICS_INFO_INTERVAL_MILLIS + "}")
	private String collectStaticsInfoIntervalMillis;

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public Object getClusterList(
			Model model
			) {
		model.addAttribute("buildNumber", buildNumber);
		model.addAttribute("collectStaticsInfoIntervalMillis", collectStaticsInfoIntervalMillis);
		return "/index";
	}
}
