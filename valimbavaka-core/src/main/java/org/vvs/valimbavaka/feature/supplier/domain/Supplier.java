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

    /** Days between the order and the delivery (FRS-04); defaults to the type's lead time. */
    @Column(name = "lead_time_days", nullable = false)
    private int leadTimeDays;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    public static Supplier create(SupplierDetails details) {
        var supplier = new Supplier();
        supplier.apply(details);
        supplier.active = true;
        return supplier;
    }

    /** Applies the sheet once normalized and reports whether anything changed (nothing to trace otherwise). */
    public boolean update(SupplierDetails details) {
        var before = details();
        apply(details);
        return !before.equals(details());
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
        var leadTime = details.leadTimeDays();
        this.leadTimeDays = leadTime == null ? type.defaultLeadTimeDays() : leadTime;
    }

    private SupplierDetails details() {
        return new SupplierDetails(name, type, contactName, phone, email, address, notes, leadTimeDays);
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
