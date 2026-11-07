package org.vvs.valimbavaka.feature.transfer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.transfer.domain.Transfer;
import org.vvs.valimbavaka.feature.transfer.domain.TransferCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferLineCommand;
import org.vvs.valimbavaka.feature.transfer.domain.TransferOrigin;
import org.vvs.valimbavaka.feature.transfer.domain.TransferStatus;
import org.vvs.valimbavaka.feature.transfer.dto.StockReturnWDto;
import org.vvs.valimbavaka.feature.transfer.dto.TransferLineWDto;
import org.vvs.valimbavaka.feature.transfer.mapper.TransferMapper;
import org.vvs.valimbavaka.feature.transfer.repository.TransferRepository;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    private static final UUID STORE = UUID.randomUUID();
    private static final UUID WAREHOUSE = UUID.randomUUID();
    private static final UUID ARTICLE_ID = UUID.randomUUID();

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private TransferMapper transferMapper;

    @Mock
    private TransferPolicy policy;

    @Mock
    private TransferLedger ledger;

    @Mock
    private TransferHistory history;

    @Mock
    private TransferSheet sheet;

    private TransferServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new TransferServiceImpl(transferRepository, transferMapper, policy, ledger, history, sheet);
    }

    @Test
    void recordReturn_validRequest_savesAReturnWithItsDedicatedStatusAndMovesTheStock() {
        // Arrange
        var request = new StockReturnWDto(
                STORE, WAREHOUSE, List.of(new TransferLineWDto(ARTICLE_ID, new BigDecimal("4"))), "Invendus");
        when(policy.check(any())).thenReturn("Boutique → Entrepôt");
        when(transferRepository.nextReturnNumber()).thenReturn(15L);
        when(transferRepository.save(any(Transfer.class))).thenAnswer(call -> call.getArgument(0));

        // Act
        sut.recordReturn(request);

        // Assert
        var saved = ArgumentCaptor.forClass(Transfer.class);
        verify(ledger).post(saved.capture(), eq("Boutique → Entrepôt"));
        var transfer = saved.getValue();
        assertThat(transfer.getNumber()).isEqualTo("RET-0015");
        assertThat(transfer.getOrigin()).isEqualTo(TransferOrigin.RETURN);
        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.RETURN);
        assertThat(transfer.getFromLocationId()).isEqualTo(STORE);
        assertThat(transfer.getToLocationId()).isEqualTo(WAREHOUSE);
        assertThat(transfer.getLines()).singleElement().satisfies(line -> {
            assertThat(line.getArticleId()).isEqualTo(ARTICLE_ID);
            assertThat(line.getQuantity()).isEqualByComparingTo("4");
        });
        verify(transferMapper).toRDto(transfer);
        verify(transferRepository, never()).nextTransferNumber();
    }

    @Test
    void record_morningTransfer_isValidatedWithATransferNumber() {
        // Arrange
        var command = new TransferCommand(TransferOrigin.MORNING, WAREHOUSE, STORE, List.of(), null);
        when(policy.check(command)).thenReturn("Entrepôt → Boutique");
        when(transferRepository.nextTransferNumber()).thenReturn(92L);
        when(transferRepository.save(any(Transfer.class))).thenAnswer(call -> call.getArgument(0));

        // Act
        sut.record(command);

        // Assert
        var saved = ArgumentCaptor.forClass(Transfer.class);
        verify(ledger).post(saved.capture(), eq("Entrepôt → Boutique"));
        assertThat(saved.getValue().getNumber()).isEqualTo("TRF-0092");
        assertThat(saved.getValue().getStatus()).isEqualTo(TransferStatus.VALIDATED);
    }

    @Test
    void record_rejectedByThePolicy_savesNothingAndMovesNothing() {
        // Arrange
        var command = new TransferCommand(TransferOrigin.RETURN, STORE, STORE, List.of(), null);
        when(policy.check(command)).thenThrow(new BusinessException("Origine et destination identiques"));

        // Act + Assert
        assertThatThrownBy(() -> sut.record(command)).isInstanceOf(BusinessException.class);
        verify(transferRepository, never()).save(any());
        verifyNoInteractions(ledger);
    }

    @Test
    void getTransfers_noSort_searchesNewestFirstWithTheTrimmedTermAndTheOrigin() {
        // Arrange
        var query = new PageQuery(2, 10, "  ret-00 ", null, null);
        when(transferRepository.search(eq("ret-00"), eq(TransferOrigin.RETURN), any()))
                .thenReturn(Page.empty());

        // Act
        sut.getTransfers(query, TransferOrigin.RETURN);

        // Assert
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(transferRepository).search(eq("ret-00"), eq(TransferOrigin.RETURN), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
        verify(history).toRDtos(any());
    }

    @Test
    void getTransfers_sortOutsideTheWhiteList_fallsBackToTheDate() {
        // Arrange
        var query = new PageQuery(1, 20, null, "note", SortDirection.ASC);
        when(transferRepository.search(eq(""), isNull(), any())).thenReturn(Page.empty());

        // Act
        sut.getTransfers(query, null);

        // Assert
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(transferRepository).search(eq(""), isNull(), pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "createdAt"));
    }

    @Test
    void getTransfers_sortByNumber_isAllowed() {
        // Arrange
        var query = new PageQuery(1, 20, null, "number", SortDirection.ASC);
        when(transferRepository.search(eq(""), isNull(), any())).thenReturn(Page.empty());

        // Act
        sut.getTransfers(query, null);

        // Assert
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(transferRepository).search(eq(""), isNull(), pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "number"));
    }

    @Test
    void getTransfer_existingTransfer_returnsItsSheet() {
        // Arrange
        var transferId = UUID.randomUUID();
        var line = new TransferLineCommand(ARTICLE_ID, BigDecimal.TWO);
        var command = new TransferCommand(TransferOrigin.RETURN, STORE, WAREHOUSE, List.of(line), null);
        var transfer = Transfer.create("RET-0015", command);
        when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));

        // Act
        sut.getTransfer(transferId);

        // Assert
        verify(sheet).toRDto(transfer);
    }

    @Test
    void getTransfer_unknownTransfer_throwsNotFound() {
        // Arrange
        var transferId = UUID.randomUUID();
        when(transferRepository.findById(transferId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.getTransfer(transferId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(transferId.toString());
        verifyNoInteractions(sheet);
    }
}
