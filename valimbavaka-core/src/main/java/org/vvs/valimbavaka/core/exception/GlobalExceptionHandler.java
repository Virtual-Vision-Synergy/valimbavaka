package org.vvs.valimbavaka.core.exception;

import java.sql.SQLException;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Point unique de traduction des exceptions en réponses RFC 9457 (convention §7.4) :
 * 400 requête illisible, 401, 403, 404, 409, 422 validation/règle métier, 500 générique.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String CODE = "code";
    private static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    private static final String CONFLICT = "CONFLICT";
    /** SQLSTATE PostgreSQL d'une violation de contrainte d'unicité. */
    private static final String UNIQUE_VIOLATION = "23505";

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex) {
        return ProblemDetails.of(ex.getStatus(), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        return ProblemDetails.of(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentification requise");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return ProblemDetails.of(HttpStatus.FORBIDDEN, "FORBIDDEN", "Accès refusé");
    }

    /** Modification concurrente détectée par le verrou optimiste ({@code @Version}) : 409, à recharger. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException ex) {
        return ProblemDetails.of(
                HttpStatus.CONFLICT,
                CONFLICT,
                "La ressource a été modifiée entre-temps : rechargez-la puis recommencez.");
    }

    /**
     * Doublon refusé par la base (deux créations simultanées passées toutes deux par le contrôle applicatif) : 409.
     * Toute autre violation de contrainte reste une erreur interne.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        if (isUniqueViolation(ex)) {
            return ProblemDetails.of(HttpStatus.CONFLICT, CONFLICT, "Cette valeur est déjà utilisée.");
        }
        return handleUnexpected(ex);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Erreur non prévue", ex);
        return ProblemDetails.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Erreur interne");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), String.valueOf(error.getDefaultMessage())))
                .toList();
        return validationFailed(errors);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorDetail> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldErrorDetail(
                                String.valueOf(result.getMethodParameter().getParameterName()),
                                String.valueOf(error.getDefaultMessage()))))
                .toList();
        return validationFailed(errors);
    }

    /** Ajoute le code machine aux ProblemDetail produits par Spring MVC (400 requête mal formée, 405…). */
    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem && problem.getProperties() == null) {
            problem.setProperty(
                    CODE,
                    statusCode.value() == HttpStatus.BAD_REQUEST.value()
                            ? "MALFORMED_REQUEST"
                            : HttpStatus.valueOf(statusCode.value()).name());
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private static boolean isUniqueViolation(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private static ResponseEntity<Object> validationFailed(List<FieldErrorDetail> errors) {
        var problem = ProblemDetails.of(
                HttpStatus.UNPROCESSABLE_CONTENT, VALIDATION_FAILED, errors.size() + " champ(s) invalide(s)");
        problem.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(problem);
    }
}
