package com.jiwon.payment.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 226, Already Used Exception
@ResponseStatus(HttpStatus.IM_USED)
public class AlreadyUsedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AlreadyUsedException() {
        super(new StringBuilder("The payment or cancel is in progress.").toString());
    }

    public AlreadyUsedException(String parameter) {
        super(new StringBuilder("The payment or cancel is in progress. {}").append(parameter).append("'").toString());
    }
}
