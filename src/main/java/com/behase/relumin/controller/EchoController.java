package com.behase.relumin.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class EchoController {

    @RequestMapping(value = "/echo", method = RequestMethod.POST)
    public Object echo(
            @RequestBody String body
    ) {
        log.debug("echo = {}", body);
        return body;
    }
}
