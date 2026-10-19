package org.vvs.valimbavaka.feature.referential.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Canal de vente (boutique, Facebook, téléphone, livraison). Valeurs versionnées par les migrations. */
@Entity
@Table(name = "sales_channels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesChannel extends ReferentialEntity {
}
