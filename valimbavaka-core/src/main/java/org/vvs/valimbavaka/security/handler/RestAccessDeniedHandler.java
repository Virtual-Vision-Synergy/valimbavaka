package org.vvs.valimbavaka.security.handler;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.vvs.valimbavaka.core.exception.ProblemDetails;

/** 403 au format ProblemDetail pour un utilisateur authentifié sans la permission requise. */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ProblemDetailWriter problemDetailWriter;

    public RestAccessDeniedHandler(ProblemDetailWriter problemDetailWriter) {
        this.problemDetailWriter = problemDetailWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        problemDetailWriter.write(request, response,
                ProblemDetails.of(HttpStatus.FORBIDDEN, "FORBIDDEN", "Accès refusé"));
    }
}
