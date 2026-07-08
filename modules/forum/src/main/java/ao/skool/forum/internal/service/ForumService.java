package ao.skool.forum.internal.service;

import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.common.security.Roles;
import ao.skool.common.web.error.ApplicationException;
import ao.skool.common.web.error.NotFoundException;
import ao.skool.forum.api.event.ForumReplyPosted;
import ao.skool.forum.internal.domain.Forum;
import ao.skool.forum.internal.domain.ForumPost;
import ao.skool.forum.internal.domain.ForumThread;
import ao.skool.forum.internal.domain.ForumUpvote;
import ao.skool.forum.internal.persistence.ForumPostRepository;
import ao.skool.forum.internal.persistence.ForumRepository;
import ao.skool.forum.internal.persistence.ForumThreadRepository;
import ao.skool.forum.internal.persistence.ForumUpvoteRepository;
import ao.skool.forum.internal.web.dto.ForumDtos.CreateForum;
import ao.skool.forum.internal.web.dto.ForumDtos.CreatePost;
import ao.skool.forum.internal.web.dto.ForumDtos.CreateThread;
import ao.skool.forum.internal.web.dto.ForumDtos.ForumResponse;
import ao.skool.forum.internal.web.dto.ForumDtos.ModerationFlags;
import ao.skool.forum.internal.web.dto.ForumDtos.PostResponse;
import ao.skool.forum.internal.web.dto.ForumDtos.ThreadDetail;
import ao.skool.forum.internal.web.dto.ForumDtos.ThreadResponse;
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
public class ForumService {

    private static final String TARGET_THREAD = "THREAD";
    private static final String TARGET_POST = "POST";

    private final ForumRepository forums;
    private final ForumThreadRepository threads;
    private final ForumPostRepository posts;
    private final ForumUpvoteRepository upvotes;
    private final TenantContext tenant;
    private final ApplicationEventPublisher events;

    public ForumService(ForumRepository forums, ForumThreadRepository threads,
                        ForumPostRepository posts, ForumUpvoteRepository upvotes,
                        TenantContext tenant, ApplicationEventPublisher events) {
        this.forums = forums;
        this.threads = threads;
        this.posts = posts;
        this.upvotes = upvotes;
        this.tenant = tenant;
        this.events = events;
    }

    public ForumResponse createForum(CreateForum cmd) {
        UUID tenantId = tenant.current().value();
        forums.findBySubjectIdAndTurmaIdAndAcademicYearId(cmd.subjectId(), cmd.turmaId(), cmd.academicYearId())
                .ifPresent(f -> { throw new ApplicationException(HttpStatus.CONFLICT, "error.conflict"); });
        Forum f = new Forum(UUID.randomUUID(), tenantId, cmd.subjectId(), cmd.turmaId(),
                cmd.academicYearId(), cmd.title(), cmd.description());
        forums.save(f);
        return toForum(f);
    }

    @Transactional(readOnly = true)
    public ForumResponse findOrGetForum(UUID subjectId, UUID turmaId, UUID academicYearId) {
        Forum f = forums.findBySubjectIdAndTurmaIdAndAcademicYearId(subjectId, turmaId, academicYearId)
                .orElseThrow(NotFoundException::new);
        return toForum(f);
    }

    @Transactional(readOnly = true)
    public List<ForumResponse> listForumsByTurma(UUID turmaId) {
        UUID tenantId = tenant.current().value();
        return forums.findByTenantIdAndTurmaIdOrderByTitleAsc(tenantId, turmaId)
                .stream().map(this::toForum).toList();
    }

    public ThreadResponse createThread(UUID forumId, CreateThread cmd) {
        Forum forum = requireOwnedForum(forumId);
        var principal = currentPrincipal();
        ForumThread t = new ForumThread(UUID.randomUUID(), forum.tenantId(), forum.id(),
                cmd.title(), cmd.body(), UUID.fromString(principal.userId()), principal.fullName());
        threads.save(t);
        return toThread(t);
    }

    @Transactional(readOnly = true)
    public List<ThreadResponse> listThreads(UUID forumId) {
        requireOwnedForum(forumId);
        return threads.findByForumIdOrderByPinnedDescLastActivityAtDesc(forumId)
                .stream().filter(t -> !t.hidden()).map(this::toThread).toList();
    }

    @Transactional(readOnly = true)
    public ThreadDetail getThread(UUID threadId) {
        ForumThread t = requireOwnedThread(threadId);
        List<PostResponse> postRows = posts.findByThreadIdOrderByCreatedAtAsc(threadId)
                .stream().filter(p -> !p.hidden()).map(this::toPost).toList();
        return new ThreadDetail(toThread(t), postRows);
    }

    public PostResponse createPost(UUID threadId, CreatePost cmd) {
        ForumThread t = requireOwnedThread(threadId);
        var principal = currentPrincipal();
        String role = principal.roles().isEmpty() ? "STUDENT" : principal.roles().iterator().next();
        ForumPost p = new ForumPost(UUID.randomUUID(), t.tenantId(), t.id(), cmd.body(),
                UUID.fromString(principal.userId()), principal.fullName(), role);
        posts.save(p);
        t.bumpReply();
        events.publishEvent(new ForumReplyPosted(UUID.randomUUID(), tenant.current(),
                t.forumId(), t.id(), p.id(), t.authorId(), p.authorId(), role, Instant.now()));
        return toPost(p);
    }

    /**
     * Toggle a user's upvote on a thread or post. Idempotent in intent — calling twice
     * cancels out, which lets a client swallow retries without double-counting.
     */
    public ThreadResponse toggleThreadUpvote(UUID threadId) {
        ForumThread t = requireOwnedThread(threadId);
        UUID userId = UUID.fromString(currentPrincipal().userId());
        var key = new ForumUpvote.PK(TARGET_THREAD, threadId, userId);
        var existing = upvotes.findById(key);
        if (existing.isPresent()) {
            upvotes.deleteById(key);
            t.decrementUpvote();
        } else {
            upvotes.save(new ForumUpvote(TARGET_THREAD, threadId, userId));
            t.incrementUpvote();
        }
        return toThread(t);
    }

    public PostResponse togglePostUpvote(UUID postId) {
        ForumPost p = posts.findById(postId).orElseThrow(NotFoundException::new);
        if (!p.tenantId().equals(tenant.current().value())) throw new NotFoundException();
        UUID userId = UUID.fromString(currentPrincipal().userId());
        var key = new ForumUpvote.PK(TARGET_POST, postId, userId);
        var existing = upvotes.findById(key);
        if (existing.isPresent()) {
            upvotes.deleteById(key);
            p.decrementUpvote();
        } else {
            upvotes.save(new ForumUpvote(TARGET_POST, postId, userId));
            p.incrementUpvote();
        }
        return toPost(p);
    }

    public ThreadResponse moderateThread(UUID threadId, ModerationFlags flags) {
        assertTeacherOrAdmin();
        ForumThread t = requireOwnedThread(threadId);
        if (flags.pinned() != null) t.pin(flags.pinned());
        if (flags.hidden() != null) t.hide(flags.hidden());
        return toThread(t);
    }

    public PostResponse moderatePost(UUID postId, ModerationFlags flags) {
        assertTeacherOrAdmin();
        ForumPost p = posts.findById(postId).orElseThrow(NotFoundException::new);
        if (!p.tenantId().equals(tenant.current().value())) throw new NotFoundException();
        if (flags.hidden() != null) p.hide(flags.hidden());
        if (flags.markedVerified() != null) p.markVerified(flags.markedVerified());
        return toPost(p);
    }

    private void assertTeacherOrAdmin() {
        var principal = currentPrincipal();
        if (principal.roles().stream().noneMatch(r ->
                r.equals(Roles.TEACHER) || r.equals(Roles.DIRECTOR) || r.equals(Roles.ADMIN))) {
            throw new ApplicationException(HttpStatus.FORBIDDEN, "error.forbidden");
        }
    }

    private Forum requireOwnedForum(UUID forumId) {
        Forum f = forums.findById(forumId).orElseThrow(NotFoundException::new);
        if (!f.tenantId().equals(tenant.current().value())) throw new NotFoundException();
        return f;
    }

    private ForumThread requireOwnedThread(UUID threadId) {
        ForumThread t = threads.findById(threadId).orElseThrow(NotFoundException::new);
        if (!t.tenantId().equals(tenant.current().value())) throw new NotFoundException();
        return t;
    }

    private SkoolPrincipal currentPrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SkoolPrincipal p)) {
            throw new ApplicationException(HttpStatus.UNAUTHORIZED, "error.unauthorized");
        }
        return p;
    }

    private ForumResponse toForum(Forum f) {
        return new ForumResponse(f.id(), f.subjectId(), f.turmaId(), f.academicYearId(),
                f.title(), f.description(), f.createdAt());
    }

    private ThreadResponse toThread(ForumThread t) {
        return new ThreadResponse(t.id(), t.forumId(), t.title(), t.body(), t.authorId(),
                t.authorName(), t.pinned(), t.hidden(), t.upvoteCount(), t.replyCount(),
                t.lastActivityAt(), t.createdAt());
    }

    private PostResponse toPost(ForumPost p) {
        return new PostResponse(p.id(), p.threadId(), p.body(), p.authorId(), p.authorName(),
                p.authorRole(), p.upvoteCount(), p.hidden(), p.markedVerified(), p.createdAt());
    }
}
