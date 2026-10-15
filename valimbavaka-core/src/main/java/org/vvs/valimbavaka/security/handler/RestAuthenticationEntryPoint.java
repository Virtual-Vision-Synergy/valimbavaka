package org.vvs.valimbavaka.security.handler;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.vvs.valimbavaka.core.exception.ProblemDetails;

/** 401 au format ProblemDetail pour toute requête non authentifiée. */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailWriter problemDetailWriter;

    public RestAuthenticationEntryPoint(ProblemDetailWriter problemDetailWriter) {
        this.problemDetailWriter = problemDetailWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        problemDetailWriter.write(request, response,
                ProblemDetails.of(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentification requise"));
    }
}
