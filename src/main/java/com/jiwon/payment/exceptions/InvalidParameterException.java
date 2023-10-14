package com.jiwon.payment.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 405, Invalid Parameter Exception
@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
public class InvalidParameterException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidParameterException() {
        super(new StringBuilder("The parameter is invalid.").toString());
    }

    public InvalidParameterException(String invalidParameter) {
        super(new StringBuilder("Invalid Parameter : '").append(invalidParameter).append("'").toString());
    }
}
