package org.vvs.valimbavaka.iam.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.vvs.valimbavaka.common.domain.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    /** Un utilisateur porte un seul rôle. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    /** Vrai tant que l'utilisateur doit remplacer son mot de passe initial (ex. admin créé au démarrage). */
    @Column(nullable = false)
    private boolean mustChangePassword;

    public String getFullName() {
        return lastName + " " + firstName;
    }
}
