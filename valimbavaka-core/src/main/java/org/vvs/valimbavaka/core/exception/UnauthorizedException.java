package org.vvs.valimbavaka.core.exception;

import java.io.Serial;

import org.springframework.http.HttpStatus;

/** Non authentifié (401). */
public class UnauthorizedException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", message);
    }
}
