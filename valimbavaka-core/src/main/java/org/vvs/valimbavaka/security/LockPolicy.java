package org.vvs.valimbavaka.security;

import java.time.Duration;

/** Verrouillage après {@code maxAttempts} échecs, durée croissante à chaque échec supplémentaire (plafonnée). */
public record LockPolicy(int maxAttempts, Duration baseDuration, Duration maxDuration) {

    /** Durée du verrouillage pour un nombre d'échecs consécutifs ; {@link Duration#ZERO} sous le seuil. */
    public Duration lockDurationFor(int failedAttempts) {
        if (failedAttempts < maxAttempts) {
            return Duration.ZERO;
        }
        var duration = baseDuration.multipliedBy(failedAttempts - maxAttempts + 1L);
        return duration.compareTo(maxDuration) > 0 ? maxDuration : duration;
    }
}
