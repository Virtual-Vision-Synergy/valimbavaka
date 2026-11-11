package org.vvs.valimbavaka.feature.sale.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.jspecify.annotations.Nullable;

import org.vvs.valimbavaka.feature.sale.domain.PaymentMethod;

/**
 * Sale entered at the counter (VTE-01). Number, day, stock location, prices, totals, discount and status are set by
 * the server; {@code promoCode} (VTE-04) is only a request: the server checks it and computes the discount.
 */
public record SaleWDto(
        @NotNull(message = "Le canal de vente est obligatoire")
        UUID salesChannelId,

        @NotNull(message = "La vendeuse est obligatoire") UUID sellerId,

        @NotNull(message = "Le mode de règlement est obligatoire")
        PaymentMethod paymentMethod,

        @NotEmpty(message = "La vente doit compter au moins un article")
        @Size(max = 100, message = "Une vente compte {max} lignes au maximum")
        @Valid
        List<@NotNull(message = "Une ligne vide n'est pas acceptée") SaleLineWDto> lines,

        @Pattern(regexp = "^\\s*[A-Za-z0-9]{3,12}\\s*$|^\\s*$", message = "Le code compte 3 à 12 lettres ou chiffres")
        @Nullable
        String promoCode) {}
