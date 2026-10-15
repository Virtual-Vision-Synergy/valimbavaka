package org.vvs.valimbavaka.feature.referential.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import org.vvs.valimbavaka.shared.domain.BaseEntity;

import lombok.Getter;

/** Champs communs aux référentiels métier : code stable, libellé affiché, activation. */
@Getter
@MappedSuperclass
public abstract class ReferentialEntity extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
