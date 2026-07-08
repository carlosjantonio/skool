package ao.skool.forum.internal.persistence;

import ao.skool.forum.internal.domain.ForumThread;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ForumThreadRepository extends JpaRepository<ForumThread, UUID> {

    List<ForumThread> findByForumIdOrderByPinnedDescLastActivityAtDesc(UUID forumId);
}
