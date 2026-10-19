package org.vvs.valimbavaka.shared.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class MaxPageSizeValidatorTest {

    private static ValidatorFactory factory;
    private static Validator sut;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        sut = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void validate_standardRequestAbove100_isRejected() {
        // Act
        var violations = sut.validate(new PageQuery(1, 101, null, null, null));

        // Assert
        assertThat(violations)
                .singleElement()
                .satisfies(v -> assertThat(v.getPropertyPath()).hasToString("size"))
                .satisfies(v -> assertThat(v.getMessage()).isEqualTo("doit être inférieur ou égal à 100"));
    }

    @Test
    void constructAndValidate_raisedLimit_defaultsToAndAcceptsTheRaisedMaximum() {
        // Arrange
        raiseLimitTo(10_000);

        // Act
        var byDefault = new PageQuery(null, null, null, null, null);
        var atMaximum = new PageQuery(1, 10_000, null, null, null);

        // Assert
        assertThat(byDefault.size()).isEqualTo(10_000);
        assertThat(sut.validate(atMaximum)).isEmpty();
    }

    @Test
    void validate_raisedLimitExceeded_isRejected() {
        // Arrange
        raiseLimitTo(10_000);

        // Act
        var violations = sut.validate(new PageQuery(1, 10_001, null, null, null));

        // Assert
        assertThat(violations)
                .singleElement()
                .satisfies(v -> assertThat(v.getMessage()).isEqualTo("doit être inférieur ou égal à 10000"));
    }

    private static void raiseLimitTo(int max) {
        var request = new MockHttpServletRequest();
        request.setAttribute(PageSizeLimit.REQUEST_ATTRIBUTE, new PageSizeLimit(max, max));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
