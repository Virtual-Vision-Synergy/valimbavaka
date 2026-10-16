package org.vvs.valimbavaka.feature.user.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.UuidGenerator;
import org.vvs.valimbavaka.shared.domain.BaseEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

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

    public static User create(UserProfile profile, String passwordHash, Role role) {
        var user = new User();
        user.applyProfile(profile, role);
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

    public void update(UserProfile profile, Role role, boolean isActive) {
        applyProfile(profile, role);
        this.active = isActive;
    }

    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private void applyProfile(UserProfile profile, Role role) {
        this.email = normalizeEmail(profile.email());
        this.firstName = profile.firstName().strip();
        this.lastName = profile.lastName().strip();
        this.role = Objects.requireNonNull(role);
    }
}
