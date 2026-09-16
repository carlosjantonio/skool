package ao.skool.app.phase4;

import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.AcademicYearResponse;
import ao.skool.academic_structure.internal.web.dto.SubjectDtos.SubjectResponse;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.TurmaResponse;
import ao.skool.app.support.SchoolFixture;
import ao.skool.subject_board.api.event.AnnouncementPosted;
import ao.skool.subject_board.internal.domain.BoardEntryKind;
import ao.skool.subject_board.internal.web.dto.BoardDtos.CreateEntry;
import ao.skool.subject_board.internal.web.dto.BoardDtos.EntryResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 4 — the subject board: announcements, materials and links, with the
 * low-bandwidth flag the PWA uses to decide what not to prefetch on a slow connection.
 */
class SubjectBoardTest extends SchoolFixture {

    private record Course(UUID subjectId, UUID turmaId, UUID yearId) {}

    private Course course(Actor admin) throws Exception {
        AcademicYearResponse year = createAcademicYear(admin);
        TurmaResponse turma = createTurma(admin, year.id());
        SubjectResponse subject = createSubject(admin, "Física");
        return new Course(subject.id(), turma.id(), year.id());
    }

    private CreateEntry announcement(Course c, String title, String body) {
        return new CreateEntry(c.subjectId(), c.turmaId(), c.yearId(), BoardEntryKind.ANNOUNCEMENT,
                title, body, null, null, null, null, null);
    }

    @Test
    @DisplayName("an announcement is posted and publishes AnnouncementPosted")
    void postsAnnouncement() throws Exception {
        Actor teacher = teacher();
        Course c = course(admin());

        events.clear();
        EntryResponse entry = post(teacher, "/api/board",
                announcement(c, "Teste na próxima semana", "Estudem os capítulos 3 e 4."),
                EntryResponse.class);

        assertThat(entry.kind()).isEqualTo(BoardEntryKind.ANNOUNCEMENT);
        assertThat(entry.title()).isEqualTo("Teste na próxima semana");
        assertThat(entry.authorName()).isEqualTo("Ana Silva");
        assertThat(entry.authorId()).isEqualTo(teacher.userId());
        assertThat(entry.publishedAt()).isNotNull();
        assertThat(entry.lowBandwidth()).isFalse();

        AnnouncementPosted event = events.onlyOne(AnnouncementPosted.class);
        assertThat(event.entryId()).isEqualTo(entry.id());
        assertThat(event.turmaId()).isEqualTo(c.turmaId());
    }

    @Test
    @DisplayName("a material entry needs a document, a link entry needs a URL")
    void enforcesPerKindRequirements() throws Exception {
        Actor teacher = teacher();
        Course c = course(admin());

        assertThat(postStatus(teacher, "/api/board",
                new CreateEntry(c.subjectId(), c.turmaId(), c.yearId(), BoardEntryKind.MATERIAL,
                        "Slides", null, null, null, null, null, null)))
                .as("a MATERIAL with no documentId is meaningless").isEqualTo(400);
        assertThat(postStatus(teacher, "/api/board",
                new CreateEntry(c.subjectId(), c.turmaId(), c.yearId(), BoardEntryKind.LINK,
                        "Vídeo", null, null, null, null, null, null)))
                .as("a LINK with no externalUrl is meaningless").isEqualTo(400);
        assertThat(postStatus(teacher, "/api/board",
                announcement(c, "Sem corpo", null)))
                .as("an ANNOUNCEMENT with no body is meaningless").isEqualTo(400);

        // The valid forms go through.
        assertThat(postStatus(teacher, "/api/board",
                new CreateEntry(c.subjectId(), c.turmaId(), c.yearId(), BoardEntryKind.MATERIAL,
                        "Slides", null, UUID.randomUUID(), null, null, null, null))).isEqualTo(200);
        assertThat(postStatus(teacher, "/api/board",
                new CreateEntry(c.subjectId(), c.turmaId(), c.yearId(), BoardEntryKind.LINK,
                        "Vídeo", null, null, "https://exemplo.ao/aula", null, null, null))).isEqualTo(200);
    }

    @Test
    @DisplayName("a due date makes the entry part of the deadline-aware feed")
    void carriesDueDate() throws Exception {
        Actor teacher = teacher();
        Course c = course(admin());
        Instant dueAt = Instant.now().plus(3, ChronoUnit.DAYS);

        EntryResponse entry = post(teacher, "/api/board",
                new CreateEntry(c.subjectId(), c.turmaId(), c.yearId(), BoardEntryKind.ANNOUNCEMENT,
                        "Entrega do trabalho", "Não se esqueçam.", null, null, dueAt, null, null),
                EntryResponse.class);

        assertThat(entry.dueAt()).isEqualTo(dueAt);
    }

    @Test
    @DisplayName("the low-bandwidth flag rides through so the PWA can skip prefetching")
    void carriesLowBandwidthFlag() throws Exception {
        Actor teacher = teacher();
        Course c = course(admin());

        EntryResponse heavy = post(teacher, "/api/board",
                new CreateEntry(c.subjectId(), c.turmaId(), c.yearId(), BoardEntryKind.MATERIAL,
                        "Vídeo da aula (200 MB)", null, UUID.randomUUID(), null, null, null, true),
                EntryResponse.class);

        assertThat(heavy.lowBandwidth()).isTrue();
    }

    @Test
    @DisplayName("pinned entries sort to the top of the class feed")
    void pinnedEntriesComeFirst() throws Exception {
        Actor teacher = teacher();
        Course c = course(admin());

        post(teacher, "/api/board", announcement(c, "Normal 1", "corpo"), EntryResponse.class);
        post(teacher, "/api/board", announcement(c, "Normal 2", "corpo"), EntryResponse.class);
        EntryResponse pinned = post(teacher, "/api/board",
                new CreateEntry(c.subjectId(), c.turmaId(), c.yearId(), BoardEntryKind.ANNOUNCEMENT,
                        "Regras da disciplina", "Leiam primeiro.", null, null, null, true, null),
                EntryResponse.class);

        List<EntryResponse> feed = get(teacher, "/api/board?turmaId=" + c.turmaId(),
                new TypeReference<>() {});

        assertThat(feed).hasSize(3);
        assertThat(feed.getFirst().id()).isEqualTo(pinned.id());
        assertThat(feed.getFirst().pinned()).isTrue();
    }

    @Test
    @DisplayName("the feed can be narrowed to one subject or show the whole turma")
    void feedFiltersBySubject() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        SubjectResponse otherSubject = createSubject(admin, "Química");

        EntryResponse fisica = post(teacher, "/api/board", announcement(c, "Aviso de Física", "corpo"),
                EntryResponse.class);
        EntryResponse quimica = post(teacher, "/api/board",
                new CreateEntry(otherSubject.id(), c.turmaId(), c.yearId(), BoardEntryKind.ANNOUNCEMENT,
                        "Aviso de Química", "corpo", null, null, null, null, null),
                EntryResponse.class);

        assertThat(get(teacher, "/api/board?turmaId=" + c.turmaId(),
                new TypeReference<List<EntryResponse>>() {}))
                .extracting(EntryResponse::id).containsExactlyInAnyOrder(fisica.id(), quimica.id());

        assertThat(get(teacher, "/api/board?turmaId=" + c.turmaId() + "&subjectId=" + c.subjectId(),
                new TypeReference<List<EntryResponse>>() {}))
                .extracting(EntryResponse::id).containsExactly(fisica.id());
    }

    @Test
    @DisplayName("students read the board; only teachers write and delete it")
    void boardIsReadableByStudentsAndWritableByTeachers() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        EntryResponse entry = post(teacher, "/api/board", announcement(c, "Aviso", "corpo"),
                EntryResponse.class);

        assertThat(get(student.login(), "/api/board?turmaId=" + c.turmaId(),
                new TypeReference<List<EntryResponse>>() {}))
                .extracting(EntryResponse::id).containsExactly(entry.id());

        assertThat(postStatus(student.login(), "/api/board", announcement(c, "Aviso falso", "corpo")))
                .isEqualTo(403);
        assertThat(deleteStatus(student.login(), "/api/board/" + entry.id())).isEqualTo(403);

        assertThat(deleteStatus(teacher, "/api/board/" + entry.id())).isEqualTo(200);
        assertThat(get(teacher, "/api/board?turmaId=" + c.turmaId(),
                new TypeReference<List<EntryResponse>>() {})).isEmpty();
    }

    @Test
    @DisplayName("another school's board entry cannot be deleted")
    void deleteIsTenantScoped() throws Exception {
        Actor teacher = teacher();
        Course c = course(admin());
        EntryResponse entry = post(teacher, "/api/board", announcement(c, "Aviso", "corpo"),
                EntryResponse.class);

        Actor outsider = actorInOtherTenant("Professor Alheio", "TEACHER");

        assertThat(deleteStatus(outsider, "/api/board/" + entry.id())).isEqualTo(404);
    }
}
