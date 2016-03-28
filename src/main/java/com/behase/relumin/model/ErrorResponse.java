package com.behase.relumin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private Error error;

    public ErrorResponse(String code, String message) {
        this.error = new Error();
        this.error.code = code;
        this.error.message = message;
    }

    @Data
    public static class Error {
        private String code;
        private String message;
    }
}
