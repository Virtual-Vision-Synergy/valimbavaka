package org.vvs.valimbavaka.feature.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.jspecify.annotations.Nullable;
import org.vvs.valimbavaka.shared.domain.BaseEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Rôle métier : référentiel fixé par les migrations, jamais créé par l'application. */
@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private RoleName name;

    /** Libellé affiché (Administrateur, Vendeuse, Livreur). */
    @Column(name = "description", columnDefinition = "TEXT")
    private @Nullable String description;
}
