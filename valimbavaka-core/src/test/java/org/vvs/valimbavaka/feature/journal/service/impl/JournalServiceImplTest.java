package org.vvs.valimbavaka.feature.journal.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.vvs.valimbavaka.feature.journal.domain.JournalAction;
import org.vvs.valimbavaka.feature.journal.domain.JournalEntry;
import org.vvs.valimbavaka.feature.journal.mapper.JournalMapper;
import org.vvs.valimbavaka.feature.journal.repository.JournalEntryRepository;
import org.vvs.valimbavaka.shared.dto.PageQuery;
import org.vvs.valimbavaka.shared.dto.SortDirection;
import org.vvs.valimbavaka.security.AuthenticatedUser;
import org.vvs.valimbavaka.security.AuthenticatedUserLoader;

@ExtendWith(MockitoExtension.class)
class JournalServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-10-19T08:30:00Z");

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private JournalMapper journalMapper;

    @Mock
    private AuditorAware<UUID> auditorAware;

    @Mock
    private AuthenticatedUserLoader authenticatedUserLoader;

    @Mock
    private AuthenticatedUser author;

    private JournalServiceImpl sut;

    @BeforeEach
    void setUp() {
        var clock = Clock.fixed(NOW, ZoneOffset.UTC);
        sut = new JournalServiceImpl(
                journalEntryRepository, journalMapper, auditorAware, authenticatedUserLoader, clock);
    }

    @Test
    void record_authenticatedUser_savesTimestampedEntryInTheNameOfTheUser() {
        // given
        var authorId = UUID.randomUUID();
        var resourceId = UUID.randomUUID();
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(authorId));
        when(authenticatedUserLoader.findById(authorId)).thenReturn(Optional.of(author));
        when(author.firstName()).thenReturn("Rasoa");
        when(author.lastName()).thenReturn("Rakoto");
        // when
        sut.record(JournalAction.CREATE, "USER", resourceId, "Création de l'utilisateur x");
        // then
        var saved = savedEntry();
        assertThat(saved.getOccurredAt()).isEqualTo(NOW);
        assertThat(saved.getActorId()).isEqualTo(authorId);
        assertThat(saved.getActorName()).isEqualTo("Rasoa Rakoto");
        assertThat(saved.getAction()).isEqualTo(JournalAction.CREATE);
        assertThat(saved.getResourceType()).isEqualTo("USER");
        assertThat(saved.getResourceId()).isEqualTo(resourceId);
        assertThat(saved.getDescription()).isEqualTo("Création de l'utilisateur x");
    }

    @Test
    void record_descriptionLongerThanTheColumn_isTruncatedInsteadOfFailingTheOperation() {
        // given
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.empty());
        var description = "Annulation de la vente : " + "a".repeat(600);
        // when
        sut.record(JournalAction.CANCEL, "SALE", UUID.randomUUID(), description);
        // then
        var saved = savedEntry().getDescription();
        assertThat(saved)
                .hasSize(JournalEntry.DESCRIPTION_MAX_LENGTH)
                .startsWith("Annulation de la vente : ")
                .endsWith("…");
    }

    @Test
    void record_noAuthenticatedUser_attributesTheEntryToTheSystem() {
        // given
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.empty());
        // when
        sut.record(JournalAction.UPDATE, "USER", UUID.randomUUID(), "Tâche planifiée");
        // then
        var saved = savedEntry();
        assertThat(saved.getActorId()).isNull();
        assertThat(saved.getActorName()).isEqualTo(JournalServiceImpl.SYSTEM_ACTOR);
    }

    @Test
    void record_authorAccountNoLongerExists_keepsTheIdAndFlagsTheNameAsUnknown() {
        // given
        var authorId = UUID.randomUUID();
        when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of(authorId));
        when(authenticatedUserLoader.findById(authorId)).thenReturn(Optional.empty());
        // when
        sut.record(JournalAction.DELETE, "USER", UUID.randomUUID(), "Suppression");
        // then
        var saved = savedEntry();
        assertThat(saved.getActorId()).isEqualTo(authorId);
        assertThat(saved.getActorName()).isEqualTo(JournalServiceImpl.UNKNOWN_ACTOR);
    }

    @Test
    void getEntries_withDate_queriesTheUtcDayOldestFirst() {
        // given
        var query = new PageQuery(null, null, null, null, null);
        when(journalEntryRepository.searchBetween(eq(""), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());
        // when
        sut.getEntries(query, LocalDate.of(2026, 10, 29));
        // then
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(journalEntryRepository)
                .searchBetween(
                        eq(""),
                        eq(Instant.parse("2026-10-29T00:00:00Z")),
                        eq(Instant.parse("2026-10-30T00:00:00Z")),
                        pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("occurredAt").getDirection())
                .isEqualTo(Sort.Direction.ASC);
        verify(journalEntryRepository, never()).search(any(), any());
    }

    @Test
    void getEntries_withoutDate_listsEverythingNewestFirst() {
        // given
        var query = new PageQuery(null, null, "  rasoa ", null, null);
        when(journalEntryRepository.search(eq("rasoa"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        // when
        sut.getEntries(query, null);
        // then
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(journalEntryRepository).search(eq("rasoa"), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("occurredAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getEntries_withDateAndExplicitDirection_keepsTheRequestedDirection() {
        // given
        var query = new PageQuery(null, null, null, "occurredAt", SortDirection.DESC);
        when(journalEntryRepository.searchBetween(any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());
        // when
        sut.getEntries(query, LocalDate.of(2026, 10, 29));
        // then
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(journalEntryRepository).searchBetween(any(), any(), any(), pageable.capture());
        assertThat(pageable.getValue().getSort().getOrderFor("occurredAt").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    private JournalEntry savedEntry() {
        var captor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(captor.capture());
        assertThat(captor.getValue()).isNotNull();
        return captor.getValue();
    }
}
