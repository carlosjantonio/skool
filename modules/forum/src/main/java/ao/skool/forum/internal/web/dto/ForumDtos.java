package ao.skool.forum.internal.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ForumDtos {

    public record CreateForum(
            @NotNull UUID subjectId,
            @NotNull UUID turmaId,
            @NotNull UUID academicYearId,
            @NotBlank String title,
            String description
    ) {}

    public record ForumResponse(
            UUID id,
            UUID subjectId,
            UUID turmaId,
            UUID academicYearId,
            String title,
            String description,
            Instant createdAt
    ) {}

    public record CreateThread(
            @NotBlank String title,
            @NotBlank String body
    ) {}

    public record ThreadResponse(
            UUID id,
            UUID forumId,
            String title,
            String body,
            UUID authorId,
            String authorName,
            boolean pinned,
            boolean hidden,
            int upvoteCount,
            int replyCount,
            Instant lastActivityAt,
            Instant createdAt
    ) {}

    public record CreatePost(
            @NotBlank String body
    ) {}

    public record PostResponse(
            UUID id,
            UUID threadId,
            String body,
            UUID authorId,
            String authorName,
            String authorRole,
            int upvoteCount,
            boolean hidden,
            boolean markedVerified,
            Instant createdAt
    ) {}

    public record ThreadDetail(
            ThreadResponse thread,
            List<PostResponse> posts
    ) {}

    public record ModerationFlags(
            Boolean pinned,
            Boolean hidden,
            Boolean markedVerified
    ) {}

    private ForumDtos() {}
}
