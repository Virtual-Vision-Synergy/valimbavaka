package org.vvs.valimbavaka.feature.user.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.user.config.AdminAccountProperties;
import org.vvs.valimbavaka.feature.user.dto.CreateUserWDto;
import org.vvs.valimbavaka.feature.user.dto.UpdateUserWDto;
import org.vvs.valimbavaka.feature.user.dto.UserRDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

public interface UserService {

    PageResponse<UserRDto> getUsers(PageQuery query);

    UserRDto getUserById(UUID userId);

    UserRDto createUser(CreateUserWDto request);

    UserRDto updateUser(UUID userId, UpdateUserWDto request);

    void deleteUserById(UUID userId);

    /**
     * Crée le premier administrateur si aucun n'existe (idempotent). Le compte doit changer son mot
     * de passe à la première connexion.
     *
     * @return {@code true} si un compte a été créé
     * @throws org.vvs.valimbavaka.feature.user.exception.AdminAccountConfigurationException si aucun
     *         administrateur n'existe et que la configuration est absente ou invalide
     */
    boolean createInitialAdministratorIfMissing(AdminAccountProperties account);
}
