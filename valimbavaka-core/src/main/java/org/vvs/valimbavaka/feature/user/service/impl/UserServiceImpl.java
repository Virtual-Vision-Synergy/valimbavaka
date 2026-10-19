package org.vvs.valimbavaka.feature.user.service.impl;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.user.config.AdminAccountProperties;
import org.vvs.valimbavaka.feature.user.domain.Role;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.SellerTerms;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.domain.UserProfile;
import org.vvs.valimbavaka.feature.user.dto.CreateUserWDto;
import org.vvs.valimbavaka.feature.user.dto.UpdateUserWDto;
import org.vvs.valimbavaka.feature.user.dto.UserRDto;
import org.vvs.valimbavaka.feature.user.exception.AdminAccountConfigurationException;
import org.vvs.valimbavaka.feature.user.mapper.UserMapper;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;
import org.vvs.valimbavaka.feature.user.service.UserService;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final String RESOURCE_TYPE = "USER";
    private static final String EMAIL_ALREADY_USED = "Un utilisateur avec cet email existe déjà.";
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "email", "firstName", "lastName");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ReferentialService referentialService;
    private final JournalService journalService;

    public UserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            ReferentialService referentialService,
            JournalService journalService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.referentialService = referentialService;
        this.journalService = journalService;
    }

    @Override
    public PageResponse<UserRDto> getUsers(PageQuery query) {
        var pageable = query.toPageable(SORT_FIELDS, "createdAt", SortDirection.DESC);
        return PageResponse.from(
                userRepository.search(query.searchTerm(), pageable).map(userMapper::toRDto));
    }

    @Override
    public UserRDto getUserById(UUID userId) {
        return userMapper.toRDto(findUserWithRole(userId));
    }

    @Override
    @Transactional
    public UserRDto createUser(CreateUserWDto request) {
        if (userRepository.existsByEmail(User.normalizeEmail(request.email()))) {
            throw new ConflictException(EMAIL_ALREADY_USED);
        }
        var profile = new UserProfile(request.email(), request.firstName(), request.lastName());
        var terms = sellerTerms(request.salesChannelId(), request.commissionRate());
        var user = User.create(profile, passwordEncoder.encode(request.password()), findRole(request.roleId()), terms);
        User saved = userRepository.save(user);
        journalService.record(
                JournalAction.CREATE, RESOURCE_TYPE, saved.getId(), "Création de l'utilisateur " + saved.getEmail());
        log.info("Utilisateur {} créé", saved.getId());
        return userMapper.toRDto(saved);
    }

    @Override
    @Transactional
    public UserRDto updateUser(UUID userId, UpdateUserWDto request) {
        User user = findUserWithRole(userId);
        if (userRepository.existsByEmailAndIdNot(User.normalizeEmail(request.email()), userId)) {
            throw new ConflictException(EMAIL_ALREADY_USED);
        }
        var profile = new UserProfile(request.email(), request.firstName(), request.lastName());
        var terms = sellerTerms(request.salesChannelId(), request.commissionRate());
        user.update(profile, findRole(request.roleId()), terms, request.active());
        journalService.record(
                JournalAction.UPDATE, RESOURCE_TYPE, userId, "Modification de l'utilisateur " + user.getEmail());
        return userMapper.toRDto(user);
    }

    @Override
    @Transactional
    public void deleteUserById(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> userNotFound(userId));
        userRepository.delete(user);
        journalService.record(
                JournalAction.DELETE, RESOURCE_TYPE, userId, "Suppression de l'utilisateur " + user.getEmail());
        log.info("Utilisateur {} supprimé", userId);
    }

    @Override
    @Transactional
    public boolean createInitialAdministratorIfMissing(AdminAccountProperties account) {
        if (userRepository.existsByRoleName(RoleName.ADMIN)) {
            return false;
        }
        var violations = account.violations();
        if (!violations.isEmpty()) {
            throw new AdminAccountConfigurationException("Aucun administrateur en base : renseigner les variables "
                    + "d'environnement du premier compte. " + String.join(" ; ", violations));
        }
        // Champs non null : garanti par violations() ci-dessus.
        var profile = new UserProfile(
                Objects.requireNonNull(account.email()),
                Objects.requireNonNull(account.firstName()),
                Objects.requireNonNull(account.lastName()));
        var passwordHash = Objects.requireNonNull(passwordEncoder.encode(Objects.requireNonNull(account.password())));
        var administrator = User.createWithTemporaryPassword(
                profile,
                passwordHash,
                roleRepository
                        .findByName(RoleName.ADMIN)
                        .orElseThrow(() -> new AdminAccountConfigurationException(
                                "Rôle ADMIN absent : vérifier les migrations Liquibase")));
        User saved = userRepository.save(administrator);
        log.info("Administrateur initial {} créé, changement de mot de passe requis", saved.getId());
        return true;
    }

    private User findUserWithRole(UUID userId) {
        return userRepository.findByIdWithRole(userId).orElseThrow(() -> userNotFound(userId));
    }

    private Role findRole(UUID roleId) {
        return roleRepository.findById(roleId).orElseThrow(() -> new NotFoundException("Rôle introuvable : " + roleId));
    }

    private SellerTerms sellerTerms(@Nullable UUID salesChannelId, @Nullable BigDecimal commissionRate) {
        if (salesChannelId != null && !referentialService.isActiveSalesChannel(salesChannelId)) {
            throw new BusinessException("Canal de vente introuvable : " + salesChannelId);
        }
        return new SellerTerms(salesChannelId, commissionRate);
    }

    private static NotFoundException userNotFound(UUID userId) {
        return new NotFoundException("Utilisateur introuvable : " + userId);
    }
}
