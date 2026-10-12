package org.vvs.valimbavaka.referential.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Gamme de produits (Zezika, Botanika, Solika). */
@Entity
@Table(name = "product_range")
public class ProductRange extends ReferentialEntity {
}
