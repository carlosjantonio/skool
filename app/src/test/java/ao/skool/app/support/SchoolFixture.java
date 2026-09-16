package ao.skool.app.support;

import ao.skool.academic_structure.internal.domain.CurricularTrack;
import ao.skool.academic_structure.internal.domain.GradeLevel;
import ao.skool.academic_structure.internal.domain.TrimesterKey;
import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.AcademicYearResponse;
import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.CreateAcademicYear;
import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.TrimesterInput;
import ao.skool.academic_structure.internal.web.dto.SubjectDtos.CreateSubject;
import ao.skool.academic_structure.internal.web.dto.SubjectDtos.SubjectResponse;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.CreateTurma;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.TurmaResponse;
import ao.skool.sis.api.StudentDirectory;
import ao.skool.sis.internal.domain.Sex;
import ao.skool.sis.internal.web.dto.EnrollmentDtos.CreateEnrollment;
import ao.skool.sis.internal.web.dto.EnrollmentDtos.EnrollmentResponse;
import ao.skool.sis.internal.web.dto.StudentDtos.CreateStudent;
import ao.skool.sis.internal.web.dto.StudentDtos.ProvisionStudentUser;
import ao.skool.sis.internal.web.dto.StudentDtos.StudentResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds the school graph every phase-2-and-later test needs — academic year,
 * subject, turma, student, enrolment — by calling the real endpoints rather than
 * inserting rows. That way the fixture itself is a smoke test of Phase 1 and 2:
 * if creating a turma breaks, every downstream test fails loudly instead of
 * silently testing against hand-built state that production never produces.
 * <p>
 * Tests may reference the DTOs from {@code internal.web.dto} — the module-boundary
 * rule is about production code (the ArchUnit importer excludes test classes), and
 * {@code DemoDataInitializer} sets the same precedent for the app module.
 */
public abstract class SchoolFixture extends ApiTestBase {

    /** Keeps subject/turma codes unique across an H2 database shared by the whole suite. */
    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired protected StudentDirectory studentDirectory;

    protected AcademicYearResponse createAcademicYear(Actor as) throws Exception {
        int year = 2025;
        var cmd = new CreateAcademicYear(
                "Ano " + year + "-" + SEQ.incrementAndGet(),
                LocalDate.of(year, 9, 1),
                LocalDate.of(year + 1, 7, 31),
                java.util.List.of(
                        new TrimesterInput(TrimesterKey.T1, LocalDate.of(year, 9, 1), LocalDate.of(year, 12, 15)),
                        new TrimesterInput(TrimesterKey.T2, LocalDate.of(year + 1, 1, 8), LocalDate.of(year + 1, 4, 5)),
                        new TrimesterInput(TrimesterKey.T3, LocalDate.of(year + 1, 4, 15), LocalDate.of(year + 1, 7, 31))));
        return post(as, "/api/academic-years", cmd, AcademicYearResponse.class);
    }

    protected SubjectResponse createSubject(Actor as, String name) throws Exception {
        return createSubject(as, name, GradeLevel.CLASSE_10);
    }

    protected SubjectResponse createSubject(Actor as, String name, GradeLevel level) throws Exception {
        String code = name.substring(0, Math.min(3, name.length())).toUpperCase() + SEQ.incrementAndGet();
        return post(as, "/api/subjects", new CreateSubject(name, code, level), SubjectResponse.class);
    }

    protected TurmaResponse createTurma(Actor as, UUID academicYearId) throws Exception {
        var cmd = new CreateTurma(UUID.randomUUID(), academicYearId,
                "10ª A-" + SEQ.incrementAndGet(), GradeLevel.CLASSE_10,
                CurricularTrack.CIENCIAS_FISICAS_BIOLOGICAS, 35);
        return post(as, "/api/turmas", cmd, TurmaResponse.class);
    }

    protected StudentResponse createStudent(Actor as, String fullName) throws Exception {
        var cmd = new CreateStudent(fullName, LocalDate.of(2009, 4, 12), Sex.M,
                null, "Maianga", "Rua Amílcar Cabral, 12", null);
        return post(as, "/api/students", cmd, StudentResponse.class);
    }

    protected EnrollmentResponse enrol(Actor as, UUID studentId, UUID academicYearId, UUID turmaId) throws Exception {
        return post(as, "/api/enrollments",
                new CreateEnrollment(studentId, academicYearId, turmaId), EnrollmentResponse.class);
    }

    /**
     * Gives the student a portal login and returns an {@link Actor} carrying that
     * user's id, so student-scoped endpoints (which resolve the student from the
     * token, never from the URL) can be exercised.
     */
    protected Actor portalLoginFor(Actor admin, UUID studentId, String fullName) throws Exception {
        String email = "aluno" + SEQ.incrementAndGet() + "@escola.test";
        post(admin, "/api/students/" + studentId + "/portal-user",
                new ProvisionStudentUser(email), StudentResponse.class);
        // findStudent is tenant-scoped, so it has to run with the tenant bound.
        UUID userId = inRequestScope(admin, () -> studentDirectory.findStudent(studentId))
                .map(StudentDirectory.StudentSummary::userId)
                .orElseThrow(() -> new AssertionError("student " + studentId + " not found after provisioning"));
        if (userId == null) {
            throw new AssertionError("portal-user did not link a user to student " + studentId);
        }
        return actor(userId, fullName, "STUDENT");
    }

    /** Convenience: student profile + enrolment + portal login in one call. */
    protected EnrolledStudentFixture enrolledStudent(Actor admin, String fullName,
                                                      UUID academicYearId, UUID turmaId) throws Exception {
        StudentResponse student = createStudent(admin, fullName);
        enrol(admin, student.id(), academicYearId, turmaId);
        Actor login = portalLoginFor(admin, student.id(), fullName);
        return new EnrolledStudentFixture(student.id(), fullName, login);
    }

    protected record EnrolledStudentFixture(UUID studentId, String fullName, Actor login) {}
}
