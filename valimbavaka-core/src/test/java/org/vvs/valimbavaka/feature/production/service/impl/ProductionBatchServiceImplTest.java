package org.vvs.valimbavaka.feature.production.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.BASE_ID;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.OIL_ID;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.finishedProduct;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.request;

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
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatch;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatchDetails;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatchStatus;
import org.vvs.valimbavaka.feature.production.domain.ProductionInput;
import org.vvs.valimbavaka.feature.production.mapper.ProductionMapper;
import org.vvs.valimbavaka.feature.production.repository.ProductionBatchRepository;

@ExtendWith(MockitoExtension.class)
class ProductionBatchServiceImplTest {

    @Mock
    private ProductionBatchRepository productionBatchRepository;

    @Mock
    private ProductionMapper productionMapper;

    @Mock
    private ProductionBatchPolicy policy;

    @Mock
    private JournalService journalService;

    @Mock
    private ProductionCompletion completion;

    private ProductionBatchServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new ProductionBatchServiceImpl(
                productionBatchRepository, productionMapper, policy, journalService, completion);
    }

    @Test
    void createBatch_validRequest_savesAPlannedBatchNumberedFromTheSequence() {
        // Arrange
        var request = request(OIL_ID, BASE_ID);
        when(policy.check(request)).thenReturn(finishedProduct());
        when(productionBatchRepository.nextNumber()).thenReturn(7L);
        when(productionBatchRepository.save(any(ProductionBatch.class))).thenAnswer(call -> call.getArgument(0));

        // Act
        sut.createBatch(request);

        // Assert
        var saved = ArgumentCaptor.forClass(ProductionBatch.class);
        verify(productionBatchRepository).save(saved.capture());
        assertThat(saved.getValue().getNumber()).isEqualTo("LOT-00007");
        assertThat(saved.getValue().getStatus()).isEqualTo(ProductionBatchStatus.PLANNED);
        assertThat(saved.getValue().getInputs()).hasSize(2);
    }

    @Test
    void createBatch_validRequest_tracesTheCreationInTheJournal() {
        // Arrange
        var request = request(OIL_ID, BASE_ID);
        when(policy.check(request)).thenReturn(finishedProduct());
        when(productionBatchRepository.nextNumber()).thenReturn(12L);
        when(productionBatchRepository.save(any(ProductionBatch.class))).thenAnswer(call -> call.getArgument(0));

        // Act
        sut.createBatch(request);

        // Assert
        var description = ArgumentCaptor.forClass(String.class);
        verify(journalService)
                .record(eq(JournalAction.CREATE), eq("PRODUCTION_BATCH"), any(), description.capture());
        assertThat(description.getValue()).isEqualTo("Création du lot LOT-00012 : 30 × ART-0031 · Savon noir 100 g "
                + "(2 entrant(s))");
    }

    @Test
    void createBatch_rejectedByThePolicy_savesNothingAndTracesNothing() {
        // Arrange
        var request = request(OIL_ID);
        when(policy.check(request)).thenThrow(new BusinessException(ProductionBatchPolicy.NOT_A_SELLER));

        // Act + Assert
        assertThatThrownBy(() -> sut.createBatch(request)).isInstanceOf(BusinessException.class);
        verify(productionBatchRepository, never()).save(any());
        verify(productionBatchRepository, never()).nextNumber();
        verifyNoInteractions(journalService);
    }

    @Test
    void getBatchById_unknownBatch_throwsNotFound() {
        // Arrange
        var id = UUID.randomUUID();
        when(productionBatchRepository.findById(id)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.getBatchById(id)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void completeBatch_plannedBatch_movesTheStockThenFlushesAndTracesTheValidation() {
        // Arrange
        var batch = plannedBatch();
        var id = UUID.randomUUID();
        when(productionBatchRepository.findByIdForUpdate(id)).thenReturn(Optional.of(batch));
        when(productionBatchRepository.saveAndFlush(batch)).thenReturn(batch);

        // Act
        sut.completeBatch(id);

        // Assert
        var order = inOrder(completion, productionBatchRepository, journalService);
        order.verify(completion).complete(batch);
        order.verify(productionBatchRepository).saveAndFlush(batch);
        var description = ArgumentCaptor.forClass(String.class);
        order.verify(journalService)
                .record(eq(JournalAction.UPDATE), eq("PRODUCTION_BATCH"), any(), description.capture());
        assertThat(description.getValue())
                .isEqualTo("Validation du lot LOT-00003 : 1 entrant(s) sortis, 30 unité(s) entrées à l'entrepôt");
    }

    @Test
    void completeBatch_unknownBatch_throwsNotFoundAndMovesNothing() {
        // Arrange
        var id = UUID.randomUUID();
        when(productionBatchRepository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.completeBatch(id)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(completion, journalService);
    }

    @Test
    void completeBatch_refusedMovement_tracesNothing() {
        // Arrange
        var batch = plannedBatch();
        var id = UUID.randomUUID();
        when(productionBatchRepository.findByIdForUpdate(id)).thenReturn(Optional.of(batch));
        doThrow(new ConflictException("Stock insuffisant")).when(completion).complete(batch);

        // Act + Assert
        assertThatThrownBy(() -> sut.completeBatch(id)).isInstanceOf(ConflictException.class);
        verify(productionBatchRepository, never()).saveAndFlush(any());
        verifyNoInteractions(journalService);
    }

    @Test
    void createCompletedBatch_validRequest_plansThenCompletesTheSameBatch() {
        // Arrange
        var request = request(OIL_ID, BASE_ID);
        when(policy.check(request)).thenReturn(finishedProduct());
        when(productionBatchRepository.nextNumber()).thenReturn(9L);
        when(productionBatchRepository.save(any(ProductionBatch.class))).thenAnswer(call -> call.getArgument(0));
        when(productionBatchRepository.saveAndFlush(any(ProductionBatch.class)))
                .thenAnswer(call -> call.getArgument(0));

        // Act
        sut.createCompletedBatch(request);

        // Assert
        var saved = ArgumentCaptor.forClass(ProductionBatch.class);
        verify(productionBatchRepository).save(saved.capture());
        verify(completion).complete(saved.getValue());
        verify(journalService).record(eq(JournalAction.CREATE), eq("PRODUCTION_BATCH"), any(), any());
        verify(journalService).record(eq(JournalAction.UPDATE), eq("PRODUCTION_BATCH"), any(), any());
    }

    private static ProductionBatch plannedBatch() {
        var oil = new ProductionInput(OIL_ID, new BigDecimal("0.45"));
        var quantity = new BigDecimal("30.000");
        var details = new ProductionBatchDetails(UUID.randomUUID(), quantity, UUID.randomUUID(), List.of(oil));
        return ProductionBatch.plan("LOT-00003", details);
    }
}
