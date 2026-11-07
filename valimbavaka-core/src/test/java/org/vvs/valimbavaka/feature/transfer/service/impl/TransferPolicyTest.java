package org.vvs.valimbavaka.feature.transfer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.referential.dto.StockLocationRDto;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.transfer.domain.TransferCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferLineCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.shared.dto.PageResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferPolicyTest {

    private static final UUID STORE = UUID.randomUUID();
    private static final UUID WAREHOUSE = UUID.randomUUID();

    @Mock
    private ReferentialService referentialService;

    @Mock
    private ArticleService articleService;

    private TransferPolicy sut;

    @BeforeEach
    void setUp() {
        sut = new TransferPolicy(referentialService, articleService);
        var locations = List.of(
                new StockLocationRDto(STORE, "STORE", "Boutique"),
                new StockLocationRDto(WAREHOUSE, "WAREHOUSE", "Entrepôt"));
        when(referentialService.getStockLocations(any())).thenReturn(new PageResponse<>(locations, 1, 100, 2, 1));
    }

    @Test
    void check_validReturn_returnsTheRouteOfTheJournal() {
        assertThat(sut.check(command(STORE, WAREHOUSE, line("3")))).isEqualTo("Boutique → Entrepôt");
    }

    @Test
    void check_sameOriginAndDestination_throwsBusinessException() {
        assertThatThrownBy(() -> sut.check(command(STORE, STORE, line("1"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("différentes");
    }

    @Test
    void check_unknownOrInactiveLocation_throwsBusinessException() {
        assertThatThrownBy(() -> sut.check(command(UUID.randomUUID(), WAREHOUSE, line("1"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inconnu ou inactif");
    }

    @Test
    void check_noLine_throwsBusinessException() {
        assertThatThrownBy(() -> sut.check(command(STORE, WAREHOUSE)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("de 1 à 100 lignes");
    }

    @Test
    void check_101Lines_throwsBusinessException() {
        var lines = new ArrayList<TransferLineCommand>();
        for (var i = 0; i <= TransferPolicy.MAX_LINES; i++) {
            lines.add(line("1"));
        }
        var command = new TransferCommand(TransferOrigin.MORNING, STORE, WAREHOUSE, lines, null);

        assertThatThrownBy(() -> sut.check(command)).isInstanceOf(BusinessException.class);
    }

    @Test
    void check_sameArticleTwice_throwsBusinessException() {
        var articleId = UUID.randomUUID();
        var command = command(
                STORE,
                WAREHOUSE,
                new TransferLineCommand(articleId, BigDecimal.ONE),
                new TransferLineCommand(articleId, BigDecimal.TEN));

        assertThatThrownBy(() -> sut.check(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("qu'une fois");
    }

    @Test
    void check_negativeQuantityThroughThePort_throwsBusinessException() {
        assertThatThrownBy(() -> sut.check(command(STORE, WAREHOUSE, line("-1"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("supérieure à zéro");
    }

    @Test
    void check_unknownArticle_propagatesNotFound() {
        var line = line("1");
        when(articleService.getArticleById(line.articleId())).thenThrow(new NotFoundException("Article introuvable"));

        assertThatThrownBy(() -> sut.check(command(STORE, WAREHOUSE, line))).isInstanceOf(NotFoundException.class);
    }

    private static TransferCommand command(UUID from, UUID to, TransferLineCommand... lines) {
        return new TransferCommand(TransferOrigin.RETURN, from, to, List.of(lines), null);
    }

    private static TransferLineCommand line(String quantity) {
        return new TransferLineCommand(UUID.randomUUID(), new BigDecimal(quantity));
    }
}
