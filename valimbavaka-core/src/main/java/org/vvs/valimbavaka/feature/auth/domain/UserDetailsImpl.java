package org.vvs.valimbavaka.feature.auth.domain;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;

import org.vvs.valimbavaka.security.audit.AuditableUser;

/** Principal de l'utilisateur connecté ; son identifiant alimente created_by / updated_by. */
@Getter
public class UserDetailsImpl implements UserDetails, AuditableUser {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID id;

    private final String username;

    private final String password;

    public UserDetailsImpl(UUID id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }
}
