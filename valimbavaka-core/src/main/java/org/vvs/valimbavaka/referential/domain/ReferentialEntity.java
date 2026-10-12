package org.vvs.valimbavaka.referential.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.vvs.valimbavaka.common.domain.BaseEntity;

/** Entrée de référentiel : un code stable (utilisable dans le code) et un libellé affiché. */
@Getter
@Setter
@MappedSuperclass
public abstract class ReferentialEntity extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, unique = true, length = 100)
    private String name;
}
