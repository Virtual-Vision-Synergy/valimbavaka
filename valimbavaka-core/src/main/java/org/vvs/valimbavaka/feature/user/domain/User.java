package org.vvs.valimbavaka.feature.user.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.jspecify.annotations.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.vvs.valimbavaka.core.exception.BusinessException;
import org.vvs.valimbavaka.security.LockPolicy;
import org.vvs.valimbavaka.shared.domain.BaseEntity;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /** Hash préfixé {id} (DelegatingPasswordEncoder), jamais le mot de passe en clair. */
    @Column(name = "password", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /** Un utilisateur porte un seul rôle. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    /** Mot de passe provisoire : l'utilisateur doit le changer à sa prochaine connexion. */
    @Column(name = "is_password_change_required", nullable = false)
    private boolean passwordChangeRequired;

    /** Canal de vente de la vendeuse : référence par identifiant, pas d'association JPA entre features (§2.3). */
    @Column(name = "sales_channel_id")
    private @Nullable UUID salesChannelId;

    /** Taux de commission en pourcentage ; renseigné uniquement pour une vendeuse. */
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private @Nullable BigDecimal commissionRate;

    /** Échecs de connexion consécutifs ; remis à zéro au succès. */
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private @Nullable Instant lockedUntil;

    public static User create(UserProfile profile, String passwordHash, Role role) {
        return create(profile, passwordHash, role, SellerTerms.NONE);
    }

    public static User create(UserProfile profile, String passwordHash, Role role, SellerTerms terms) {
        var user = new User();
        user.applyProfile(profile, role, terms);
        user.passwordHash = Objects.requireNonNull(passwordHash);
        user.active = true;
        return user;
    }

    /** Compte créé avec un mot de passe provisoire (premier administrateur, création par un tiers). */
    public static User createWithTemporaryPassword(UserProfile profile, String passwordHash, Role role) {
        var user = create(profile, passwordHash, role);
        user.passwordChangeRequired = true;
        return user;
    }

    public void update(UserProfile profile, Role role, SellerTerms terms, boolean isActive) {
        applyProfile(profile, role, terms);
        this.active = isActive;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    /** Compte l'échec ; au-delà du seuil, verrouille pour une durée croissante (convention §9.2). */
    public void registerFailedLogin(Instant now, LockPolicy policy) {
        this.failedAttempts++;
        var lockDuration = policy.lockDurationFor(this.failedAttempts);
        if (!lockDuration.isZero()) {
            this.lockedUntil = now.plus(lockDuration);
        }
    }

    public void registerSuccessfulLogin() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }

    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private void applyProfile(UserProfile profile, Role role, SellerTerms terms) {
        validateTerms(role, terms);
        this.email = normalizeEmail(profile.email());
        this.firstName = profile.firstName().strip();
        this.lastName = profile.lastName().strip();
        this.role = Objects.requireNonNull(role);
        this.salesChannelId = terms.salesChannelId();
        this.commissionRate = terms.commissionRate();
    }

    /** Canal et commission n'ont de sens que pour une vendeuse, et vont ensemble : règle portée par l'entité. */
    private static void validateTerms(Role role, SellerTerms terms) {
        if (role.getName() == RoleName.SELLER) {
            if (!terms.isComplete()) {
                throw new BusinessException(
                        "Le canal de vente et le taux de commission sont obligatoires pour une vendeuse.");
            }
        } else if (!terms.isEmpty()) {
            throw new BusinessException("Le canal de vente et le taux de commission sont réservés aux vendeuses.");
        }
    }
}
