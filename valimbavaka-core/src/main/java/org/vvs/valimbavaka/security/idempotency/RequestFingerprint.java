package org.vvs.valimbavaka.security.idempotency;

import org.jspecify.annotations.Nullable;

/**
 * Empreinte d'une requête rejouable : méthode, chemin et empreinte du corps. L'empreinte du corps est
 * absente quand le corps n'a pas été lu (envoi de fichier, corps trop gros) : seul le chemin est alors comparé.
 */
public record RequestFingerprint(
        String method, String path, @Nullable String bodyHash) {

    /** Même méthode et même chemin ; le contenu se compare à part. */
    boolean sameTarget(RequestFingerprint other) {
        return method.equals(other.method) && path.equals(other.path);
    }

    /** Vrai seulement si les deux corps ont été empreints et diffèrent. */
    boolean differentContent(RequestFingerprint other) {
        return bodyHash != null && other.bodyHash != null && !bodyHash.equals(other.bodyHash);
    }
}
