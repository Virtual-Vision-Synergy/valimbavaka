package org.vvs.valimbavaka.core.exception;

import java.io.Serial;

import org.springframework.http.HttpStatus;

/**
 * Racine des exceptions métier (convention §6.6) : règle métier violée → 422.
 * Les sous-classes précisent le statut HTTP et le code machine renvoyés dans le ProblemDetail.
 */
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final HttpStatus status;
    private final String code;

    public BusinessException(String message) {
        this(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED", message);
    }

    protected BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
