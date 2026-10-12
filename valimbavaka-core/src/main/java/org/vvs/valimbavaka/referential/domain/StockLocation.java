package org.vvs.valimbavaka.referential.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Emplacement de stock (entrepôt, boutique, livraison, salon). */
@Entity
@Table(name = "stock_location")
public class StockLocation extends ReferentialEntity {
}
