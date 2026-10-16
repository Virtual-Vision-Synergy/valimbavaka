package org.vvs.valimbavaka.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** Fabrique des réponses d'erreur RFC 9457 avec l'extension {@code code} (convention §7.4). */
public final class ProblemDetails {

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatus status, String code, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        return problem;
    }
}
