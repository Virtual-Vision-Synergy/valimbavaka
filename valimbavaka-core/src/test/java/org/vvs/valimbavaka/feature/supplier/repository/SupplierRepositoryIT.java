package org.vvs.valimbavaka.feature.supplier.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import org.vvs.valimbavaka.feature.supplier.domain.Supplier;
import org.vvs.valimbavaka.feature.supplier.domain.SupplierDetails;
import org.vvs.valimbavaka.feature.supplier.domain.SupplierType;
import org.vvs.valimbavaka.support.IntegrationTest;

/** FRS-01: paged search and case-insensitive name uniqueness of suppliers on PostgreSQL. */
@IntegrationTest
class SupplierRepositoryIT {

    @Autowired
    private SupplierRepository sut;

    @Test
    void save_newSupplier_isPersistedWithAuditFields() {
        // Act
        var saved = sut.save(supplier(uniqueName(), SupplierType.LOCAL, null));

        // Assert
        var reloaded = sut.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.isActive()).isTrue();
    }

    @Test
    void existsByNameIgnoreCase_sameNameOtherCase_returnsTrue() {
        // Arrange
        var name = uniqueName();
        var saved = sut.save(supplier(name, SupplierType.LOCAL, null));

        // Act + Assert
        assertThat(sut.existsByNameIgnoreCase(name.toUpperCase())).isTrue();
        assertThat(sut.existsByNameIgnoreCaseAndIdNot(name.toUpperCase(), saved.getId())).isFalse();
    }

    @Test
    void search_byContactName_findsSupplierWhoseNameDoesNotMatch() {
        // Arrange
        var contact = "Contact-" + UUID.randomUUID();
        var saved = sut.save(supplier(uniqueName(), SupplierType.LOCAL, contact));

        // Act
        var page = sut.search(contact.toLowerCase(), null, null, PageRequest.of(0, 10));

        // Assert
        assertThat(page.getContent()).extracting(Supplier::getId).containsExactly(saved.getId());
    }

    @Test
    void search_withTypeAndStatusFilters_returnsOnlyMatchingSuppliers() {
        // Arrange
        var prefix = "Filtre-" + UUID.randomUUID();
        var local = sut.save(supplier(prefix + "-local", SupplierType.LOCAL, null));
        sut.save(supplier(prefix + "-foreign", SupplierType.FOREIGN, null));
        var inactive = supplier(prefix + "-inactive", SupplierType.LOCAL, null);
        inactive.deactivate();
        sut.save(inactive);

        // Act
        var page = sut.search(prefix, SupplierType.LOCAL, true, PageRequest.of(0, 10));

        // Assert
        assertThat(page.getContent()).extracting(Supplier::getId).containsExactly(local.getId());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    private static String uniqueName() {
        return "Fournisseur-" + UUID.randomUUID();
    }

    private static Supplier supplier(String name, SupplierType type, String contactName) {
        return Supplier.create(new SupplierDetails(name, type, contactName, null, null, null, null));
    }
}
