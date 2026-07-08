package ao.skool.forum.internal.web;

import ao.skool.common.security.Roles;
import ao.skool.forum.internal.service.ForumService;
import ao.skool.forum.internal.web.dto.ForumDtos.CreateForum;
import ao.skool.forum.internal.web.dto.ForumDtos.CreatePost;
import ao.skool.forum.internal.web.dto.ForumDtos.CreateThread;
import ao.skool.forum.internal.web.dto.ForumDtos.ForumResponse;
import ao.skool.forum.internal.web.dto.ForumDtos.ModerationFlags;
import ao.skool.forum.internal.web.dto.ForumDtos.PostResponse;
import ao.skool.forum.internal.web.dto.ForumDtos.ThreadDetail;
import ao.skool.forum.internal.web.dto.ForumDtos.ThreadResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/forums")
public class ForumController {

    private static final String ANY_LOGGED_IN = "hasAnyRole('" + Roles.STUDENT + "','" + Roles.TEACHER + "','"
            + Roles.DIRECTOR + "','" + Roles.SECRETARY + "','" + Roles.ADMIN + "','" + Roles.GUARDIAN + "')";
    private static final String TEACHER_ROLES = "hasAnyRole('" + Roles.TEACHER + "','" + Roles.DIRECTOR + "','" + Roles.ADMIN + "')";

    private final ForumService service;

    public ForumController(ForumService service) { this.service = service; }

    @PostMapping
    @PreAuthorize(TEACHER_ROLES)
    public ForumResponse createForum(@Valid @RequestBody CreateForum cmd) {
        return service.createForum(cmd);
    }

    @GetMapping
    @PreAuthorize(ANY_LOGGED_IN)
    public List<ForumResponse> listByTurma(@RequestParam UUID turmaId) {
        return service.listForumsByTurma(turmaId);
    }

    @GetMapping("/{forumId}/threads")
    @PreAuthorize(ANY_LOGGED_IN)
    public List<ThreadResponse> listThreads(@PathVariable UUID forumId) {
        return service.listThreads(forumId);
    }

    @PostMapping("/{forumId}/threads")
    @PreAuthorize(ANY_LOGGED_IN)
    public ThreadResponse createThread(@PathVariable UUID forumId, @Valid @RequestBody CreateThread cmd) {
        return service.createThread(forumId, cmd);
    }

    @GetMapping("/threads/{threadId}")
    @PreAuthorize(ANY_LOGGED_IN)
    public ThreadDetail getThread(@PathVariable UUID threadId) {
        return service.getThread(threadId);
    }

    @PostMapping("/threads/{threadId}/posts")
    @PreAuthorize(ANY_LOGGED_IN)
    public PostResponse createPost(@PathVariable UUID threadId, @Valid @RequestBody CreatePost cmd) {
        return service.createPost(threadId, cmd);
    }

    @PostMapping("/threads/{threadId}/upvote")
    @PreAuthorize(ANY_LOGGED_IN)
    public ThreadResponse toggleThreadUpvote(@PathVariable UUID threadId) {
        return service.toggleThreadUpvote(threadId);
    }

    @PostMapping("/posts/{postId}/upvote")
    @PreAuthorize(ANY_LOGGED_IN)
    public PostResponse togglePostUpvote(@PathVariable UUID postId) {
        return service.togglePostUpvote(postId);
    }

    @PatchMapping("/threads/{threadId}/moderation")
    @PreAuthorize(TEACHER_ROLES)
    public ThreadResponse moderateThread(@PathVariable UUID threadId, @RequestBody ModerationFlags flags) {
        return service.moderateThread(threadId, flags);
    }

    @PatchMapping("/posts/{postId}/moderation")
    @PreAuthorize(TEACHER_ROLES)
    public PostResponse moderatePost(@PathVariable UUID postId, @RequestBody ModerationFlags flags) {
        return service.moderatePost(postId, flags);
    }
}
