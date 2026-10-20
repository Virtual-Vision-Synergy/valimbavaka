package org.vvs.valimbavaka.feature.referential.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Emplacement de stock (entrepôt, boutique, livraison, salon). Valeurs versionnées par les migrations. */
@Entity
@Table(name = "stock_locations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockLocation extends ReferentialEntity {
}
