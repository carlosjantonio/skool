package ao.skool.forum.internal.persistence;

import ao.skool.forum.internal.domain.ForumPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ForumPostRepository extends JpaRepository<ForumPost, UUID> {

    List<ForumPost> findByThreadIdOrderByCreatedAtAsc(UUID threadId);
}
