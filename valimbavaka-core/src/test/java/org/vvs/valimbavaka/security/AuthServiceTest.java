package org.vvs.valimbavaka.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.vvs.valimbavaka.core.exception.UnauthorizedException;
import org.vvs.valimbavaka.security.RefreshTokenStore.StoredRefreshToken;
import org.vvs.valimbavaka.security.jwt.JwtProperties;
import org.vvs.valimbavaka.security.jwt.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T08:00:00Z");
    private static final String EMAIL = "vendeuse@valimbavaka.mg";
    private static final String RAW_PASSWORD = "mot-de-passe-long-et-valide";
    private static final String PASSWORD_HASH = "{bcrypt}hash";
    private static final String DUMMY_HASH = "{bcrypt}dummy";
    private static final String ACCESS_TOKEN = "access.jwt.token";
    private static final LockPolicy LOCK_POLICY = new LockPolicy(5, Duration.ofMinutes(5));

    @Mock
    private AuthenticatedUserLoader users;
    @Mock
    private RefreshTokenStore refreshTokens;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService sut;

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode(anyString())).thenReturn(DUMMY_HASH);
        var jwtProperties = new JwtProperties("secret-de-test-de-32-caracteres-min!", Duration.ofMinutes(15),
                Duration.ofDays(7), Duration.ofSeconds(30));
        var authProperties = new AuthProperties(5, Duration.ofMinutes(5), false);
        sut = new AuthService(users, refreshTokens, passwordEncoder, jwtService, jwtProperties, authProperties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void login_validCredentials_returnsTokensForTheRoleStoredOnTheAccount() {
        // Arrange
        var user = user(true, null);
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(jwtService.issueAccessToken(user)).thenReturn(ACCESS_TOKEN);

        // Act
        var tokens = sut.login("  Vendeuse@Valimbavaka.mg ", RAW_PASSWORD);

        // Assert
        assertThat(tokens.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(tokens.user().role()).isEqualTo("SELLER");
        assertThat(tokens.refreshToken()).isNotBlank();
        verify(users).recordSuccess(user.id());
        verify(refreshTokens).save(eq(user.id()), any(UUID.class), anyString(), eq(NOW.plus(Duration.ofDays(7))));
    }

    @Test
    void login_wrongPassword_throwsBadCredentialsAndRecordsFailure() {
        // Arrange
        var user = user(true, null);
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> sut.login(EMAIL, RAW_PASSWORD)).isInstanceOf(BadCredentialsException.class);
        verify(users).recordFailure(user.id(), LOCK_POLICY);
        verify(refreshTokens, never()).save(any(), any(), anyString(), any());
    }

    @Test
    void login_unknownEmail_throwsSameBadCredentialsAndComparesAgainstDummyHash() {
        // Arrange
        when(users.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.matches(RAW_PASSWORD, DUMMY_HASH)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> sut.login(EMAIL, RAW_PASSWORD))
                .isInstanceOf(BadCredentialsException.class).hasMessage("Identifiants invalides");
        verify(passwordEncoder).matches(RAW_PASSWORD, DUMMY_HASH);
    }

    @Test
    void login_inactiveAccount_throwsBadCredentialsEvenWithGoodPassword() {
        // Arrange
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user(false, null)));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> sut.login(EMAIL, RAW_PASSWORD)).isInstanceOf(BadCredentialsException.class);
        verify(jwtService, never()).issueAccessToken(any());
    }

    @Test
    void login_lockedAccount_throwsBadCredentialsEvenWithGoodPassword() {
        // Arrange
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user(true, NOW.plusSeconds(60))));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> sut.login(EMAIL, RAW_PASSWORD)).isInstanceOf(BadCredentialsException.class);
        verify(jwtService, never()).issueAccessToken(any());
    }

    @Test
    void refresh_validToken_revokesOldOneAndKeepsTheSameFamily() {
        // Arrange
        var user = user(true, null);
        var familyId = UUID.randomUUID();
        var stored = new StoredRefreshToken(UUID.randomUUID(), user.id(), familyId, NOW.plusSeconds(3600), false);
        when(refreshTokens.findByHash(anyString())).thenReturn(Optional.of(stored));
        when(users.findById(user.id())).thenReturn(Optional.of(user));
        when(jwtService.issueAccessToken(user)).thenReturn(ACCESS_TOKEN);

        // Act
        var tokens = sut.refresh("raw-refresh-token");

        // Assert
        assertThat(tokens.accessToken()).isEqualTo(ACCESS_TOKEN);
        verify(refreshTokens).revoke(stored.id());
        verify(refreshTokens).save(eq(user.id()), eq(familyId), anyString(), any(Instant.class));
    }

    @Test
    void refresh_revokedToken_revokesWholeFamilyAndThrowsUnauthorized() {
        // Arrange
        var familyId = UUID.randomUUID();
        var stored = new StoredRefreshToken(UUID.randomUUID(), UUID.randomUUID(), familyId,
                NOW.plusSeconds(3600), true);
        when(refreshTokens.findByHash(anyString())).thenReturn(Optional.of(stored));

        // Act + Assert
        assertThatThrownBy(() -> sut.refresh("reused-token")).isInstanceOf(UnauthorizedException.class);
        verify(refreshTokens).revokeFamily(familyId);
    }

    @Test
    void refresh_expiredToken_throwsUnauthorized() {
        // Arrange
        var user = user(true, null);
        var stored = new StoredRefreshToken(UUID.randomUUID(), user.id(), UUID.randomUUID(), NOW.minusSeconds(1), false);
        when(refreshTokens.findByHash(anyString())).thenReturn(Optional.of(stored));
        when(users.findById(user.id())).thenReturn(Optional.of(user));

        // Act + Assert
        assertThatThrownBy(() -> sut.refresh("expired-token")).isInstanceOf(UnauthorizedException.class);
        verify(refreshTokens, never()).revoke(any());
    }

    @Test
    void refresh_deactivatedAccount_revokesFamilyAndThrowsUnauthorized() {
        // Arrange
        var user = user(false, null);
        var familyId = UUID.randomUUID();
        var stored = new StoredRefreshToken(UUID.randomUUID(), user.id(), familyId, NOW.plusSeconds(3600), false);
        when(refreshTokens.findByHash(anyString())).thenReturn(Optional.of(stored));
        when(users.findById(user.id())).thenReturn(Optional.of(user));

        // Act + Assert
        assertThatThrownBy(() -> sut.refresh("token")).isInstanceOf(UnauthorizedException.class);
        verify(refreshTokens).revokeFamily(familyId);
    }

    @Test
    void logout_knownToken_revokesItsFamily() {
        // Arrange
        var familyId = UUID.randomUUID();
        var stored = new StoredRefreshToken(UUID.randomUUID(), UUID.randomUUID(), familyId, NOW.plusSeconds(60), false);
        when(refreshTokens.findByHash(anyString())).thenReturn(Optional.of(stored));

        // Act
        sut.logout("token");

        // Assert
        verify(refreshTokens).revokeFamily(familyId);
    }

    private static AuthenticatedUser user(boolean enabled, Instant lockedUntil) {
        return new AuthenticatedUser(UUID.randomUUID(), EMAIL, "Hanta", "Rasolofoson", "SELLER", "Vendeuse",
                PASSWORD_HASH, Set.of("sales.read"), enabled, false, lockedUntil);
    }
}
