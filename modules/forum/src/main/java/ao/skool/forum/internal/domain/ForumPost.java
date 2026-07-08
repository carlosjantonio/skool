package ao.skool.forum.internal.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "forum_posts",
       indexes = @Index(name = "idx_posts_thread", columnList = "thread_id, created_at ASC"))
public class ForumPost {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "author_name", nullable = false, length = 255)
    private String authorName;

    /** Recorded at post time so historical rows survive a user's role change. */
    @Column(name = "author_role", nullable = false, length = 24)
    private String authorRole;

    @Column(name = "upvote_count", nullable = false)
    private int upvoteCount = 0;

    @Column(nullable = false)
    private boolean hidden = false;

    @Column(name = "marked_verified", nullable = false)
    private boolean markedVerified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ForumPost() {}

    public ForumPost(UUID id, UUID tenantId, UUID threadId, String body,
                     UUID authorId, String authorName, String authorRole) {
        this.id = id;
        this.tenantId = tenantId;
        this.threadId = threadId;
        this.body = body;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorRole = authorRole;
    }

    public void hide(boolean hidden) { this.hidden = hidden; }
    public void markVerified(boolean verified) { this.markedVerified = verified; }
    public void incrementUpvote() { this.upvoteCount++; }
    public void decrementUpvote() { if (this.upvoteCount > 0) this.upvoteCount--; }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID threadId() { return threadId; }
    public String body() { return body; }
    public UUID authorId() { return authorId; }
    public String authorName() { return authorName; }
    public String authorRole() { return authorRole; }
    public int upvoteCount() { return upvoteCount; }
    public boolean hidden() { return hidden; }
    public boolean markedVerified() { return markedVerified; }
    public Instant createdAt() { return createdAt; }
}
