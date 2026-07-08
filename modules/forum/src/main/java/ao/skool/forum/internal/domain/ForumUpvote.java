package ao.skool.forum.internal.domain;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "forum_upvotes")
@IdClass(ForumUpvote.PK.class)
public class ForumUpvote {

    @Id
    @Column(name = "target_type", nullable = false, length = 8)
    private String targetType;

    @Id
    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ForumUpvote() {}

    public ForumUpvote(String targetType, UUID targetId, UUID userId) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.userId = userId;
    }

    public String targetType() { return targetType; }
    public UUID targetId() { return targetId; }
    public UUID userId() { return userId; }
    public Instant createdAt() { return createdAt; }

    public static class PK implements Serializable {
        private String targetType;
        private UUID targetId;
        private UUID userId;

        public PK() {}
        public PK(String targetType, UUID targetId, UUID userId) {
            this.targetType = targetType;
            this.targetId = targetId;
            this.userId = userId;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(targetType, pk.targetType)
                    && Objects.equals(targetId, pk.targetId)
                    && Objects.equals(userId, pk.userId);
        }
        @Override public int hashCode() { return Objects.hash(targetType, targetId, userId); }
    }
}
