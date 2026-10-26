package org.vvs.valimbavaka.feature.supplier.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.supplier.domain.Supplier;
import org.vvs.valimbavaka.feature.supplier.domain.SupplierDetails;
import org.vvs.valimbavaka.feature.supplier.domain.SupplierType;
import org.vvs.valimbavaka.feature.supplier.dto.SupplierWDto;
import org.vvs.valimbavaka.feature.supplier.mapper.SupplierMapper;
import org.vvs.valimbavaka.feature.supplier.repository.SupplierRepository;

@ExtendWith(MockitoExtension.class)
class SupplierServiceImplTest {

    private static final UUID SUPPLIER_ID = UUID.randomUUID();

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private JournalService journalService;

    private SupplierServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new SupplierServiceImpl(supplierRepository, supplierMapper, journalService);
    }

    @Test
    void createSupplier_newName_savesNormalizedActiveSupplierAndTracesInJournal() {
        // Arrange
        when(supplierRepository.existsByNameIgnoreCase("Coopérative Itasy")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new SupplierWDto(
                "  Coopérative Itasy ", SupplierType.LOCAL, " ", "034 00 000 00", "Contact@Itasy.MG", null, null, null);

        // Act
        sut.createSupplier(request);

        // Assert
        var saved = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Coopérative Itasy");
        assertThat(saved.getValue().getContactName()).isNull();
        assertThat(saved.getValue().getEmail()).isEqualTo("contact@itasy.mg");
        assertThat(saved.getValue().isActive()).isTrue();
        verify(journalService)
                .record(
                        eq(JournalAction.CREATE),
                        eq("SUPPLIER"),
                        any(),
                        eq("Création du fournisseur Coopérative Itasy"));
    }

    @Test
    void createSupplier_nameAlreadyUsed_throwsConflictWithoutSaving() {
        // Arrange
        when(supplierRepository.existsByNameIgnoreCase("Coopérative Itasy")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> sut.createSupplier(request("Coopérative Itasy")))
                .isInstanceOf(ConflictException.class);
        verify(supplierRepository, never()).save(any());
        verifyNoInteractions(journalService);
    }

    @Test
    void updateSupplier_existingSupplier_appliesAllFieldsAndTracesInJournal() {
        // Arrange
        var supplier = existingSupplier();
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));
        when(supplierRepository.existsByNameIgnoreCaseAndIdNot("Import Océan", SUPPLIER_ID))
                .thenReturn(false);
        var request = new SupplierWDto(
                "Import Océan",
                SupplierType.FOREIGN,
                "Mr Lee",
                "+33 1 00 00 00 00",
                "lee@ocean.com",
                "Marseille",
                "Raw materials",
                null);

        // Act
        sut.updateSupplier(SUPPLIER_ID, request);

        // Assert
        assertThat(supplier.getName()).isEqualTo("Import Océan");
        assertThat(supplier.getType()).isEqualTo(SupplierType.FOREIGN);
        assertThat(supplier.getContactName()).isEqualTo("Mr Lee");
        assertThat(supplier.getPhone()).isEqualTo("+33 1 00 00 00 00");
        assertThat(supplier.getEmail()).isEqualTo("lee@ocean.com");
        assertThat(supplier.getAddress()).isEqualTo("Marseille");
        assertThat(supplier.getNotes()).isEqualTo("Raw materials");
        verify(journalService)
                .record(JournalAction.UPDATE, "SUPPLIER", SUPPLIER_ID, "Modification du fournisseur Import Océan");
    }

    @Test
    void updateSupplier_sameValuesWithOtherSpacing_leavesNoTrace() {
        // Arrange
        var supplier = existingSupplier();
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));
        when(supplierRepository.existsByNameIgnoreCaseAndIdNot("Coopérative Itasy", SUPPLIER_ID))
                .thenReturn(false);
        var request = new SupplierWDto("  Coopérative Itasy ", SupplierType.LOCAL, " ", "", null, null, null, null);

        // Act
        sut.updateSupplier(SUPPLIER_ID, request);

        // Assert
        assertThat(supplier.getName()).isEqualTo("Coopérative Itasy");
        assertThat(supplier.getContactName()).isNull();
        verifyNoInteractions(journalService);
    }

    @Test
    void updateSupplier_nameUsedByAnotherSupplier_throwsConflict() {
        // Arrange
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(existingSupplier()));
        when(supplierRepository.existsByNameIgnoreCaseAndIdNot("Import Océan", SUPPLIER_ID))
                .thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> sut.updateSupplier(SUPPLIER_ID, request("Import Océan")))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(journalService);
    }

    @Test
    void updateSupplier_staleVersion_throwsConflictAndChangesNothing() {
        // Arrange
        var supplier = existingSupplier();
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));
        var staleRequest = new SupplierWDto("Import Océan", SupplierType.FOREIGN, null, null, null, null, null, 7L);

        // Act + Assert
        assertThatThrownBy(() -> sut.updateSupplier(SUPPLIER_ID, staleRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("modifié entre-temps");
        assertThat(supplier.getName()).isNotEqualTo("Import Océan");
        verifyNoInteractions(journalService);
    }

    @Test
    void getSupplierById_unknownId_throwsNotFound() {
        // Arrange
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> sut.getSupplierById(SUPPLIER_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(SUPPLIER_ID.toString());
    }

    @Test
    void setActive_activeSupplierDeactivated_deactivatesAndTracesInJournal() {
        // Arrange
        var supplier = existingSupplier();
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(supplier));

        // Act
        sut.setActive(SUPPLIER_ID, false);

        // Assert
        assertThat(supplier.isActive()).isFalse();
        verify(journalService)
                .record(
                        JournalAction.UPDATE,
                        "SUPPLIER",
                        SUPPLIER_ID,
                        "Désactivation du fournisseur Coopérative Itasy");
    }

    @Test
    void setActive_stateAlreadyRequested_leavesNoTrace() {
        // Arrange
        when(supplierRepository.findById(SUPPLIER_ID)).thenReturn(Optional.of(existingSupplier()));

        // Act
        sut.setActive(SUPPLIER_ID, true);

        // Assert
        verify(journalService, never()).record(any(), anyString(), any(), anyString());
    }

    private static Supplier existingSupplier() {
        return Supplier.create(
                new SupplierDetails("Coopérative Itasy", SupplierType.LOCAL, null, null, null, null, null));
    }

    private static SupplierWDto request(String name) {
        return new SupplierWDto(name, SupplierType.LOCAL, null, null, null, null, null, null);
    }
}
