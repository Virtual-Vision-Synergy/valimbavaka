package org.vvs.valimbavaka.shared.dto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** {@code size} au plus égal au maximum de {@link PageSizeLimit#current()} (100, ou plus sur un export). */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxPageSizeValidator.class)
public @interface MaxPageSize {

    String message() default "taille de page trop grande";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
