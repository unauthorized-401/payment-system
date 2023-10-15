package com.jiwon.payment.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 404, Resource Not Found Exception
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException() {
        super(new StringBuilder("Can't find the payment history.").toString());
    }

    public ResourceNotFoundException(String id) {
        super(new StringBuilder("This payment is already canceled completely.").toString());
    }
}
