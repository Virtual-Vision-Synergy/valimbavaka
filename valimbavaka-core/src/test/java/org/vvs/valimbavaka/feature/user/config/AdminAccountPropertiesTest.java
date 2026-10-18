package org.vvs.valimbavaka.feature.user.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AdminAccountPropertiesTest {

    private static final String VALID_PASSWORD = "quinze-caracteres";

    @Test
    void violations_completeConfiguration_returnsNothing() {
        // given
        var sut = new AdminAccountProperties("admin@valimbavaka.mg", VALID_PASSWORD, "Rasoa", "Rakoto");
        // when / then
        assertThat(sut.violations()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
        "NULL, quinze-caracteres, Rasoa, Rakoto, ADMIN_EMAIL",
        "pas-un-email, quinze-caracteres, Rasoa, Rakoto, ADMIN_EMAIL",
        "admin@valimbavaka.mg, quatorze-carac, Rasoa, Rakoto, ADMIN_PASSWORD",
        "admin@valimbavaka.mg, NULL, Rasoa, Rakoto, ADMIN_PASSWORD",
        "admin@valimbavaka.mg, quinze-caracteres, ' ', Rakoto, ADMIN_FIRST_NAME",
        "admin@valimbavaka.mg, quinze-caracteres, Rasoa, NULL, ADMIN_LAST_NAME"
    })
    void violations_invalidValue_namesTheVariable(String email, String password, String firstName,
            String lastName, String expectedVariable) {
        // given
        var sut = new AdminAccountProperties(email, password, firstName, lastName);
        // when / then
        assertThat(sut.violations()).singleElement().asString().startsWith(expectedVariable);
    }

    @Test
    void toString_always_masksPassword() {
        // given
        var sut = new AdminAccountProperties("admin@valimbavaka.mg", VALID_PASSWORD, "Rasoa", "Rakoto");
        // when / then
        assertThat(sut.toString()).doesNotContain(VALID_PASSWORD);
    }
}
