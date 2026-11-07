package org.vvs.valimbavaka.feature.salonservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.article.service.ArticleService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.salonservice.domain.ConsumedProduct;
import org.vvs.valimbavaka.feature.salonservice.domain.SalonService;
import org.vvs.valimbavaka.feature.salonservice.domain.SalonServiceDetails;
import org.vvs.valimbavaka.feature.salonservice.dto.ConsumedProductWDto;
import org.vvs.valimbavaka.feature.salonservice.dto.SalonServiceWDto;
import org.vvs.valimbavaka.feature.salonservice.mapper.SalonServiceMapper;
import org.vvs.valimbavaka.feature.salonservice.repository.SalonServiceRepository;

@ExtendWith(MockitoExtension.class)
class SalonCatalogServiceImplTest {

    private static final UUID SERVICE_ID = UUID.randomUUID();
    private static final UUID COCONUT_OIL = UUID.randomUUID();
    private static final UUID CLAY_MASK = UUID.randomUUID();

    @Mock
    private SalonServiceRepository serviceRepository;

    @Mock
    private SalonServiceMapper serviceMapper;

    @Mock
    private ArticleService articleService;

    @Mock
    private JournalService journalService;

    private SalonCatalogServiceImpl sut;

    @BeforeEach
    void setUp() {
        var checker = new ConsumedProductChecker(articleService);
        sut = new SalonCatalogServiceImpl(serviceRepository, serviceMapper, checker, journalService);
    }

    @Test
    void createService_validRequest_savesNormalizedActiveServiceAndTracesInJournal() {
        // Arrange
        when(articleService.isActiveArticle(COCONUT_OIL)).thenReturn(true);
        when(serviceRepository.existsByNameIgnoreCase("Soin visage éclat")).thenReturn(false);
        when(serviceRepository.save(any(SalonService.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var products = List.of(new ConsumedProductWDto(COCONUT_OIL, new BigDecimal("15")));

        // Act
        sut.createService(request("  Soin visage éclat ", "12.5", products, null));

        // Assert
        var saved = ArgumentCaptor.forClass(SalonService.class);
        verify(serviceRepository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Soin visage éclat");
        assertThat(saved.getValue().getDurationMinutes()).isEqualTo(45);
        assertThat(saved.getValue().getCommissionRate()).isEqualByComparingTo("12.50");
        var expected = new ConsumedProduct(COCONUT_OIL, new BigDecimal("15"));
        assertThat(saved.getValue().getProducts()).containsExactly(expected);
        assertThat(saved.getValue().isActive()).isTrue();
        verify(journalService)
                .record(
                        eq(JournalAction.CREATE),
                        eq("SALON_SERVICE"),
                        any(),
                        eq("Création du service salon Soin visage éclat (35000.00 Ar)"));
    }

    @Test
    void createService_withoutProductsNorRate_savesEmptyRecipe() {
        // Arrange
        when(serviceRepository.save(any(SalonService.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        sut.createService(request("Épilation sourcils", null, null, null));

        // Assert
        var saved = ArgumentCaptor.forClass(SalonService.class);
        verify(serviceRepository).save(saved.capture());
        assertThat(saved.getValue().getProducts()).isEmpty();
        assertThat(saved.getValue().getCommissionRate()).isNull();
    }

    @Test
    void createService_nameAlreadyUsed_throwsConflictWithoutSaving() {
        // Arrange
        when(serviceRepository.existsByNameIgnoreCase("Soin visage")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> sut.createService(request("Soin visage", null, null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("nom");
        verify(serviceRepository, never()).save(any());
        verify(journalService, never()).record(any(), anyString(), any(), anyString());
    }

    @Test
    void createService_sameArticleListedTwice_throwsBusinessException() {
        // Arrange
        when(articleService.isActiveArticle(COCONUT_OIL)).thenReturn(true);
        var products = List.of(line(COCONUT_OIL, "15"), line(COCONUT_OIL, "10"));

        // Act + Assert
        assertThatThrownBy(() -> sut.createService(request("Soin visage", null, products, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("qu'une fois");
        verify(serviceRepository, never()).save(any());
    }

    @Test
    void createService_unknownOrInactiveArticle_throwsBusinessException() {
        // Arrange
        when(articleService.isActiveArticle(CLAY_MASK)).thenReturn(false);
        var products = List.of(line(CLAY_MASK, "1"));

        // Act + Assert
        assertThatThrownBy(() -> sut.createService(request("Soin visage", null, products, null)))
                .isInstanceOf(BusinessException.class)
                .isNotInstanceOf(ConflictException.class)
                .hasMessageContaining("Article introuvable ou inactif");
        verify(serviceRepository, never()).save(any());
    }

    @Test
    void updateService_staleVersion_throwsConflictWithoutChange() {
        // Arrange
        var service = existing(List.of());
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));

        // Act + Assert
        assertThatThrownBy(() -> sut.updateService(SERVICE_ID, request("Autre nom", null, null, 7L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("modifié entre-temps");
        assertThat(service.getName()).isEqualTo("Soin visage");
    }

    @Test
    void updateService_articleDeactivatedSinceButAlreadyListed_isAccepted() {
        // Arrange
        var service = existing(List.of(new ConsumedProduct(CLAY_MASK, BigDecimal.ONE)));
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(serviceRepository.existsByNameIgnoreCaseAndIdNot("Soin visage", SERVICE_ID)).thenReturn(false);

        // Act
        sut.updateService(SERVICE_ID, request("Soin visage", "8", List.of(line(CLAY_MASK, "2")), 0L));

        // Assert
        verify(articleService, never()).isActiveArticle(CLAY_MASK);
        assertThat(service.getCommissionRate()).isEqualByComparingTo("8.00");
        verify(journalService)
                .record(eq(JournalAction.UPDATE), eq("SALON_SERVICE"), eq(SERVICE_ID), startsWith("Modification"));
    }

    @Test
    void updateService_nothingChanged_writesNoJournalLine() {
        // Arrange
        var service = existing(List.of());
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));
        when(serviceRepository.existsByNameIgnoreCaseAndIdNot("Soin visage", SERVICE_ID)).thenReturn(false);

        // Act
        sut.updateService(SERVICE_ID, request(" Soin visage ", null, List.of(), null));

        // Assert
        verify(journalService, never()).record(any(), anyString(), any(), anyString());
    }

    @Test
    void updateService_unknownId_throwsNotFound() {
        // Arrange
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.updateService(SERVICE_ID, request("Soin visage", null, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void setActive_deactivateThenDeactivateAgain_tracesOnce() {
        // Arrange
        var service = existing(List.of());
        when(serviceRepository.findById(SERVICE_ID)).thenReturn(Optional.of(service));

        // Act
        sut.setActive(SERVICE_ID, false);
        sut.setActive(SERVICE_ID, false);

        // Assert
        assertThat(service.isActive()).isFalse();
        verify(journalService)
                .record(
                        JournalAction.UPDATE,
                        "SALON_SERVICE",
                        SERVICE_ID,
                        "Désactivation du service salon Soin visage (35000.00 Ar)");
    }

    private static SalonService existing(List<ConsumedProduct> products) {
        return SalonService.create(
                new SalonServiceDetails("Soin visage", 45, new BigDecimal("35000"), null, products));
    }

    private static ConsumedProductWDto line(UUID articleId, String quantity) {
        return new ConsumedProductWDto(articleId, new BigDecimal(quantity));
    }

    private static SalonServiceWDto request(
            String name, String commissionRate, List<ConsumedProductWDto> products, Long version) {
        var rate = commissionRate == null ? null : new BigDecimal(commissionRate);
        return new SalonServiceWDto(name, 45, new BigDecimal("35000"), rate, products, version);
    }
}
