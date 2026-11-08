package org.vvs.valimbavaka.feature.transfer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.feature.transfer.domain.TransferStatus;
import org.vvs.valimbavaka.feature.transfer.repository.TransferSummary;
import org.vvs.valimbavaka.security.AuthenticatedUser;
import org.vvs.valimbavaka.security.AuthenticatedUserLoader;
import org.vvs.valimbavaka.shared.dto.PageResponse;

@ExtendWith(MockitoExtension.class)
class TransferHistoryTest {

    private static final UUID STORE = UUID.randomUUID();
    private static final UUID WAREHOUSE = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();

    @Mock
    private ReferentialService referentialService;

    @Mock
    private AuthenticatedUserLoader userLoader;

    private TransferHistory sut;

    @BeforeEach
    void setUp() {
        sut = new TransferHistory(referentialService, userLoader);
        var locations = List.of(
                new StockLocationRDto(STORE, "STORE", "Boutique"),
                new StockLocationRDto(WAREHOUSE, "WAREHOUSE", "Entrepôt"));
        when(referentialService.getStockLocations(any())).thenReturn(new PageResponse<>(locations, 1, 100, 2, 1));
    }

    @Test
    void toRDtos_returnBySeller_showsReferenceDateRouteStatusAndIssuer() {
        // Arrange
        when(userLoader.findById(SELLER_ID)).thenReturn(Optional.of(user(SELLER_ID)));
        var row = row("RET-0007", STORE, WAREHOUSE, SELLER_ID);

        // Act
        var page = sut.toRDtos(new PageImpl<>(List.of(row), PageRequest.of(0, 20), 1));

        // Assert
        assertThat(page.totalCount()).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(item -> {
            assertThat(item.number()).isEqualTo("RET-0007");
            assertThat(item.createdAt()).isEqualTo(row.createdAt());
            assertThat(item.fromLocationLabel()).isEqualTo("Boutique");
            assertThat(item.toLocationLabel()).isEqualTo("Entrepôt");
            assertThat(item.status()).isEqualTo(TransferStatus.RETURN);
            assertThat(item.createdBy()).isEqualTo(SELLER_ID);
            assertThat(item.createdByName()).isEqualTo("Tiana Ranaivoson");
            assertThat(item.lineCount()).isEqualTo(2);
            assertThat(item.quantity()).isEqualByComparingTo("5.5");
        });
    }

    @Test
    void toRDtos_transferWithoutIssuer_showsTheSystem() {
        // Act
        var page = sut.toRDtos(new PageImpl<>(List.of(row("TRF-0092", WAREHOUSE, STORE, null))));

        // Assert
        assertThat(page.items().getFirst().createdByName()).isEqualTo(TransferHistory.SYSTEM_ISSUER);
    }

    @Test
    void toRDtos_accountRemovedSince_showsAnUnknownAccount() {
        // Arrange
        when(userLoader.findById(SELLER_ID)).thenReturn(Optional.empty());

        // Act
        var page = sut.toRDtos(new PageImpl<>(List.of(row("RET-0001", STORE, WAREHOUSE, SELLER_ID))));

        // Assert
        assertThat(page.items().getFirst().createdByName()).isEqualTo(TransferHistory.UNKNOWN_ISSUER);
        assertThat(page.items().getFirst().createdBy()).isEqualTo(SELLER_ID);
    }

    @Test
    void toRDtos_sameIssuerOnSeveralRows_loadsTheAccountOnce() {
        // Arrange
        when(userLoader.findById(SELLER_ID)).thenReturn(Optional.of(user(SELLER_ID)));
        var rows = List.of(
                row("RET-0001", STORE, WAREHOUSE, SELLER_ID),
                row("RET-0002", STORE, WAREHOUSE, SELLER_ID),
                row("RET-0003", STORE, WAREHOUSE, SELLER_ID));

        // Act
        var page = sut.toRDtos(new PageImpl<>(rows));

        // Assert
        assertThat(page.items()).extracting(item -> item.createdByName()).containsOnly("Tiana Ranaivoson");
        verify(userLoader, times(1)).findById(SELLER_ID);
    }

    @Test
    void toRDtos_locationDeactivatedSince_keepsTheIdentifierWithoutLabel() {
        // Arrange
        var deactivated = UUID.randomUUID();

        // Act
        var page = sut.toRDtos(new PageImpl<>(List.of(row("TRF-0003", deactivated, STORE, null))));

        // Assert
        var item = page.items().getFirst();
        assertThat(item.fromLocationId()).isEqualTo(deactivated);
        assertThat(item.fromLocationLabel()).isNull();
        assertThat(item.toLocationLabel()).isEqualTo("Boutique");
    }

    private static TransferSummary row(String number, UUID from, UUID to, @Nullable UUID createdBy) {
        var origin = number.startsWith("RET") ? TransferOrigin.RETURN : TransferOrigin.MORNING;
        var status = origin.initialStatus();
        return new TransferSummary(
                UUID.randomUUID(),
                number,
                origin,
                status,
                from,
                to,
                2L,
                new BigDecimal("5.5"),
                Instant.parse("2026-11-08T07:30:00Z"),
                createdBy);
    }

    private static AuthenticatedUser user(UUID id) {
        return new AuthenticatedUser(
                id,
                "tiana@valimbavaka.mg",
                "Tiana",
                "Ranaivoson",
                "SELLER",
                "Vendeuse",
                "hash",
                Set.of(),
                true,
                false,
                null);
    }
}
