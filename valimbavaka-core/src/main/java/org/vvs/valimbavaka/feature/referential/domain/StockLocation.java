package org.vvs.valimbavaka.feature.referential.domain;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Emplacement de stock (entrepôt, boutique, livraison, salon). Valeurs versionnées par les migrations. */
@Entity
@Table(name = "stock_locations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockLocation extends ReferentialEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;
}
