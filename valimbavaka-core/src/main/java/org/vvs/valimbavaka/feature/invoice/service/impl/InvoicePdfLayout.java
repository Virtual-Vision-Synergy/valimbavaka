package org.vvs.valimbavaka.feature.invoice.service.impl;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import org.vvs.valimbavaka.feature.invoice.dto.InvoiceRDto;
import org.vvs.valimbavaka.shared.excel.config.ExcelProperties;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

/** Texts printed around the table of lines of an invoice PDF, in French (dates {@code jj/mm/aaaa}, amounts in Ar). */
@Component
class InvoicePdfLayout {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String NOT_CONFIGURED = "[à compléter]";
    private static final Map<String, String> PAYMENT_LABELS = Map.of(
            "CASH", "Espèces",
            "MVOLA", "MVola",
            "ORANGE_MONEY", "Orange Money",
            "CARD", "Carte bancaire",
            "ON_DELIVERY", "À la livraison");

    private final ZoneId zone;

    InvoicePdfLayout(ExcelProperties properties) {
        this.zone = properties.timeZone();
    }

    String title(InvoiceRDto invoice) {
        return "Facture " + invoice.number();
    }

    List<String> details(InvoiceRDto invoice) {
        var lines = new ArrayList<String>();
        var nif = invoice.issuerNif() == null ? NOT_CONFIGURED : invoice.issuerNif();
        lines.add("Émetteur : %s · NIF : %s".formatted(invoice.issuerName(), nif));
        var issuedAt = DATE_TIME.format(invoice.issuedAt().atZone(zone));
        var businessDate = DATE.format(invoice.businessDate());
        lines.add("Émise le %s · journée du %s · vente %s".formatted(issuedAt, businessDate, invoice.saleNumber()));
        var payment = PAYMENT_LABELS.getOrDefault(invoice.paymentMethod(), invoice.paymentMethod());
        var seller = orDash(invoice.sellerName());
        var channel = orDash(invoice.salesChannelLabel());
        lines.add("Vendeuse : %s · canal : %s · règlement : %s".formatted(seller, channel, payment));
        if (invoice.status() != OperationStatus.VALIDATED) {
            var status = invoice.status() == OperationStatus.CANCELLED ? "annulée" : "contre-passée";
            lines.add("Statut : " + status);
        }
        return lines;
    }

    List<String> summary(InvoiceRDto invoice) {
        var lines = new ArrayList<String>();
        lines.add("Sous-total : " + amount(invoice.subtotal()));
        if (invoice.discountAmount().signum() != 0) {
            lines.add("Remise : -" + amount(invoice.discountAmount()));
        }
        lines.add("Total à payer : " + amount(invoice.total()));
        return lines;
    }

    private static String orDash(@Nullable String value) {
        return value == null ? "-" : value;
    }

    // One instance per call: DecimalFormat is not thread-safe
    private static String amount(BigDecimal value) {
        var symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.##", symbols).format(value) + " Ar";
    }
}
