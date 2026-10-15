package org.vvs.valimbavaka.core.exception;

import java.io.Serial;

import org.springframework.http.HttpStatus;

/** Ressource introuvable ou non visible par l'utilisateur (404). */
public class NotFoundException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}
