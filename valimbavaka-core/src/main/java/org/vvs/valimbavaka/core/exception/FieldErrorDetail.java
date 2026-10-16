package org.vvs.valimbavaka.core.exception;

/** Élément de l'extension {@code errors} d'un ProblemDetail 422. */
public record FieldErrorDetail(String field, String message) {
}
