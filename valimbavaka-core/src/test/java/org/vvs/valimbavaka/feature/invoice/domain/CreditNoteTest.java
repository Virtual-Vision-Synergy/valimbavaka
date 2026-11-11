package org.vvs.valimbavaka.feature.invoice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.shared.operation.OperationStatus;

/** FAC-04: reversal of an invoice and the frozen copy it leaves in the credit note. */
class CreditNoteTest {

    private static final UUID SALE_ID = UUID.randomUUID();
    private static final UUID SOAP = UUID.randomUUID();
    private static final UUID OIL = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-08T09:30:00Z");

    @Test
    void reverse_validatedInvoice_becomesReversed() {
        // Arrange
        var sut = invoice();

        // Act
        sut.reverse();

        // Assert
        assertThat(sut.getStatus()).isEqualTo(OperationStatus.REVERSED);
    }

    @Test
    void reverse_alreadyReversedInvoice_throwsConflict() {
        // Arrange
        var sut = invoice();
        sut.reverse();

        // Act + Assert
        assertThatThrownBy(sut::reverse).isInstanceOf(ConflictException.class).hasMessageContaining("REVERSED");
    }

    @Test
    void issue_invoiceWithTwoLines_copiesTheIssuerTheAmountsAndTheLinesInOrder() {
        // Arrange
        var invoice = invoice();

        // Act
        var sut = CreditNote.issue("AV-2026-0007", invoice, "Produit défectueux", NOW);

        // Assert
        assertThat(sut.getNumber()).isEqualTo("AV-2026-0007");
        assertThat(sut.getReason()).isEqualTo("Produit défectueux");
        assertThat(sut.getInvoiceNumber()).isEqualTo("FAC-2026-0042");
        assertThat(sut.getSaleId()).isEqualTo(SALE_ID);
        assertThat(sut.getSaleNumber()).isEqualTo("VTE-0042");
        assertThat(sut.getIssuedAt()).isEqualTo(NOW);
        assertThat(sut.getBusinessDate()).isEqualTo(LocalDate.of(2026, 11, 8));
        assertThat(sut.getIssuerName()).isEqualTo("By Valimbavaka Cosmetics");
        assertThat(sut.getIssuerNif()).isEqualTo("4001234567");
        assertThat(sut.getSubtotal()).isEqualByComparingTo("29000");
        assertThat(sut.getDiscountAmount()).isEqualByComparingTo("1500");
        assertThat(sut.getTotal()).isEqualByComparingTo("27500");
        assertThat(sut.getLines()).extracting(InvoiceLine::getLabel).containsExactly("Savon coco", "Huile de ricin");
        assertThat(sut.getLines().getFirst().getQuantity()).isEqualByComparingTo("2");
        assertThat(sut.getLines().getFirst().getLineTotal()).isEqualByComparingTo("17000");
    }

    @Test
    void getLines_afterIssue_isReadOnly() {
        // Arrange
        var sut = CreditNote.issue("AV-2026-0008", invoice(), "Erreur de caisse", NOW);

        // Act + Assert
        assertThatThrownBy(() -> sut.getLines().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    private static Invoice invoice() {
        var lines = List.of(
                new InvoiceLineDraft(SOAP, "Savon coco", new BigDecimal("2.000"), amount("8500"), amount("17000")),
                new InvoiceLineDraft(OIL, "Huile de ricin", BigDecimal.ONE, amount("12000"), amount("12000")));
        var draft = new InvoiceDraft(
                SALE_ID,
                "VTE-0042",
                Instant.parse("2026-11-08T07:00:00Z"),
                LocalDate.of(2026, 11, 8),
                "Boutique",
                UUID.randomUUID(),
                "Miora Rakoto",
                "MVOLA",
                "By Valimbavaka Cosmetics",
                "4001234567",
                amount("29000"),
                amount("1500"),
                amount("27500"),
                lines);
        return Invoice.issue("FAC-2026-0042", draft);
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value).setScale(2);
    }
}
