package ao.skool.app.phase4;

import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.AcademicYearResponse;
import ao.skool.academic_structure.internal.web.dto.SubjectDtos.SubjectResponse;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.TurmaResponse;
import ao.skool.app.support.SchoolFixture;
import ao.skool.assignments.api.event.AssignmentDue;
import ao.skool.assignments.internal.domain.AssignmentStatus;
import ao.skool.assignments.internal.domain.SubmissionStatus;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.AssignmentResponse;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.CreateAssignment;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.GradeSubmission;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.SubmissionResponse;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.SubmitAssignment;
import ao.skool.assignments.internal.web.dto.AssignmentDtos.TeacherAssignmentDetail;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 4 — the assignment loop: create → submit → grade → feedback.
 */
class AssignmentsTest extends SchoolFixture {

    private record Course(UUID subjectId, UUID turmaId, UUID yearId) {}

    private Course course(Actor admin) throws Exception {
        AcademicYearResponse year = createAcademicYear(admin);
        TurmaResponse turma = createTurma(admin, year.id());
        SubjectResponse subject = createSubject(admin, "Português");
        return new Course(subject.id(), turma.id(), year.id());
    }

    private CreateAssignment spec(Course c, Instant dueAt, Boolean allowLate) {
        return new CreateAssignment(c.subjectId(), c.turmaId(), c.yearId(), "T1",
                "Composição sobre a Independência",
                "Escreva 500 palavras.", "Estrutura 40%, ortografia 30%, argumentação 30%",
                new BigDecimal("20.00"), dueAt, allowLate);
    }

    private AssignmentResponse createAssignment(Actor teacher, Course c, Instant dueAt, Boolean allowLate)
            throws Exception {
        return post(teacher, "/api/assignments", spec(c, dueAt, allowLate), AssignmentResponse.class);
    }

    @Test
    @DisplayName("creating an assignment stores the rubric and fires AssignmentDue")
    void createsAssignment() throws Exception {
        Actor teacher = teacher();
        Course c = course(admin());
        Instant dueAt = Instant.now().plus(7, ChronoUnit.DAYS);

        events.clear();
        AssignmentResponse assignment = createAssignment(teacher, c, dueAt, true);

        assertThat(assignment.title()).isEqualTo("Composição sobre a Independência");
        assertThat(assignment.rubric()).contains("Estrutura 40%");
        assertThat(assignment.maxScore()).isEqualByComparingTo("20.00");
        assertThat(assignment.allowLate()).isTrue();
        assertThat(assignment.status()).isEqualTo(AssignmentStatus.OPEN);
        assertThat(assignment.submissionCount()).isZero();

        // Phase 6's notification module will consume this to send deadline reminders.
        AssignmentDue event = events.onlyOne(AssignmentDue.class);
        assertThat(event.assignmentId()).isEqualTo(assignment.id());
        assertThat(event.turmaId()).isEqualTo(c.turmaId());
        assertThat(event.dueAt()).isEqualTo(dueAt);
    }

    @Test
    @DisplayName("maxScore defaults to the Angolan 20-point scale")
    void maxScoreDefaultsToTwenty() throws Exception {
        Actor teacher = teacher();
        Course c = course(admin());

        AssignmentResponse assignment = post(teacher, "/api/assignments",
                new CreateAssignment(c.subjectId(), c.turmaId(), c.yearId(), "T1", "Sem nota máxima",
                        null, null, null, Instant.now().plus(1, ChronoUnit.DAYS), null),
                AssignmentResponse.class);

        assertThat(assignment.maxScore()).isEqualByComparingTo("20.00");
        assertThat(assignment.allowLate()).as("late submissions are allowed unless opted out").isTrue();
    }

    @Test
    @DisplayName("a student submits, sees their own submission, and can replace it")
    void submitAndResubmit() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        AssignmentResponse assignment = createAssignment(teacher, c, Instant.now().plus(7, ChronoUnit.DAYS), true);

        SubmissionResponse first = post(student.login(),
                "/api/student/assignments/" + assignment.id() + "/submit",
                new SubmitAssignment(null, "Primeira versão."), SubmissionResponse.class);

        assertThat(first.studentId()).isEqualTo(student.studentId());
        assertThat(first.notes()).isEqualTo("Primeira versão.");
        assertThat(first.isLate()).isFalse();
        assertThat(first.status()).isEqualTo(SubmissionStatus.SUBMITTED);

        UUID documentId = UUID.randomUUID();
        SubmissionResponse second = post(student.login(),
                "/api/student/assignments/" + assignment.id() + "/submit",
                new SubmitAssignment(documentId, "Versão corrigida."), SubmissionResponse.class);

        assertThat(second.id()).as("re-submitting replaces the row, it does not add one").isEqualTo(first.id());
        assertThat(second.notes()).isEqualTo("Versão corrigida.");
        assertThat(second.documentId()).isEqualTo(documentId);

        SubmissionResponse mine = get(student.login(),
                "/api/student/assignments/" + assignment.id() + "/mine", SubmissionResponse.class);
        assertThat(mine.id()).isEqualTo(first.id());
        assertThat(mine.notes()).isEqualTo("Versão corrigida.");

        // One student, one submission — the teacher's roll should say the same.
        assertThat(get(teacher, "/api/assignments/" + assignment.id(), AssignmentResponse.class)
                .submissionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("asking for a submission that was never made is a 404")
    void missingSubmissionIs404() throws Exception {
        Actor admin = admin();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        AssignmentResponse assignment = createAssignment(teacher(), c, Instant.now().plus(1, ChronoUnit.DAYS), true);

        assertThat(getStatus(student.login(), "/api/student/assignments/" + assignment.id() + "/mine"))
                .isEqualTo(404);
    }

    @Test
    @DisplayName("a submission after the deadline is flagged late when late work is allowed")
    void lateSubmissionIsFlagged() throws Exception {
        Actor admin = admin();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        AssignmentResponse assignment = createAssignment(teacher(), c,
                Instant.now().minus(1, ChronoUnit.DAYS), true);

        SubmissionResponse submission = post(student.login(),
                "/api/student/assignments/" + assignment.id() + "/submit",
                new SubmitAssignment(null, "Atrasado, peço desculpa."), SubmissionResponse.class);

        assertThat(submission.isLate()).isTrue();
    }

    @Test
    @DisplayName("a submission after the deadline is refused when late work is not allowed")
    void lateSubmissionRejectedWhenDisallowed() throws Exception {
        Actor admin = admin();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        AssignmentResponse assignment = createAssignment(teacher(), c,
                Instant.now().minus(1, ChronoUnit.DAYS), false);

        assertThat(postStatus(student.login(), "/api/student/assignments/" + assignment.id() + "/submit",
                new SubmitAssignment(null, "Tarde demais."))).isEqualTo(409);
    }

    @Test
    @DisplayName("the teacher grades a submission with feedback and the student sees it")
    void gradeWithFeedback() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        AssignmentResponse assignment = createAssignment(teacher, c, Instant.now().plus(7, ChronoUnit.DAYS), true);

        SubmissionResponse submitted = post(student.login(),
                "/api/student/assignments/" + assignment.id() + "/submit",
                new SubmitAssignment(null, "A minha composição."), SubmissionResponse.class);

        SubmissionResponse graded = post(teacher,
                "/api/assignments/submissions/" + submitted.id() + "/grade",
                new GradeSubmission(new BigDecimal("16.50"), "Boa estrutura, cuidado com a ortografia."),
                SubmissionResponse.class);

        assertThat(graded.score()).isEqualByComparingTo("16.50");
        assertThat(graded.feedback()).isEqualTo("Boa estrutura, cuidado com a ortografia.");
        assertThat(graded.gradedAt()).isNotNull();
        assertThat(graded.status()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(graded.studentName()).isEqualTo("João Baptista");

        SubmissionResponse asStudentSeesIt = get(student.login(),
                "/api/student/assignments/" + assignment.id() + "/mine", SubmissionResponse.class);
        assertThat(asStudentSeesIt.score()).isEqualByComparingTo("16.50");
        assertThat(asStudentSeesIt.feedback()).isEqualTo("Boa estrutura, cuidado com a ortografia.");
    }

    @Test
    @DisplayName("the teacher's detail view lists every submission with the student's name")
    void teacherDetailListsSubmissions() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var a = enrolledStudent(admin, "Aluno A", c.yearId(), c.turmaId());
        var b = enrolledStudent(admin, "Aluno B", c.yearId(), c.turmaId());
        AssignmentResponse assignment = createAssignment(teacher, c, Instant.now().plus(7, ChronoUnit.DAYS), true);

        post(a.login(), "/api/student/assignments/" + assignment.id() + "/submit",
                new SubmitAssignment(null, "A"), SubmissionResponse.class);
        post(b.login(), "/api/student/assignments/" + assignment.id() + "/submit",
                new SubmitAssignment(null, "B"), SubmissionResponse.class);

        TeacherAssignmentDetail detail = get(teacher,
                "/api/assignments/" + assignment.id() + "/detail", TeacherAssignmentDetail.class);

        assertThat(detail.assignment().submissionCount()).isEqualTo(2);
        assertThat(detail.submissions()).extracting(SubmissionResponse::studentName)
                .containsExactlyInAnyOrder("Aluno A", "Aluno B");
    }

    @Test
    @DisplayName("a student's own list shows only their submissions")
    void studentSeesOnlyOwnSubmissions() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var mine = enrolledStudent(admin, "Aluno A", c.yearId(), c.turmaId());
        var theirs = enrolledStudent(admin, "Aluno B", c.yearId(), c.turmaId());
        AssignmentResponse assignment = createAssignment(teacher, c, Instant.now().plus(7, ChronoUnit.DAYS), true);

        post(mine.login(), "/api/student/assignments/" + assignment.id() + "/submit",
                new SubmitAssignment(null, "A"), SubmissionResponse.class);
        post(theirs.login(), "/api/student/assignments/" + assignment.id() + "/submit",
                new SubmitAssignment(null, "B"), SubmissionResponse.class);

        List<SubmissionResponse> list = get(mine.login(), "/api/student/assignments/mine",
                new TypeReference<>() {});

        assertThat(list).singleElement()
                .extracting(SubmissionResponse::studentId).isEqualTo(mine.studentId());
    }

    @Test
    @DisplayName("closing an assignment marks it closed")
    void closesAssignment() throws Exception {
        Actor teacher = teacher();
        Course c = course(admin());
        AssignmentResponse assignment = createAssignment(teacher, c, Instant.now().plus(1, ChronoUnit.DAYS), true);

        AssignmentResponse closed = post(teacher, "/api/assignments/" + assignment.id() + "/close",
                null, AssignmentResponse.class);

        assertThat(closed.status()).isEqualTo(AssignmentStatus.CLOSED);
    }

    @Test
    @DisplayName("an assignment from another school is invisible, not just unauthorised")
    void crossTenantAssignmentIsNotFound() throws Exception {
        Actor teacher = teacher();
        Course c = course(admin());
        AssignmentResponse assignment = createAssignment(teacher, c, Instant.now().plus(1, ChronoUnit.DAYS), true);

        Actor outsider = actorInOtherTenant("Professor de Outra Escola", "TEACHER");

        assertThat(getStatus(outsider, "/api/assignments/" + assignment.id())).isEqualTo(404);
    }

    @Test
    @DisplayName("students cannot create or grade assignments")
    void authoringIsTeacherOnly() throws Exception {
        Actor admin = admin();
        Actor teacher = teacher();
        Course c = course(admin);
        var student = enrolledStudent(admin, "João Baptista", c.yearId(), c.turmaId());
        AssignmentResponse assignment = createAssignment(teacher, c, Instant.now().plus(1, ChronoUnit.DAYS), true);
        SubmissionResponse submitted = post(student.login(),
                "/api/student/assignments/" + assignment.id() + "/submit",
                new SubmitAssignment(null, "A minha."), SubmissionResponse.class);

        assertThat(postStatus(student.login(), "/api/assignments",
                spec(c, Instant.now().plus(1, ChronoUnit.DAYS), true))).isEqualTo(403);
        assertThat(postStatus(student.login(),
                "/api/assignments/submissions/" + submitted.id() + "/grade",
                new GradeSubmission(new BigDecimal("20.00"), "Dou-me 20."))).isEqualTo(403);
        assertThat(getStatus(student.login(), "/api/assignments/" + assignment.id() + "/detail"))
                .isEqualTo(403);
    }
}
