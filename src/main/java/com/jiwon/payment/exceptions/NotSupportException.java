package com.jiwon.payment.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 406, Not Support Exception
@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class NotSupportException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NotSupportException() {
        super(new StringBuilder("This cancel request is invalid.").toString());
    }

    public NotSupportException(String invalidParameter) {
        super(new StringBuilder("The ").append(invalidParameter).append("value is not correct.").toString());
    }
}
