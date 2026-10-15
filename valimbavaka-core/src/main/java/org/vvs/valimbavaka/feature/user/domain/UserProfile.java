package org.vvs.valimbavaka.feature.user.domain;

/** Identité d'un utilisateur, regroupée pour limiter le nombre de paramètres (§2.2). */
public record UserProfile(String email, String firstName, String lastName) {
}
