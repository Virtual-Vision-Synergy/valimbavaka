package org.vvs.valimbavaka.feature.delivery.domain;

import org.jspecify.annotations.Nullable;

/** Customer to deliver: name, phone, address and optional district, already stripped. */
public record DeliveryCustomer(
        String name, String phone, String address, @Nullable String district) {}
