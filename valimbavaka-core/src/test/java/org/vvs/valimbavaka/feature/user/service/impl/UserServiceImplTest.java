package org.vvs.valimbavaka.feature.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.core.exception.ConflictException;
import org.vvs.valimbavaka.core.exception.NotFoundException;
import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.feature.referential.service.ReferentialService;
import org.vvs.valimbavaka.feature.user.config.AdminAccountProperties;
import org.vvs.valimbavaka.feature.user.domain.Role;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.User;
import org.vvs.valimbavaka.feature.user.dto.CreateUserWDto;
import org.vvs.valimbavaka.feature.user.exception.AdminAccountConfigurationException;
import org.vvs.valimbavaka.feature.user.mapper.UserMapper;
import org.vvs.valimbavaka.feature.user.repository.RoleRepository;
import org.vvs.valimbavaka.feature.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String RAW_PASSWORD = "mot-de-passe-provisoire-long";
    private static final String NEW_PASSWORD = "nouveau-mot-de-passe-choisi";
    private static final String PASSWORD_HASH = "{bcrypt}hash";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ReferentialService referentialService;

    @Mock
    private JournalService journalService;

    @Mock
    private AuditorAware<UUID> auditorAware;

    @Mock
    private User deletedUser;

    @Mock
    private User targetUser;

    @Mock
    private Role administratorRole;

    @Mock
    private Role sellerRole;

    private UserServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new UserServiceImpl(
                userRepository,
                roleRepository,
                userMapper,
                passwordEncoder,
                referentialService,
                journalService,
                auditorAware);
    }

    @Test
    void createInitialAdministratorIfMissing_administratorExists_createsNothing() {
        // given
        when(userRepository.existsByRoleName(RoleName.ADMIN)).thenReturn(true);
        // when
        boolean created = sut.createInitialAdministratorIfMissing(validAccount());
        // then
        assertThat(created).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void createInitialAdministratorIfMissing_variablesMissing_refusesStartup() {
        // given
        when(userRepository.existsByRoleName(RoleName.ADMIN)).thenReturn(false);
        var emptyAccount = new AdminAccountProperties("", "", "", "");
        // when / then
        assertThatThrownBy(() -> sut.createInitialAdministratorIfMissing(emptyAccount))
                .isInstanceOf(AdminAccountConfigurationException.class)
                .hasMessageContaining("ADMIN_EMAIL")
                .hasMessageContaining("ADMIN_PASSWORD");
        verify(userRepository, never()).save(any());
    }

    @Test
    void createInitialAdministratorIfMissing_passwordTooShort_neverLeaksPasswordInMessage() {
        // given
        when(userRepository.existsByRoleName(RoleName.ADMIN)).thenReturn(false);
        var account = new AdminAccountProperties("admin@valimbavaka.mg", "secret-court", "Rasoa", "Rakoto");
        // when / then
        assertThatThrownBy(() -> sut.createInitialAdministratorIfMissing(account))
                .isInstanceOf(AdminAccountConfigurationException.class)
                .message()
                .doesNotContain("secret-court");
    }

    @Test
    void createInitialAdministratorIfMissing_validVariables_createsAdministratorRequiringPasswordChange() {
        // given
        when(userRepository.existsByRoleName(RoleName.ADMIN)).thenReturn(false);
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(administratorRole));
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(PASSWORD_HASH);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // when
        boolean created = sut.createInitialAdministratorIfMissing(validAccount());
        // then
        var saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(created).isTrue();
        assertThat(saved.getValue())
                .extracting(
                        User::getEmail,
                        User::getPasswordHash,
                        User::getRole,
                        User::isPasswordChangeRequired,
                        User::isActive)
                .containsExactly("admin@valimbavaka.mg", PASSWORD_HASH, administratorRole, true, true);
    }

    @Test
    void changeOwnPassword_wrongCurrentPassword_throwsBusinessExceptionAndChangesNothing() {
        // given
        var userId = givenAccountWithPassword();
        when(passwordEncoder.matches("pas-le-bon", PASSWORD_HASH)).thenReturn(false);
        // when / then
        assertThatThrownBy(() -> sut.changeOwnPassword(userId, "pas-le-bon", NEW_PASSWORD))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("actuel est incorrect");
        verify(targetUser, never()).changePassword(any());
        verifyNoInteractions(journalService);
    }

    @Test
    void changeOwnPassword_sameAsCurrent_throwsBusinessExceptionAndChangesNothing() {
        // given
        var userId = givenAccountWithPassword();
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);
        // when / then
        assertThatThrownBy(() -> sut.changeOwnPassword(userId, RAW_PASSWORD, RAW_PASSWORD))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("différent");
        verify(targetUser, never()).changePassword(any());
        verifyNoInteractions(journalService);
    }

    @Test
    void changeOwnPassword_validRequest_storesNewHashAndRecordsInJournal() {
        // given
        var userId = givenAccountWithPassword();
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(passwordEncoder.matches(NEW_PASSWORD, PASSWORD_HASH)).thenReturn(false);
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("{argon2}nouveau");
        when(targetUser.getEmail()).thenReturn("admin@valimbavaka.mg");
        // when
        sut.changeOwnPassword(userId, RAW_PASSWORD, NEW_PASSWORD);
        // then
        verify(targetUser).changePassword("{argon2}nouveau");
        verify(journalService)
                .record(
                        JournalAction.UPDATE,
                        "USER",
                        userId,
                        "Changement du mot de passe de l'utilisateur admin@valimbavaka.mg");
    }

    private UUID givenAccountWithPassword() {
        var userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(targetUser));
        when(targetUser.getPasswordHash()).thenReturn(PASSWORD_HASH);
        return userId;
    }

    @Test
    void createUser_emailAlreadyUsed_throwsConflict() {
        // given
        var request = new CreateUserWDto(
                " Vendeuse@Valimbavaka.mg ", "mot-de-passe", "Hery", "Rabe", UUID.randomUUID(), null, null);
        when(userRepository.existsByEmail("vendeuse@valimbavaka.mg")).thenReturn(true);
        // when / then
        assertThatThrownBy(() -> sut.createUser(request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void createUser_unknownSalesChannel_throwsBusinessException() {
        // given
        var salesChannelId = UUID.randomUUID();
        var request = new CreateUserWDto(
                "vendeuse@valimbavaka.mg",
                "mot-de-passe",
                "Hery",
                "Rabe",
                UUID.randomUUID(),
                salesChannelId,
                new BigDecimal("5.00"));
        when(referentialService.isActiveSalesChannel(salesChannelId)).thenReturn(false);
        // when / then
        assertThatThrownBy(() -> sut.createUser(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Canal de vente introuvable");
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_sellerWithChannelAndCommission_savesSellerTerms() {
        // given
        var salesChannelId = UUID.randomUUID();
        var roleId = UUID.randomUUID();
        var request = new CreateUserWDto(
                "vendeuse@valimbavaka.mg",
                "mot-de-passe",
                "Hery",
                "Rabe",
                roleId,
                salesChannelId,
                new BigDecimal("7.50"));
        when(userRepository.existsByEmail("vendeuse@valimbavaka.mg")).thenReturn(false);
        when(referentialService.isActiveSalesChannel(salesChannelId)).thenReturn(true);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(sellerRole));
        when(sellerRole.getName()).thenReturn(RoleName.SELLER);
        when(passwordEncoder.encode("mot-de-passe")).thenReturn(PASSWORD_HASH);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // when
        sut.createUser(request);
        // then
        var saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue())
                .extracting(User::getSalesChannelId, User::getCommissionRate)
                .containsExactly(salesChannelId, new BigDecimal("7.50"));
    }

    @Test
    void createUser_sellerWithoutCommission_throwsBusinessException() {
        // given
        var roleId = UUID.randomUUID();
        var request = new CreateUserWDto("vendeuse@valimbavaka.mg", "mot-de-passe", "Hery", "Rabe", roleId, null, null);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(sellerRole));
        when(sellerRole.getName()).thenReturn(RoleName.SELLER);
        when(passwordEncoder.encode("mot-de-passe")).thenReturn(PASSWORD_HASH);
        // when / then
        assertThatThrownBy(() -> sut.createUser(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("obligatoires pour une vendeuse");
    }

    @Test
    void createUser_administratorWithCommission_throwsBusinessException() {
        // given
        var roleId = UUID.randomUUID();
        var request = new CreateUserWDto(
                "admin@valimbavaka.mg", "mot-de-passe", "Hery", "Rabe", roleId, null, new BigDecimal("5.00"));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(administratorRole));
        when(administratorRole.getName()).thenReturn(RoleName.ADMIN);
        when(passwordEncoder.encode("mot-de-passe")).thenReturn(PASSWORD_HASH);
        // when / then
        assertThatThrownBy(() -> sut.createUser(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("réservés aux vendeuses");
    }

    @Test
    void createUser_validRequest_recordsCreationInJournal() {
        // given
        var roleId = UUID.randomUUID();
        var request = new CreateUserWDto("admin2@valimbavaka.mg", "mot-de-passe", "Hery", "Rabe", roleId, null, null);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(administratorRole));
        when(administratorRole.getName()).thenReturn(RoleName.ADMIN);
        when(passwordEncoder.encode("mot-de-passe")).thenReturn(PASSWORD_HASH);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // when
        sut.createUser(request);
        // then
        verify(journalService)
                .record(
                        eq(JournalAction.CREATE),
                        eq("USER"),
                        any(),
                        eq("Création de l'utilisateur admin2@valimbavaka.mg"));
    }

    @Test
    void createUser_emailAlreadyUsed_recordsNothingInJournal() {
        // given
        var request = new CreateUserWDto(
                "doublon@valimbavaka.mg", "mot-de-passe", "Hery", "Rabe", UUID.randomUUID(), null, null);
        when(userRepository.existsByEmail("doublon@valimbavaka.mg")).thenReturn(true);
        // when / then
        assertThatThrownBy(() -> sut.createUser(request)).isInstanceOf(ConflictException.class);
        verifyNoInteractions(journalService);
    }

    @Test
    void deleteUserById_existingUser_recordsDeletionInJournal() {
        // given
        var userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(deletedUser));
        when(deletedUser.getEmail()).thenReturn("partant@valimbavaka.mg");
        // when
        sut.deleteUserById(userId);
        // then
        verify(userRepository).delete(deletedUser);
        verify(journalService)
                .record(JournalAction.DELETE, "USER", userId, "Suppression de l'utilisateur partant@valimbavaka.mg");
    }

    @Test
    void deleteUserById_unknownId_throwsNotFoundAndRecordsNothing() {
        // given
        var unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());
        // when / then
        assertThatThrownBy(() -> sut.deleteUserById(unknownId)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(journalService);
    }

    @Test
    void getUserById_unknownId_throwsNotFound() {
        // given
        var unknownId = UUID.randomUUID();
        when(userRepository.findByIdWithRole(unknownId)).thenReturn(Optional.empty());
        // when / then
        assertThatThrownBy(() -> sut.getUserById(unknownId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void setActive_deactivateActiveSeller_deactivatesAndRecordsInJournal() {
        // given
        var userId = UUID.randomUUID();
        when(userRepository.findByIdWithRole(userId)).thenReturn(Optional.of(targetUser));
        when(targetUser.isActive()).thenReturn(true);
        when(targetUser.getId()).thenReturn(userId);
        when(targetUser.getRole()).thenReturn(sellerRole);
        when(sellerRole.getName()).thenReturn(RoleName.SELLER);
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(UUID.randomUUID()));
        when(targetUser.getEmail()).thenReturn("vendeuse@valimbavaka.mg");
        // when
        sut.setActive(userId, false);
        // then
        verify(targetUser).deactivate();
        verify(journalService)
                .record(JournalAction.UPDATE, "USER", userId, "Désactivation de l'utilisateur vendeuse@valimbavaka.mg");
    }

    @Test
    void setActive_activateInactiveUser_activatesAndRecordsInJournal() {
        // given
        var userId = UUID.randomUUID();
        when(userRepository.findByIdWithRole(userId)).thenReturn(Optional.of(targetUser));
        when(targetUser.isActive()).thenReturn(false);
        when(targetUser.getEmail()).thenReturn("vendeuse@valimbavaka.mg");
        // when
        sut.setActive(userId, true);
        // then
        verify(targetUser).activate();
        verify(journalService)
                .record(JournalAction.UPDATE, "USER", userId, "Activation de l'utilisateur vendeuse@valimbavaka.mg");
    }

    @Test
    void setActive_stateUnchanged_changesNothingAndRecordsNothing() {
        // given
        var userId = UUID.randomUUID();
        when(userRepository.findByIdWithRole(userId)).thenReturn(Optional.of(targetUser));
        when(targetUser.isActive()).thenReturn(true);
        // when
        sut.setActive(userId, true);
        // then
        verify(targetUser, never()).activate();
        verifyNoInteractions(journalService);
    }

    @Test
    void setActive_ownAccount_throwsBusinessExceptionAndRecordsNothing() {
        // given
        var userId = UUID.randomUUID();
        when(userRepository.findByIdWithRole(userId)).thenReturn(Optional.of(targetUser));
        when(targetUser.isActive()).thenReturn(true);
        when(targetUser.getId()).thenReturn(userId);
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(userId));
        // when / then
        assertThatThrownBy(() -> sut.setActive(userId, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("propre compte");
        verify(targetUser, never()).deactivate();
        verifyNoInteractions(journalService);
    }

    @Test
    void setActive_lastActiveAdministrator_throwsBusinessExceptionAndRecordsNothing() {
        // given
        var userId = UUID.randomUUID();
        when(userRepository.findByIdWithRole(userId)).thenReturn(Optional.of(targetUser));
        when(targetUser.isActive()).thenReturn(true);
        when(targetUser.getId()).thenReturn(userId);
        when(targetUser.getRole()).thenReturn(administratorRole);
        when(administratorRole.getName()).thenReturn(RoleName.ADMIN);
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(UUID.randomUUID()));
        when(userRepository.countByRoleNameAndActiveTrue(RoleName.ADMIN)).thenReturn(1L);
        // when / then
        assertThatThrownBy(() -> sut.setActive(userId, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dernier administrateur");
        verify(targetUser, never()).deactivate();
        verifyNoInteractions(journalService);
    }

    @Test
    void setActive_unknownId_throwsNotFound() {
        // given
        var unknownId = UUID.randomUUID();
        when(userRepository.findByIdWithRole(unknownId)).thenReturn(Optional.empty());
        // when / then
        assertThatThrownBy(() -> sut.setActive(unknownId, false)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(journalService);
    }

    private static AdminAccountProperties validAccount() {
        return new AdminAccountProperties(" Admin@Valimbavaka.mg ", RAW_PASSWORD, "Rasoa", "Rakoto");
    }
}
