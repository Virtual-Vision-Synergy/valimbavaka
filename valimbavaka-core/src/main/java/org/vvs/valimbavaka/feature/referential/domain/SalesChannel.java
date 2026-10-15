package org.vvs.valimbavaka.feature.referential.domain;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Canal de vente (boutique, Facebook, téléphone, livraison). Valeurs versionnées par les migrations. */
@Entity
@Table(name = "sales_channels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesChannel extends ReferentialEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;
}
