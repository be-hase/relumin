package com.behase.relumin.controller;

import com.behase.relumin.model.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
public class MyErrorController implements ErrorController {
    private static final String ERROR_PATH = "/error";

    @RequestMapping(ERROR_PATH)
    @ResponseBody
    public ResponseEntity<ErrorResponse> handleError(
            HttpServletRequest request) throws JsonProcessingException {
        HttpStatus status = getStatus(request);

        if (status.equals(HttpStatus.NOT_FOUND) || ERROR_PATH.equals(request.getRequestURI())) {
            ErrorResponse response = new ErrorResponse("404", "Not Found.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        ErrorResponse response = new ErrorResponse(String.valueOf(getStatus(request)), getMessage(request));
        return new ResponseEntity<>(response, getStatus(request));
    }

    @Override
    public String getErrorPath() {
        return ERROR_PATH;
    }

    HttpStatus getStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (statusCode != null) {
            try {
                return HttpStatus.valueOf(statusCode);
            } catch (Exception e) {
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    String getMessage(HttpServletRequest request) {
        String message = (String) request.getAttribute("javax.servlet.error.message");
        return StringUtils.defaultIfBlank(message, "No message.");
    }
}
