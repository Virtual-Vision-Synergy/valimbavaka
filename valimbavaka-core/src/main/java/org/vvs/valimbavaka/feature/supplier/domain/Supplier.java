package org.vvs.valimbavaka.feature.supplier.domain;

import java.util.Locale;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.vvs.valimbavaka.shared.domain.BaseEntity;

@Entity
@Table(name = "suppliers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Supplier extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private SupplierType type;

    @Column(name = "contact_name", length = 150)
    private @Nullable String contactName;

    @Column(name = "phone", length = 50)
    private @Nullable String phone;

    @Column(name = "email")
    private @Nullable String email;

    @Column(name = "address", length = 500)
    private @Nullable String address;

    @Column(name = "notes", length = 2000)
    private @Nullable String notes;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public static Supplier create(SupplierDetails details) {
        var supplier = new Supplier();
        supplier.apply(details);
        supplier.active = true;
        return supplier;
    }

    public void update(SupplierDetails details) {
        apply(details);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private void apply(SupplierDetails details) {
        this.name = details.name().strip();
        this.type = Objects.requireNonNull(details.type());
        this.contactName = blankToNull(details.contactName());
        this.phone = blankToNull(details.phone());
        this.email = normalizeEmail(details.email());
        this.address = blankToNull(details.address());
        this.notes = blankToNull(details.notes());
    }

    private static @Nullable String normalizeEmail(@Nullable String email) {
        var value = blankToNull(email);
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private static @Nullable String blankToNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
