package org.vvs.valimbavaka.shared.operation;

import java.util.Optional;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/**
 * Socle des repositories d'opérations validées : ni suppression ni {@code JpaRepository} (JNL-02).
 * Vérifié par ArchUnit : aucune méthode {@code delete*} ou {@code remove*} ne peut être ajoutée.
 */
@NoRepositoryBean
public interface OperationRepository<T, ID> extends Repository<T, ID> {

    <S extends T> S save(S operation);

    Optional<T> findById(ID id);
}
