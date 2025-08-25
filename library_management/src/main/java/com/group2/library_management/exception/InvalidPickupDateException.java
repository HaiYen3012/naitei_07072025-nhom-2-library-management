package com.group2.library_management.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPickupDateException extends RuntimeException {
    public InvalidPickupDateException() {
    }
    
    public InvalidPickupDateException(String message) {
        super(message);
    }
}
