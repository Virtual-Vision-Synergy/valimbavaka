package org.vvs.valimbavaka.feature.journal.service;

import java.util.UUID;

import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.dto.JournalEntryRDto;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;

/** Brique commune de traçabilité : chaque module appelle {@link #record} pour toute opération métier. */
public interface JournalService {

    /**
     * Ajoute une ligne horodatée au nom de l'utilisateur authentifié (« Système » hors requête
     * authentifiée). S'exécute dans la transaction de l'appelant : si la trace ne peut pas être écrite,
     * l'opération métier est annulée plutôt que de rester sans trace.
     *
     * @param resourceType type de la ressource en majuscules (ex. {@code USER})
     * @param description libellé lisible par l'administrateur, sans donnée sensible (mot de passe, jeton)
     */
    void record(JournalAction action, String resourceType, UUID resourceId, String description);

    PageResponse<JournalEntryRDto> getEntries(PageQuery query);
}
