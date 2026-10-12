package org.vvs.valimbavaka.iam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.vvs.valimbavaka.iam.domain.Role;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByName(String name);
}
