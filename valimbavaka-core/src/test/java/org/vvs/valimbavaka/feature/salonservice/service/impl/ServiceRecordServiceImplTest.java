package org.vvs.valimbavaka.feature.salonservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.core.exception.UnauthorizedException;
import org.vvs.valimbavaka.feature.commission.domain.CommissionSource;
import org.vvs.valimbavaka.feature.commission.domain.CommissionSourceType;
import org.vvs.valimbavaka.feature.commission.service.CommissionService;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.salonservice.domain.ServiceRecord;
import org.vvs.valimbavaka.feature.salonservice.domain.ServiceRecordDraft;
import org.vvs.valimbavaka.feature.salonservice.domain.ServiceRecordDraft.Place;
import org.vvs.valimbavaka.feature.salonservice.domain.ServiceRecordDraft.Seller;
import org.vvs.valimbavaka.feature.salonservice.domain.ServiceRecordDraft.ServiceCopy;
import org.vvs.valimbavaka.feature.salonservice.domain.ServiceRecordProduct;
import org.vvs.valimbavaka.feature.salonservice.dto.ServiceRecordWDto;
import org.vvs.valimbavaka.feature.salonservice.mapper.ServiceRecordMapper;
import org.vvs.valimbavaka.feature.salonservice.repository.ServiceRecordRepository;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementCommand;
import org.vvs.valimbavaka.feature.stock.domain.StockMovementType;
import org.vvs.valimbavaka.feature.stock.service.StockMovementService;
import org.vvs.valimbavaka.shared.dto.PageQuery;

@ExtendWith(MockitoExtension.class)
class ServiceRecordServiceImplTest {

    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID SERVICE_ID = UUID.randomUUID();
    private static final UUID SALON_ID = UUID.randomUUID();
    private static final UUID OIL_ID = UUID.randomUUID();
    private static final UUID MASK_ID = UUID.randomUUID();
    private static final UUID SELLER_ID = UUID.randomUUID();

    /** 22:30 UTC on 8 November = 01:30 on 9 November in Madagascar (UTC+3). */
    private static final Clock LATE_EVENING_UTC = Clock.fixed(Instant.parse("2026-11-08T22:30:00Z"), ZoneOffset.UTC);

    @Mock
    private ServiceRecordRepository serviceRecordRepository;

    @Mock
    private ServiceRecordMapper serviceRecordMapper;

    @Mock
    private ServiceRecordPolicy policy;

    @Mock
    private StockMovementService stockMovementService;

    @Mock
    private JournalService journalService;

    @Mock
    private AuditorAware<UUID> auditorAware;

    @Mock
    private CommissionService commissionService;

    private ServiceRecordServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new ServiceRecordServiceImpl(
                serviceRecordRepository,
                serviceRecordMapper,
                policy,
                stockMovementService,
                journalService,
                auditorAware,
                LATE_EVENING_UTC,
                commissionService);
    }

    @Test
    void recordService_withProducts_recordsOneNegativeSalonMovementPerProductAndJournal() {
        // Arrange
        var request = request();
        when(policy.draft(request)).thenReturn(draft(oil(), mask()));
        when(serviceRecordRepository.nextNumber()).thenReturn(42L);
        when(serviceRecordRepository.save(any(ServiceRecord.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));

        // Act
        sut.recordService(request);

        // Assert
        var commands = ArgumentCaptor.forClass(StockMovementCommand.class);
        verify(stockMovementService, times(2)).record(commands.capture());
        var legs = commands.getAllValues().stream()
                .map(command -> command.articleId() + " " + command.quantity().toPlainString())
                .toList();
        assertThat(legs).containsExactly(OIL_ID + " -0.015", MASK_ID + " -1");
        assertThat(commands.getAllValues()).allSatisfy(command -> {
            assertThat(command.type()).isEqualTo(StockMovementType.TRADE_SHOW);
            assertThat(command.locationId()).isEqualTo(SALON_ID);
            assertThat(command.referenceId()).isEqualTo(RECORD_ID);
        });
        verify(journalService)
                .record(eq(JournalAction.CREATE), eq("SERVICE_RECORD"), eq(RECORD_ID), contains("PRE-0042"));
    }

    @Test
    void recordService_withoutProducts_recordsNoMovementButTracesJournal() {
        // Arrange
        var request = request();
        when(policy.draft(request)).thenReturn(draft());
        when(serviceRecordRepository.nextNumber()).thenReturn(7L);
        when(serviceRecordRepository.save(any(ServiceRecord.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));

        // Act
        sut.recordService(request);

        // Assert
        verifyNoInteractions(stockMovementService);
        verify(journalService)
                .record(eq(JournalAction.CREATE), eq("SERVICE_RECORD"), eq(RECORD_ID), contains("0 produit"));
        var mapped = ArgumentCaptor.forClass(ServiceRecord.class);
        verify(serviceRecordMapper).toRDto(mapped.capture());
        assertThat(mapped.getValue().getNumber()).isEqualTo("PRE-0007");
        assertThat(mapped.getValue().getPrice()).isEqualByComparingTo("35000");
    }

    @Test
    void recordService_validRecord_creditsTheCommissionOnThePriceOfTheServiceAndTheBusinessDay() {
        // Arrange : 22:30 UTC le 8 = 01:30 le 9 novembre à Antananarivo
        var request = request();
        var draft = draft();
        when(policy.draft(request)).thenReturn(draft);
        when(serviceRecordRepository.nextNumber()).thenReturn(42L);
        when(serviceRecordRepository.save(any(ServiceRecord.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));

        // Act
        sut.recordService(request);

        // Assert
        var source = ArgumentCaptor.forClass(CommissionSource.class);
        verify(commissionService).calculate(source.capture());
        assertThat(source.getValue().type()).isEqualTo(CommissionSourceType.SERVICE_RECORD);
        assertThat(source.getValue().id()).isEqualTo(RECORD_ID);
        assertThat(source.getValue().number()).isEqualTo("PRE-0042");
        assertThat(source.getValue().sellerId()).isEqualTo(draft.seller().id());
        assertThat(source.getValue().salesChannelId()).isEqualTo(draft.place().salesChannelId());
        assertThat(source.getValue().salonServiceId()).isEqualTo(SERVICE_ID);
        assertThat(source.getValue().businessDate()).isEqualTo(LocalDate.of(2026, 11, 9));
        assertThat(source.getValue().revenue()).isEqualByComparingTo("35000");
        assertThat(source.getValue().purchaseCost()).isNull();
    }

    @Test
    void recordService_insufficientSalonStock_propagatesConflictWithoutJournal() {
        // Arrange
        var request = request();
        when(policy.draft(request)).thenReturn(draft(oil()));
        when(serviceRecordRepository.nextNumber()).thenReturn(1L);
        when(serviceRecordRepository.save(any(ServiceRecord.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(stockMovementService.record(any(StockMovementCommand.class)))
                .thenThrow(new ConflictException("Stock insuffisant à cet emplacement : 0 disponible."));

        // Act + Assert
        assertThatThrownBy(() -> sut.recordService(request)).isInstanceOf(ConflictException.class);
        verify(journalService, never()).record(any(), anyString(), any(), anyString());
    }

    @Test
    void getServiceRecordById_unknownId_throwsNotFoundException() {
        // Arrange
        when(serviceRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.getServiceRecordById(RECORD_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getMyServiceRecords_withoutDate_readsCurrentSellerOnTodayInMadagascar() {
        // Arrange
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(SELLER_ID));
        when(serviceRecordRepository.findBySellerBetween(eq(SELLER_ID), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        var page = sut.getMyServiceRecords(firstPage(), null);

        // Assert
        assertThat(page.items()).isEmpty();
        verify(serviceRecordRepository)
                .findBySellerBetween(
                        eq(SELLER_ID),
                        eq(Instant.parse("2026-11-08T21:00:00Z")),
                        eq(Instant.parse("2026-11-09T21:00:00Z")),
                        any(Pageable.class));
    }

    @Test
    void getMyServiceRecords_withDate_readsThatBusinessDayMostRecentFirst() {
        // Arrange
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(SELLER_ID));
        when(serviceRecordRepository.findBySellerBetween(eq(SELLER_ID), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        sut.getMyServiceRecords(firstPage(), LocalDate.parse("2026-11-01"));

        // Assert
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(serviceRecordRepository)
                .findBySellerBetween(
                        eq(SELLER_ID),
                        eq(Instant.parse("2026-10-31T21:00:00Z")),
                        eq(Instant.parse("2026-11-01T21:00:00Z")),
                        pageable.capture());
        assertThat(pageable.getValue().getSort().toString()).isEqualTo("createdAt: DESC");
    }

    @Test
    void getMyServiceRecords_withoutSession_throwsUnauthorizedException() {
        // Arrange
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.getMyServiceRecords(firstPage(), null)).isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(serviceRecordRepository);
    }

    private static PageQuery firstPage() {
        return new PageQuery(1, 20, null, null, null);
    }

    private static ServiceRecordWDto request() {
        return new ServiceRecordWDto(SERVICE_ID, "Hanta", null, null);
    }

    private static ServiceRecordProduct oil() {
        return ServiceRecordProduct.of(OIL_ID, "ART-0001", "Huile de coco", new BigDecimal("0.015"));
    }

    private static ServiceRecordProduct mask() {
        return ServiceRecordProduct.of(MASK_ID, "ART-0002", "Masque argile", BigDecimal.ONE);
    }

    private static ServiceRecordDraft draft(ServiceRecordProduct... products) {
        return new ServiceRecordDraft(
                SERVICE_ID,
                new ServiceCopy("Soin visage", 45, new BigDecimal("35000")),
                new Seller(UUID.randomUUID(), "Miora Andria"),
                new Place(UUID.randomUUID(), SALON_ID),
                "Hanta",
                null,
                List.of(products));
    }

    /** The id is set by Hibernate on persist; the mocked repository does it by hand. */
    private static ServiceRecord withId(ServiceRecord serviceRecord) {
        ReflectionTestUtils.setField(serviceRecord, "id", RECORD_ID);
        return serviceRecord;
    }
}
