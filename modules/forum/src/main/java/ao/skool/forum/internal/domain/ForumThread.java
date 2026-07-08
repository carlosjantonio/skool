package ao.skool.forum.internal.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "forum_threads",
       indexes = {
           @Index(name = "idx_threads_forum", columnList = "forum_id, last_activity_at DESC"),
           @Index(name = "idx_threads_tenant", columnList = "tenant_id")
       })
public class ForumThread {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "forum_id", nullable = false)
    private UUID forumId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "author_name", nullable = false, length = 255)
    private String authorName;

    @Column(nullable = false)
    private boolean pinned = false;

    @Column(nullable = false)
    private boolean hidden = false;

    @Column(name = "upvote_count", nullable = false)
    private int upvoteCount = 0;

    @Column(name = "reply_count", nullable = false)
    private int replyCount = 0;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ForumThread() {}

    public ForumThread(UUID id, UUID tenantId, UUID forumId, String title, String body,
                       UUID authorId, String authorName) {
        this.id = id;
        this.tenantId = tenantId;
        this.forumId = forumId;
        this.title = title;
        this.body = body;
        this.authorId = authorId;
        this.authorName = authorName;
    }

    public void bumpReply() {
        this.replyCount++;
        this.lastActivityAt = Instant.now();
    }
    public void pin(boolean pinned) { this.pinned = pinned; }
    public void hide(boolean hidden) { this.hidden = hidden; }
    public void incrementUpvote() { this.upvoteCount++; }
    public void decrementUpvote() { if (this.upvoteCount > 0) this.upvoteCount--; }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID forumId() { return forumId; }
    public String title() { return title; }
    public String body() { return body; }
    public UUID authorId() { return authorId; }
    public String authorName() { return authorName; }
    public boolean pinned() { return pinned; }
    public boolean hidden() { return hidden; }
    public int upvoteCount() { return upvoteCount; }
    public int replyCount() { return replyCount; }
    public Instant lastActivityAt() { return lastActivityAt; }
    public Instant createdAt() { return createdAt; }
}
