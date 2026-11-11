package org.vvs.valimbavaka.feature.invoice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.vvs.valimbavaka.shared.operation.OperationStatus;

class InvoiceTest {

    private static final UUID SALE_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID SOAP = UUID.randomUUID();
    private static final UUID OIL = UUID.randomUUID();

    @Test
    void issue_saleWithTwoLines_copiesTheSnapshotAndKeepsTheLineOrder() {
        // Arrange
        var draft = draft(
                List.of(
                        new InvoiceLineDraft(
                                SOAP, "Savon coco", new BigDecimal("2.000"), new BigDecimal("8500.00"), amount("17000")),
                        new InvoiceLineDraft(
                                OIL, "Huile de ricin", BigDecimal.ONE, new BigDecimal("12000.00"), amount("12000"))),
                amount("29000"),
                amount("1500"),
                amount("27500"));

        // Act
        var sut = Invoice.issue("FAC-2026-0042", draft);

        // Assert
        assertThat(sut.getNumber()).isEqualTo("FAC-2026-0042");
        assertThat(sut.getStatus()).isEqualTo(OperationStatus.VALIDATED);
        assertThat(sut.getSaleId()).isEqualTo(SALE_ID);
        assertThat(sut.getSaleNumber()).isEqualTo("VTE-0042");
        assertThat(sut.getBusinessDate()).isEqualTo(LocalDate.of(2026, 11, 8));
        assertThat(sut.getSalesChannelLabel()).isEqualTo("Boutique");
        assertThat(sut.getSellerId()).isEqualTo(SELLER_ID);
        assertThat(sut.getSellerName()).isEqualTo("Miora Rakoto");
        assertThat(sut.getPaymentMethod()).isEqualTo("MVOLA");
        assertThat(sut.getIssuerName()).isEqualTo("By Valimbavaka Cosmetics");
        assertThat(sut.getIssuerNif()).isNull();
        assertThat(sut.getSubtotal()).isEqualByComparingTo("29000");
        assertThat(sut.getDiscountAmount()).isEqualByComparingTo("1500");
        assertThat(sut.getTotal()).isEqualByComparingTo("27500");
        assertThat(sut.getLines()).extracting(InvoiceLine::getLabel).containsExactly("Savon coco", "Huile de ricin");
        assertThat(sut.getLines().getFirst().getUnitPrice()).isEqualByComparingTo("8500");
        assertThat(sut.getLines().getFirst().getLineTotal()).isEqualByComparingTo("17000");
    }

    @Test
    void issue_saleWithoutSeller_keepsNoSellerOnTheInvoice() {
        // Arrange
        var draft = new InvoiceDraft(
                SALE_ID,
                "VTE-0043",
                Instant.parse("2026-11-08T07:00:00Z"),
                LocalDate.of(2026, 11, 8),
                "Livraison",
                null,
                null,
                "ON_DELIVERY",
                "By Valimbavaka Cosmetics",
                "4001234567",
                amount("8500"),
                amount("0"),
                amount("8500"),
                List.of(new InvoiceLineDraft(SOAP, "Savon coco", BigDecimal.ONE, amount("8500"), amount("8500"))));

        // Act
        var sut = Invoice.issue("FAC-2026-0043", draft);

        // Assert
        assertThat(sut.getSellerId()).isNull();
        assertThat(sut.getSellerName()).isNull();
        assertThat(sut.getIssuerNif()).isEqualTo("4001234567");
    }

    @Test
    void getLines_afterIssue_isReadOnly() {
        // Arrange
        var sut = Invoice.issue(
                "FAC-2026-0044",
                draft(
                        List.of(new InvoiceLineDraft(SOAP, "Savon coco", BigDecimal.ONE, amount("8500"), amount("8500"))),
                        amount("8500"),
                        amount("0"),
                        amount("8500")));

        // Act + Assert
        assertThat(sut.getLines()).hasSize(1);
        assertThatThrownBy(() -> sut.getLines().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static InvoiceDraft draft(
            List<InvoiceLineDraft> lines, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        return new InvoiceDraft(
                SALE_ID,
                "VTE-0042",
                Instant.parse("2026-11-08T07:00:00Z"),
                LocalDate.of(2026, 11, 8),
                "Boutique",
                SELLER_ID,
                "Miora Rakoto",
                "MVOLA",
                "By Valimbavaka Cosmetics",
                null,
                subtotal,
                discount,
                total,
                lines);
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value).setScale(2);
    }
}
