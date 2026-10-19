package org.vvs.valimbavaka.shared.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.jspecify.annotations.Nullable;

public class MaxPageSizeValidator implements ConstraintValidator<MaxPageSize, Integer> {

    @Override
    public boolean isValid(@Nullable Integer size, ConstraintValidatorContext context) {
        int max = PageSizeLimit.current().maxSize();
        if (size == null || size <= max) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("doit être inférieur ou égal à " + max)
                .addConstraintViolation();
        return false;
    }
}
