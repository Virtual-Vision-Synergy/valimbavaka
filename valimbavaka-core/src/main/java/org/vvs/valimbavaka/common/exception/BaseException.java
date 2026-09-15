package org.vvs.valimbavaka.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class BaseException extends RuntimeException {
    private final HttpStatus status;

    protected BaseException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    protected BaseException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}