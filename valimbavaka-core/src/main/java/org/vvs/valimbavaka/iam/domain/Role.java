package org.vvs.valimbavaka.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.vvs.valimbavaka.common.domain.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "role")
public class Role extends BaseEntity {
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
