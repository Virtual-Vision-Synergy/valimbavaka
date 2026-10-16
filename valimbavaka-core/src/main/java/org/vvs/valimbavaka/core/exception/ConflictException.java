package org.vvs.valimbavaka.core.exception;

import java.io.Serial;

import org.springframework.http.HttpStatus;

/** Doublon, contrainte d'unicité ou d'état violée (409). */
public class ConflictException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message);
    }
}
