package ao.skool.app.phase2;

import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.AcademicYearResponse;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.TurmaResponse;
import ao.skool.app.support.SchoolFixture;
import ao.skool.sis.api.StudentDirectory;
import ao.skool.sis.api.event.StudentEnrolled;
import ao.skool.sis.internal.domain.EnrollmentStatus;
import ao.skool.sis.internal.domain.GuardianRelationship;
import ao.skool.sis.internal.domain.Sex;
import ao.skool.sis.internal.web.dto.EnrollmentDtos.CreateEnrollment;
import ao.skool.sis.internal.web.dto.EnrollmentDtos.EnrollmentResponse;
import ao.skool.sis.internal.web.dto.GuardianDtos.CreateGuardian;
import ao.skool.sis.internal.web.dto.GuardianDtos.GuardianResponse;
import ao.skool.sis.internal.web.dto.StudentDtos.CreateStudent;
import ao.skool.sis.internal.web.dto.StudentDtos.GuardianSummary;
import ao.skool.sis.internal.web.dto.StudentDtos.LinkGuardian;
import ao.skool.sis.internal.web.dto.StudentDtos.ProvisionStudentUser;
import ao.skool.sis.internal.web.dto.StudentDtos.StudentResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2 — student records, guardians, matrícula, and the identity hand-off that
 * turns a guardian or student into a portal login.
 */
class SisTest extends SchoolFixture {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private String freshEmail(String prefix) {
        return prefix + SEQ.incrementAndGet() + "@encarregado.test";
    }

    @Test
    @DisplayName("a student is created with an Angolan address and optional BI")
    void createsStudent() throws Exception {
        Actor admin = admin();
        var cmd = new CreateStudent("João Baptista", LocalDate.of(2009, 4, 12), Sex.M,
                "001234567LA042", "Maianga", "Rua Amílcar Cabral, 12", "Asmático");

        StudentResponse student = post(admin, "/api/students", cmd, StudentResponse.class);

        assertThat(student.id()).isNotNull();
        assertThat(student.fullName()).isEqualTo("João Baptista");
        assertThat(student.bi()).isEqualTo("001234567LA042");
        assertThat(student.comunaOuBairro()).isEqualTo("Maianga");
        assertThat(student.healthNotes()).isEqualTo("Asmático");
        assertThat(student.active()).isTrue();
        assertThat(student.guardians()).isEmpty();
    }

    @Test
    @DisplayName("BI is optional — many students do not have one yet")
    void biIsOptional() throws Exception {
        StudentResponse student = createStudent(admin(), "Sem Bilhete");
        assertThat(student.bi()).isNull();
    }

    @Test
    @DisplayName("a future date of birth is rejected")
    void rejectsFutureDateOfBirth() throws Exception {
        var cmd = new CreateStudent("Futuro", LocalDate.now().plusDays(1), Sex.F,
                null, "Maianga", "Rua X", null);

        assertThat(postStatus(admin(), "/api/students", cmd)).isEqualTo(400);
    }

    @Test
    @DisplayName("a guardian links to a student with an Angolan relationship")
    void linksGuardianToStudent() throws Exception {
        Actor admin = admin();
        StudentResponse student = createStudent(admin, "João Baptista");
        GuardianResponse guardian = post(admin, "/api/guardians",
                new CreateGuardian("Maria Baptista", "+244923000111", freshEmail("maria"),
                        null, false), GuardianResponse.class);

        StudentResponse linked = post(admin, "/api/students/" + student.id() + "/guardians",
                new LinkGuardian(guardian.id(), GuardianRelationship.MAE, true, true),
                StudentResponse.class);

        assertThat(linked.guardians()).hasSize(1);
        GuardianSummary summary = linked.guardians().getFirst();
        assertThat(summary.fullName()).isEqualTo("Maria Baptista");
        assertThat(summary.relationship()).isEqualTo(GuardianRelationship.MAE);
        assertThat(summary.primaryGuardian()).isTrue();
        assertThat(summary.phone()).isEqualTo("+244923000111");
    }

    @Test
    @DisplayName("a guardian with provisionPortalUser gets a login and can see their children")
    void guardianPortalUserSeesOwnChildrenOnly() throws Exception {
        Actor admin = admin();
        StudentResponse mine = createStudent(admin, "João Baptista");
        StudentResponse someoneElses = createStudent(admin, "Aluno Alheio");

        GuardianResponse guardian = post(admin, "/api/guardians",
                new CreateGuardian("Maria Baptista", "+244923000111", freshEmail("maria"),
                        null, true), GuardianResponse.class);
        assertThat(guardian.userId())
                .as("provisionPortalUser should have created an identity user")
                .isNotNull();

        post(admin, "/api/students/" + mine.id() + "/guardians",
                new LinkGuardian(guardian.id(), GuardianRelationship.MAE, true, true),
                StudentResponse.class);

        Actor asGuardian = guardian(guardian.userId());
        List<Object> children = get(asGuardian, "/api/guardians/me/children", new TypeReference<>() {});

        assertThat(json.valueToTree(children).findValuesAsText("id"))
                .containsExactly(mine.id().toString())
                .doesNotContain(someoneElses.id().toString());
    }

    @Test
    @DisplayName("a guardian without provisionPortalUser gets no login")
    void guardianPortalIsOptional() throws Exception {
        GuardianResponse guardian = post(admin(), "/api/guardians",
                new CreateGuardian("Sem Portal", "+244923000222", freshEmail("semportal"),
                        null, false), GuardianResponse.class);

        assertThat(guardian.userId()).isNull();
    }

    @Test
    @DisplayName("provisioning two logins on the same email is a conflict, not a duplicate user")
    void duplicateEmailIsAConflict() throws Exception {
        Actor admin = admin();
        String email = freshEmail("duplicada");

        post(admin, "/api/guardians",
                new CreateGuardian("Primeira", "+244923000333", email, null, true), GuardianResponse.class);

        assertThat(postStatus(admin, "/api/guardians",
                new CreateGuardian("Segunda", "+244923000444", email, null, true))).isEqualTo(409);
    }

    @Test
    @DisplayName("enrolling a student publishes StudentEnrolled and lands in ENROLLED")
    void enrolmentPublishesEvent() throws Exception {
        Actor admin = admin();
        AcademicYearResponse year = createAcademicYear(admin);
        TurmaResponse turma = createTurma(admin, year.id());
        StudentResponse student = createStudent(admin, "João Baptista");

        EnrollmentResponse enrolment = enrol(admin, student.id(), year.id(), turma.id());

        assertThat(enrolment.status()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(enrolment.enrolledAt()).isNotNull();
        assertThat(enrolment.withdrawnAt()).isNull();

        StudentEnrolled event = events.onlyOne(StudentEnrolled.class);
        assertThat(event.studentId()).isEqualTo(student.id());
        assertThat(event.turmaId()).isEqualTo(turma.id());
        assertThat(event.tenantId().value()).isEqualTo(tenant);
    }

    @Test
    @DisplayName("a student cannot be enrolled twice in the same academic year")
    void rejectsDoubleEnrolment() throws Exception {
        Actor admin = admin();
        AcademicYearResponse year = createAcademicYear(admin);
        TurmaResponse turma = createTurma(admin, year.id());
        StudentResponse student = createStudent(admin, "João Baptista");

        enrol(admin, student.id(), year.id(), turma.id());

        assertThat(postStatus(admin, "/api/enrollments",
                new CreateEnrollment(student.id(), year.id(), turma.id()))).isEqualTo(409);
        // Not even into a different turma of the same year.
        TurmaResponse otherTurma = createTurma(admin, year.id());
        assertThat(postStatus(admin, "/api/enrollments",
                new CreateEnrollment(student.id(), year.id(), otherTurma.id()))).isEqualTo(409);
    }

    @Test
    @DisplayName("the same student may enrol again in the following academic year")
    void allowsEnrolmentInAnotherYear() throws Exception {
        Actor admin = admin();
        AcademicYearResponse year1 = createAcademicYear(admin);
        AcademicYearResponse year2 = createAcademicYear(admin);
        StudentResponse student = createStudent(admin, "João Baptista");

        enrol(admin, student.id(), year1.id(), createTurma(admin, year1.id()).id());
        EnrollmentResponse second = enrol(admin, student.id(), year2.id(), createTurma(admin, year2.id()).id());

        assertThat(second.status()).isEqualTo(EnrollmentStatus.ENROLLED);
    }

    @Test
    @DisplayName("withdrawing stamps the date and drops the student from the enrolled roll")
    void withdrawalRemovesFromEnrolledRoll() throws Exception {
        Actor admin = admin();
        AcademicYearResponse year = createAcademicYear(admin);
        TurmaResponse turma = createTurma(admin, year.id());
        StudentResponse student = createStudent(admin, "João Baptista");
        EnrollmentResponse enrolment = enrol(admin, student.id(), year.id(), turma.id());

        assertThat(inRequestScope(admin, () -> studentDirectory.listEnrolledForYear(year.id())))
                .extracting(StudentDirectory.EnrolledStudent::studentId).contains(student.id());

        EnrollmentResponse withdrawn = post(admin,
                "/api/enrollments/" + enrolment.id() + "/withdraw", null, EnrollmentResponse.class);

        assertThat(withdrawn.status()).isEqualTo(EnrollmentStatus.WITHDRAWN);
        assertThat(withdrawn.withdrawnAt()).isNotNull();
        // Fees fans invoices out over this list — a withdrawn student must not be billed.
        assertThat(inRequestScope(admin, () -> studentDirectory.listEnrolledForYear(year.id())))
                .extracting(StudentDirectory.EnrolledStudent::studentId).doesNotContain(student.id());
    }

    @Test
    @DisplayName("the enrolment list can be narrowed to one turma")
    void listsEnrolmentsByTurma() throws Exception {
        Actor admin = admin();
        AcademicYearResponse year = createAcademicYear(admin);
        TurmaResponse turmaA = createTurma(admin, year.id());
        TurmaResponse turmaB = createTurma(admin, year.id());

        StudentResponse inA = createStudent(admin, "Aluno A");
        StudentResponse inB = createStudent(admin, "Aluno B");
        enrol(admin, inA.id(), year.id(), turmaA.id());
        enrol(admin, inB.id(), year.id(), turmaB.id());

        List<EnrollmentResponse> onlyA = get(admin,
                "/api/enrollments?academicYearId=" + year.id() + "&turmaId=" + turmaA.id(),
                new TypeReference<>() {});

        assertThat(onlyA).extracting(EnrollmentResponse::studentId)
                .containsExactly(inA.id())
                .doesNotContain(inB.id());
    }

    @Test
    @DisplayName("the enrolled roll carries turma and grade level so fees need no cross-module join")
    void enrolledRollCarriesTurmaAndGrade() throws Exception {
        Actor admin = admin();
        AcademicYearResponse year = createAcademicYear(admin);
        TurmaResponse turma = createTurma(admin, year.id());
        StudentResponse student = createStudent(admin, "João Baptista");
        enrol(admin, student.id(), year.id(), turma.id());

        var roll = inRequestScope(admin, () -> studentDirectory.listEnrolledForYear(year.id()));

        assertThat(roll).singleElement().satisfies(row -> {
            assertThat(row.studentId()).isEqualTo(student.id());
            assertThat(row.fullName()).isEqualTo("João Baptista");
            assertThat(row.turmaId()).isEqualTo(turma.id());
            assertThat(row.gradeLevel()).isEqualTo("CLASSE_10");
            assertThat(row.tenantId()).isEqualTo(tenant);
        });
    }

    @Test
    @DisplayName("a student portal login is provisioned once and links back to the profile")
    void provisionsStudentPortalUser() throws Exception {
        Actor admin = admin();
        StudentResponse student = createStudent(admin, "João Baptista");
        String email = freshEmail("joao");

        post(admin, "/api/students/" + student.id() + "/portal-user",
                new ProvisionStudentUser(email), StudentResponse.class);

        UUID userId = inRequestScope(admin, () -> studentDirectory.findStudent(student.id()))
                .orElseThrow().userId();
        assertThat(userId).isNotNull();
        // The reverse lookup is what every student-scoped endpoint depends on.
        assertThat(inRequestScope(admin, () -> studentDirectory.findStudentByUserId(userId)))
                .get().extracting(StudentDirectory.StudentSummary::id).isEqualTo(student.id());

        // Doing it twice is a conflict, not a second account.
        assertThat(postStatus(admin, "/api/students/" + student.id() + "/portal-user",
                new ProvisionStudentUser(freshEmail("joao-again")))).isEqualTo(409);
    }

    @Test
    @DisplayName("student records are staff-only — a guardian cannot browse the roll")
    void studentRollIsStaffOnly() throws Exception {
        Actor admin = admin();
        createStudent(admin, "João Baptista");

        assertThat(getStatus(guardian(UUID.randomUUID()), "/api/students")).isEqualTo(403);
        assertThat(getStatus(student(UUID.randomUUID()), "/api/students")).isEqualTo(403);
        // A teacher needs the roll to teach.
        assertThat(getStatus(teacher(), "/api/students")).isEqualTo(200);
    }
}
