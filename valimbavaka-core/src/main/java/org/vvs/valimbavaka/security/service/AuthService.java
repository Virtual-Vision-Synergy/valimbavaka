package org.vvs.valimbavaka.security.service;

import java.util.UUID;

import org.vvs.valimbavaka.security.AuthenticatedUser;
import org.vvs.valimbavaka.security.TokenPair;

/** Connexion, rotation du refresh token et déconnexion ; le rôle appliqué est toujours celui du compte. */
public interface AuthService {

    TokenPair login(String email, String rawPassword);

    /** Rotation : l'ancien jeton est révoqué ; sa réutilisation révoque toute la famille. */
    TokenPair refresh(String rawRefreshToken);

    void logout(String rawRefreshToken);

    /** Profil relu en base : le rôle renvoyé est toujours celui du compte, jamais celui d'un jeton ancien. */
    AuthenticatedUser currentUser(UUID userId);
}
