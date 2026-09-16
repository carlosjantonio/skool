package ao.skool.app.phase4;

import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.AcademicYearResponse;
import ao.skool.academic_structure.internal.web.dto.SubjectDtos.SubjectResponse;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.TurmaResponse;
import ao.skool.app.support.SchoolFixture;
import ao.skool.forum.api.event.ForumReplyPosted;
import ao.skool.forum.internal.web.dto.ForumDtos.CreateForum;
import ao.skool.forum.internal.web.dto.ForumDtos.CreatePost;
import ao.skool.forum.internal.web.dto.ForumDtos.CreateThread;
import ao.skool.forum.internal.web.dto.ForumDtos.ForumResponse;
import ao.skool.forum.internal.web.dto.ForumDtos.ModerationFlags;
import ao.skool.forum.internal.web.dto.ForumDtos.PostResponse;
import ao.skool.forum.internal.web.dto.ForumDtos.ThreadDetail;
import ao.skool.forum.internal.web.dto.ForumDtos.ThreadResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 4 — subject forums: one per subject × turma × year, with threads, replies,
 * upvoting and teacher moderation.
 * <p>
 * Authorship is taken from the {@code name} claim in the JWT rather than by calling
 * into identity, so these tests double as proof that claim is actually wired through.
 */
class ForumTest extends SchoolFixture {

    private record Course(UUID subjectId, UUID turmaId, UUID yearId) {}

    private Course course(Actor admin) throws Exception {
        AcademicYearResponse year = createAcademicYear(admin);
        TurmaResponse turma = createTurma(admin, year.id());
        SubjectResponse subject = createSubject(admin, "Biologia");
        return new Course(subject.id(), turma.id(), year.id());
    }

    private ForumResponse createForum(Actor teacher, Course c) throws Exception {
        return post(teacher, "/api/forums",
                new CreateForum(c.subjectId(), c.turmaId(), c.yearId(),
                        "Dúvidas de Biologia", "Perguntas sobre a matéria"),
                ForumResponse.class);
    }

    private ThreadResponse createThread(Actor as, UUID forumId, String title, String body) throws Exception {
        return post(as, "/api/forums/" + forumId + "/threads", new CreateThread(title, body),
                ThreadResponse.class);
    }

    @Test
    @DisplayName("a forum is created once per subject × turma × year")
    void createsForumOncePerSubjectAndClass() throws Exception {
        Actor teacher = teacher();
        Course c = course(admin());

        ForumResponse forum = createForum(teacher, c);
        assertThat(forum.title()).isEqualTo("Dúvidas de Biologia");
        assertThat(forum.subjectId()).isEqualTo(c.subjectId());

        // A second forum for the same triple is a conflict.
        assertThat(postStatus(teacher, "/api/forums",
                new CreateForum(c.subjectId(), c.turmaId(), c.yearId(), "Duplicado", null)))
                .isEqualTo(409);

        assertThat(get(teacher, "/api/forums?turmaId=" + c.turmaId(),
                new TypeReference<List<ForumResponse>>() {}))
                .extracting(ForumResponse::id).containsExactly(forum.id());
    }

    @Test
    @DisplayName("a student opens a thread and it is attributed to them by name")
    void studentOpensThread() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        ForumResponse forum = createForum(teacher, c);

        ThreadResponse thread = createThread(student.login(), forum.id(),
                "Não percebi a mitose", "Alguém pode explicar a metáfase?");

        assertThat(thread.title()).isEqualTo("Não percebi a mitose");
        assertThat(thread.authorId()).isEqualTo(student.login().userId());
        assertThat(thread.authorName())
                .as("author name comes from the JWT's name claim, no identity lookup")
                .isEqualTo("João Baptista");
        assertThat(thread.replyCount()).isZero();
        assertThat(thread.upvoteCount()).isZero();
        assertThat(thread.pinned()).isFalse();
    }

    @Test
    @DisplayName("a teacher reply bumps the thread and publishes ForumReplyPosted")
    void teacherReplyPublishesEvent() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        ForumResponse forum = createForum(teacher, c);
        ThreadResponse thread = createThread(student.login(), forum.id(), "Dúvida", "Corpo da dúvida");

        events.clear();
        PostResponse reply = post(teacher, "/api/forums/threads/" + thread.id() + "/posts",
                new CreatePost("A metáfase é quando os cromossomas se alinham."), PostResponse.class);

        assertThat(reply.authorName()).isEqualTo("Ana Silva");
        assertThat(reply.authorRole()).isEqualTo("TEACHER");
        assertThat(reply.markedVerified()).isFalse();

        // Phase 6 turns this into "a teacher answered your question" for the student.
        ForumReplyPosted event = events.onlyOne(ForumReplyPosted.class);
        assertThat(event.threadId()).isEqualTo(thread.id());
        assertThat(event.postId()).isEqualTo(reply.id());
        assertThat(event.threadAuthorId()).isEqualTo(student.login().userId());
        assertThat(event.replyAuthorId()).isEqualTo(teacher.userId());
        assertThat(event.replyAuthorRole()).isEqualTo("TEACHER");

        ThreadDetail detail = get(student.login(), "/api/forums/threads/" + thread.id(), ThreadDetail.class);
        assertThat(detail.thread().replyCount()).isEqualTo(1);
        assertThat(detail.posts()).singleElement()
                .extracting(PostResponse::body).isEqualTo("A metáfase é quando os cromossomas se alinham.");
    }

    @Test
    @DisplayName("upvoting toggles — a repeated tap cancels rather than double-counting")
    void upvotingToggles() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        var peer = enrolledStudent(admin, "Aluno B", c.yearId(), c.turmaId());
        ForumResponse forum = createForum(teacher, c);
        ThreadResponse thread = createThread(student.login(), forum.id(), "Dúvida", "Corpo");

        ThreadResponse afterFirst = post(peer.login(), "/api/forums/threads/" + thread.id() + "/upvote",
                null, ThreadResponse.class);
        assertThat(afterFirst.upvoteCount()).isEqualTo(1);

        // Same user again — this must undo, so a retried request cannot inflate the count.
        ThreadResponse afterSecond = post(peer.login(), "/api/forums/threads/" + thread.id() + "/upvote",
                null, ThreadResponse.class);
        assertThat(afterSecond.upvoteCount()).isZero();

        // Two different users each count once.
        post(peer.login(), "/api/forums/threads/" + thread.id() + "/upvote", null, ThreadResponse.class);
        ThreadResponse afterTeacher = post(teacher, "/api/forums/threads/" + thread.id() + "/upvote",
                null, ThreadResponse.class);
        assertThat(afterTeacher.upvoteCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("post upvotes toggle independently of thread upvotes")
    void postUpvotesAreSeparate() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        ForumResponse forum = createForum(teacher, c);
        ThreadResponse thread = createThread(student.login(), forum.id(), "Dúvida", "Corpo");
        PostResponse reply = post(teacher, "/api/forums/threads/" + thread.id() + "/posts",
                new CreatePost("Resposta"), PostResponse.class);

        PostResponse upvoted = post(student.login(), "/api/forums/posts/" + reply.id() + "/upvote",
                null, PostResponse.class);
        assertThat(upvoted.upvoteCount()).isEqualTo(1);

        ThreadDetail detail = get(student.login(), "/api/forums/threads/" + thread.id(), ThreadDetail.class);
        assertThat(detail.thread().upvoteCount()).as("the thread's own count is untouched").isZero();
    }

    @Test
    @DisplayName("a teacher pins a thread and marks a reply as the verified answer")
    void teacherModeration() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        ForumResponse forum = createForum(teacher, c);
        ThreadResponse thread = createThread(student.login(), forum.id(), "Dúvida importante", "Corpo");
        PostResponse reply = post(teacher, "/api/forums/threads/" + thread.id() + "/posts",
                new CreatePost("A resposta certa."), PostResponse.class);

        ThreadResponse pinned = patch(teacher, "/api/forums/threads/" + thread.id() + "/moderation",
                new ModerationFlags(true, null, null), ThreadResponse.class);
        assertThat(pinned.pinned()).isTrue();

        PostResponse verified = patch(teacher, "/api/forums/posts/" + reply.id() + "/moderation",
                new ModerationFlags(null, null, true), PostResponse.class);
        assertThat(verified.markedVerified()).isTrue();

        // Pinned threads sort to the top of the list for the class.
        List<ThreadResponse> threads = get(student.login(), "/api/forums/" + forum.id() + "/threads",
                new TypeReference<>() {});
        assertThat(threads.getFirst().id()).isEqualTo(thread.id());
    }

    @Test
    @DisplayName("a hidden thread disappears from the class list; a hidden post from the thread")
    void moderationHidesContent() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        ForumResponse forum = createForum(teacher, c);
        ThreadResponse keep = createThread(student.login(), forum.id(), "Fica", "Corpo");
        ThreadResponse offensive = createThread(student.login(), forum.id(), "Desapropriado", "Corpo");
        PostResponse badPost = post(student.login(), "/api/forums/threads/" + keep.id() + "/posts",
                new CreatePost("Comentário desapropriado"), PostResponse.class);

        patch(teacher, "/api/forums/threads/" + offensive.id() + "/moderation",
                new ModerationFlags(null, true, null), ThreadResponse.class);
        patch(teacher, "/api/forums/posts/" + badPost.id() + "/moderation",
                new ModerationFlags(null, true, null), PostResponse.class);

        assertThat(get(student.login(), "/api/forums/" + forum.id() + "/threads",
                new TypeReference<List<ThreadResponse>>() {}))
                .extracting(ThreadResponse::id).containsExactly(keep.id());

        assertThat(get(student.login(), "/api/forums/threads/" + keep.id(), ThreadDetail.class).posts())
                .isEmpty();
    }

    @Test
    @DisplayName("students cannot moderate — that is the whole point of moderation")
    void studentsCannotModerate() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        ForumResponse forum = createForum(teacher, c);
        ThreadResponse thread = createThread(student.login(), forum.id(), "Dúvida", "Corpo");

        assertThat(patchStatus(student.login(), "/api/forums/threads/" + thread.id() + "/moderation",
                new ModerationFlags(true, null, null))).isEqualTo(403);
        // Nor may a student create the forum itself.
        assertThat(postStatus(student.login(), "/api/forums",
                new CreateForum(UUID.randomUUID(), c.turmaId(), c.yearId(), "Meu fórum", null)))
                .isEqualTo(403);
    }

    @Test
    @DisplayName("a forum from another school cannot be read or posted into")
    void forumsAreTenantScoped() throws Exception {
        Actor teacher = teacher();
        Course c = course(admin());
        ForumResponse forum = createForum(teacher, c);
        ThreadResponse thread = createThread(teacher, forum.id(), "Interno", "Corpo");

        Actor outsider = actorInOtherTenant("Professor Alheio", "TEACHER");

        assertThat(getStatus(outsider, "/api/forums/" + forum.id() + "/threads")).isEqualTo(404);
        assertThat(getStatus(outsider, "/api/forums/threads/" + thread.id())).isEqualTo(404);
        assertThat(postStatus(outsider, "/api/forums/threads/" + thread.id() + "/posts",
                new CreatePost("Intrusão"))).isEqualTo(404);
    }
}
