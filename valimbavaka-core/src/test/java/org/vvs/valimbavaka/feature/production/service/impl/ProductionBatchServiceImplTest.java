package org.vvs.valimbavaka.feature.production.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.BASE_ID;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.OIL_ID;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.finishedProduct;
import static org.vvs.valimbavaka.feature.production.service.impl.ProductionTestData.request;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatch;
import org.vvs.valimbavaka.feature.production.domain.ProductionBatchStatus;
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

    private ProductionBatchServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new ProductionBatchServiceImpl(productionBatchRepository, productionMapper, policy, journalService);
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
}
