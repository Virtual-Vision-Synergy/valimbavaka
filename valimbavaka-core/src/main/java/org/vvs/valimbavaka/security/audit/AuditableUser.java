package org.vvs.valimbavaka.security.audit;

import java.util.UUID;

/**
 * Principal capable de fournir son identifiant pour l'audit.
 * Contrat porté par security/ pour ne pas dépendre d'une feature (convention §2.5).
 */
public interface AuditableUser {

    UUID getId();
}
