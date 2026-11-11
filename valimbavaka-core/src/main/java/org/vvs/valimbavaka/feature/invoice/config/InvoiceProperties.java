package org.vvs.valimbavaka.feature.invoice.config;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Issuer printed on the invoices (FAC-01): company name and tax number (NIF, variable {@code INVOICE_ISSUER_NIF}).
 * A blank NIF is read as absent: the invoice then shows « [à compléter] ».
 */
@ConfigurationProperties(prefix = "vvs.invoice")
public record InvoiceProperties(
        @DefaultValue("By Valimbavaka Cosmetics") String issuerName, @Nullable String issuerNif) {

    /** The NIF, or {@code null} while it is not configured. */
    public @Nullable String issuerNifOrNull() {
        return issuerNif == null || issuerNif.isBlank() ? null : issuerNif.strip();
    }
}
