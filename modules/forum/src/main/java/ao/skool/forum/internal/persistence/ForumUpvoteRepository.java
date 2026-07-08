package ao.skool.forum.internal.persistence;

import ao.skool.forum.internal.domain.ForumUpvote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForumUpvoteRepository extends JpaRepository<ForumUpvote, ForumUpvote.PK> {
}
