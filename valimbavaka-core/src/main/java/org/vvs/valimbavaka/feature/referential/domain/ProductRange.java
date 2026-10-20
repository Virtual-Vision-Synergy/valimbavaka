package org.vvs.valimbavaka.feature.referential.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Gamme de produits (Zezika, Botanika, Solika). Valeurs versionnées par les migrations. */
@Entity
@Table(name = "product_ranges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductRange extends ReferentialEntity {
}
