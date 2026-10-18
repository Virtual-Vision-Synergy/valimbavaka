package org.vvs.valimbavaka.core.exception;

import java.io.Serial;

import org.springframework.http.HttpStatus;

/** Authentifié mais permission insuffisante (403). */
public class ForbiddenException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
