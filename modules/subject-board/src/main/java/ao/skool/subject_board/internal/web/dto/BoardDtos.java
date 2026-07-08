package ao.skool.subject_board.internal.web.dto;

import ao.skool.subject_board.internal.domain.BoardEntryKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class BoardDtos {

    public record CreateEntry(
            @NotNull UUID subjectId,
            @NotNull UUID turmaId,
            @NotNull UUID academicYearId,
            @NotNull BoardEntryKind kind,
            @NotBlank String title,
            String body,
            UUID documentId,
            String externalUrl,
            Instant dueAt,
            Boolean pinned,
            Boolean lowBandwidth
    ) {}

    public record EntryResponse(
            UUID id,
            UUID subjectId,
            UUID turmaId,
            UUID academicYearId,
            BoardEntryKind kind,
            String title,
            String body,
            UUID documentId,
            String externalUrl,
            Instant dueAt,
            boolean pinned,
            boolean lowBandwidth,
            UUID authorId,
            String authorName,
            Instant publishedAt
    ) {}

    private BoardDtos() {}
}
