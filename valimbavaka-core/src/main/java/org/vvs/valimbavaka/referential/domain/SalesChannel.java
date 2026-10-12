package org.vvs.valimbavaka.referential.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Canal de vente (boutique, Facebook, téléphone, livraison). */
@Entity
@Table(name = "sales_channel")
public class SalesChannel extends ReferentialEntity {
}
