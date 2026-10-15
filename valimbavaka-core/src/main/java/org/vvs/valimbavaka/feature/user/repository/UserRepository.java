package org.vvs.valimbavaka.feature.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.vvs.valimbavaka.feature.user.domain.RoleName;
import org.vvs.valimbavaka.feature.user.domain.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("select u from User u join fetch u.role where u.id = :userId")
    Optional<User> findByIdWithRole(UUID userId);

    /** Recherche insensible à la casse sur email, prénom et nom ; {@code search} vide = tout. */
    @Query(value = """
            select u from User u join fetch u.role
            where lower(u.email) like lower(concat('%', :search, '%'))
               or lower(u.firstName) like lower(concat('%', :search, '%'))
               or lower(u.lastName) like lower(concat('%', :search, '%'))
            """,
            countQuery = """
            select count(u) from User u
            where lower(u.email) like lower(concat('%', :search, '%'))
               or lower(u.firstName) like lower(concat('%', :search, '%'))
               or lower(u.lastName) like lower(concat('%', :search, '%'))
            """)
    Page<User> search(String search, Pageable pageable);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByRoleName(RoleName roleName);
}
