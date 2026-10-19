package org.vvs.valimbavaka.feature.journal.service.impl;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.domain.JournalEntry;
import org.vvs.valimbavaka.feature.journal.domain.JournalEntry.Actor;
import org.vvs.valimbavaka.feature.journal.domain.JournalEntry.Subject;
import org.vvs.valimbavaka.feature.journal.dto.JournalEntryRDto;
import org.vvs.valimbavaka.feature.journal.mapper.JournalMapper;
import org.vvs.valimbavaka.feature.journal.repository.JournalEntryRepository;
import org.vvs.valimbavaka.feature.journal.service.JournalService;
import org.vvs.valimbavaka.security.AuthenticatedUser;
import org.vvs.valimbavaka.security.AuthenticatedUserLoader;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.PageResponse;
import org.vvs.valimbavaka.shared.dto.SortDirection;

@Service
@Transactional(readOnly = true)
public class JournalServiceImpl implements JournalService {

    static final String SYSTEM_ACTOR = "Système";
    static final String UNKNOWN_ACTOR = "Compte inconnu";

    private static final Set<String> SORT_FIELDS = Set.of("occurredAt", "actorName", "action", "resourceType");

    private final JournalEntryRepository journalEntryRepository;
    private final JournalMapper journalMapper;
    private final AuditorAware<UUID> auditorAware;
    private final AuthenticatedUserLoader authenticatedUserLoader;
    private final Clock clock;

    public JournalServiceImpl(
            JournalEntryRepository journalEntryRepository,
            JournalMapper journalMapper,
            AuditorAware<UUID> auditorAware,
            AuthenticatedUserLoader authenticatedUserLoader,
            Clock clock) {
        this.journalEntryRepository = journalEntryRepository;
        this.journalMapper = journalMapper;
        this.auditorAware = auditorAware;
        this.authenticatedUserLoader = authenticatedUserLoader;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void record(JournalAction action, String resourceType, UUID resourceId, String description) {
        var subject = new Subject(resourceType, resourceId, description);
        journalEntryRepository.save(JournalEntry.of(clock.instant(), currentActor(), action, subject));
    }

    @Override
    public PageResponse<JournalEntryRDto> getEntries(PageQuery query) {
        var pageable = query.toPageable(SORT_FIELDS, "occurredAt", SortDirection.DESC);
        return PageResponse.from(
                journalEntryRepository.search(query.searchTerm(), pageable).map(journalMapper::toRDto));
    }

    private Actor currentActor() {
        return auditorAware
                .getCurrentAuditor()
                .map(id -> new Actor(id, nameOf(id)))
                .orElseGet(() -> new Actor(null, SYSTEM_ACTOR));
    }

    /** Compte introuvable (supprimé entre-temps) : l'identifiant reste la trace, le nom le signale. */
    private String nameOf(UUID userId) {
        return authenticatedUserLoader
                .findById(userId)
                .map(JournalServiceImpl::fullName)
                .orElse(UNKNOWN_ACTOR);
    }

    private static String fullName(AuthenticatedUser user) {
        return user.firstName() + " " + user.lastName();
    }
}
