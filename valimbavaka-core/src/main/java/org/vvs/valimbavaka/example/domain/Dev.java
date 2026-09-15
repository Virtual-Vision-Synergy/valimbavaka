/**
 * This file is just an example
 */

package org.vvs.valimbavaka.example.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.vvs.valimbavaka.common.domain.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "dev")
public class Dev extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String name;
}