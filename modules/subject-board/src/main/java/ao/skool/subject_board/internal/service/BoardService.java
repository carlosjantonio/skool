package ao.skool.subject_board.internal.service;

import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.common.web.error.ApplicationException;
import ao.skool.common.web.error.NotFoundException;
import ao.skool.subject_board.api.event.AnnouncementPosted;
import ao.skool.subject_board.internal.domain.BoardEntry;
import ao.skool.subject_board.internal.domain.BoardEntryKind;
import ao.skool.subject_board.internal.persistence.BoardEntryRepository;
import ao.skool.subject_board.internal.web.dto.BoardDtos.CreateEntry;
import ao.skool.subject_board.internal.web.dto.BoardDtos.EntryResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BoardService {

    private final BoardEntryRepository entries;
    private final TenantContext tenant;
    private final ApplicationEventPublisher events;

    public BoardService(BoardEntryRepository entries, TenantContext tenant,
                        ApplicationEventPublisher events) {
        this.entries = entries;
        this.tenant = tenant;
        this.events = events;
    }

    public EntryResponse create(CreateEntry cmd) {
        validate(cmd);
        var principal = currentPrincipal();
        BoardEntry entry = new BoardEntry(UUID.randomUUID(), tenant.current().value(),
                cmd.subjectId(), cmd.turmaId(), cmd.academicYearId(),
                cmd.kind(), cmd.title(), cmd.body(), cmd.documentId(),
                cmd.externalUrl(), cmd.dueAt(),
                cmd.pinned() != null && cmd.pinned(),
                cmd.lowBandwidth() != null && cmd.lowBandwidth(),
                principal != null ? uuid(principal.userId()) : null,
                principal != null ? principal.fullName() : null);
        entries.save(entry);

        if (entry.kind() == BoardEntryKind.ANNOUNCEMENT) {
            events.publishEvent(new AnnouncementPosted(UUID.randomUUID(), tenant.current(),
                    entry.id(), entry.subjectId(), entry.turmaId(), entry.title(), Instant.now()));
        }
        return toResponse(entry);
    }

    public void delete(UUID id) {
        BoardEntry e = entries.findById(id).orElseThrow(NotFoundException::new);
        if (!e.tenantId().equals(tenant.current().value())) throw new NotFoundException();
        entries.delete(e);
    }

    @Transactional(readOnly = true)
    public List<EntryResponse> feedForSubject(UUID subjectId, UUID turmaId) {
        return entries.findBySubjectIdAndTurmaIdOrderByPinnedDescPublishedAtDesc(subjectId, turmaId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EntryResponse> feedForTurma(UUID turmaId) {
        return entries.findByTenantIdAndTurmaIdOrderByPinnedDescPublishedAtDesc(
                        tenant.current().value(), turmaId)
                .stream().map(this::toResponse).toList();
    }

    private void validate(CreateEntry cmd) {
        switch (cmd.kind()) {
            case MATERIAL -> {
                if (cmd.documentId() == null) {
                    throw new ApplicationException(HttpStatus.BAD_REQUEST, "error.validation");
                }
            }
            case LINK -> {
                if (cmd.externalUrl() == null || cmd.externalUrl().isBlank()) {
                    throw new ApplicationException(HttpStatus.BAD_REQUEST, "error.validation");
                }
            }
            case ANNOUNCEMENT -> {
                if (cmd.body() == null || cmd.body().isBlank()) {
                    throw new ApplicationException(HttpStatus.BAD_REQUEST, "error.validation");
                }
            }
        }
    }

    private SkoolPrincipal currentPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getPrincipal() instanceof SkoolPrincipal p) ? p : null;
    }

    private UUID uuid(String s) {
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }

    private EntryResponse toResponse(BoardEntry e) {
        return new EntryResponse(e.id(), e.subjectId(), e.turmaId(), e.academicYearId(),
                e.kind(), e.title(), e.body(), e.documentId(), e.externalUrl(),
                e.dueAt(), e.pinned(), e.lowBandwidth(), e.authorId(), e.authorName(),
                e.publishedAt());
    }
}
