package ao.skool.app.phase2;

import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.AcademicYearResponse;
import ao.skool.academic_structure.internal.web.dto.SubjectDtos.SubjectResponse;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.TurmaResponse;
import ao.skool.app.support.SchoolFixture;
import ao.skool.staff.internal.domain.AssignmentRole;
import ao.skool.staff.internal.web.dto.StaffDtos.AssignmentResponse;
import ao.skool.staff.internal.web.dto.StaffDtos.CreateAssignment;
import ao.skool.staff.internal.web.dto.StaffDtos.CreateStaff;
import ao.skool.staff.internal.web.dto.StaffDtos.StaffResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 2 — staff records, portal provisioning, and the teacher × subject × turma
 * assignment that the whole teacher UI is built on.
 */
class StaffTest extends SchoolFixture {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private String freshEmail() {
        return "professor" + SEQ.incrementAndGet() + "@escola.test";
    }

    private StaffResponse createTeacherRecord(Actor admin, boolean withPortal) throws Exception {
        return post(admin, "/api/staff",
                new CreateStaff("Ana Silva", "001234567LA042", "0012345678", "+244923111222",
                        freshEmail(), "Licenciatura em Matemática", LocalDate.of(2019, 2, 1), withPortal),
                StaffResponse.class);
    }

    @Test
    @DisplayName("a staff record keeps BI, NIF, qualification and hire date")
    void createsStaffRecord() throws Exception {
        StaffResponse staff = createTeacherRecord(admin(), false);

        assertThat(staff.fullName()).isEqualTo("Ana Silva");
        assertThat(staff.bi()).isEqualTo("001234567LA042");
        assertThat(staff.nif()).isEqualTo("0012345678");
        assertThat(staff.qualification()).isEqualTo("Licenciatura em Matemática");
        assertThat(staff.hireDate()).isEqualTo(LocalDate.of(2019, 2, 1));
        assertThat(staff.active()).isTrue();
        assertThat(staff.userId()).as("no portal opt-in means no login").isNull();
    }

    @Test
    @DisplayName("opting into the portal provisions a TEACHER login")
    void provisionsTeacherPortalUser() throws Exception {
        StaffResponse staff = createTeacherRecord(admin(), true);

        assertThat(staff.userId()).isNotNull();

        // That login resolves back to the staff record — the teacher UI's first call.
        Actor asTeacher = actor(staff.userId(), "Ana Silva", "TEACHER");
        StaffResponse me = get(asTeacher, "/api/staff/me", StaffResponse.class);
        assertThat(me.id()).isEqualTo(staff.id());
    }

    @Test
    @DisplayName("assignments bind a teacher to a subject in a turma for one academic year")
    void createsAssignment() throws Exception {
        Actor admin = admin();
        AcademicYearResponse year = createAcademicYear(admin);
        TurmaResponse turma = createTurma(admin, year.id());
        SubjectResponse subject = createSubject(admin, "Matemática");
        StaffResponse staff = createTeacherRecord(admin, true);

        AssignmentResponse assignment = post(admin, "/api/staff/assignments",
                new CreateAssignment(staff.id(), turma.id(), subject.id(), year.id(), AssignmentRole.TEACHER),
                AssignmentResponse.class);

        assertThat(assignment.staffId()).isEqualTo(staff.id());
        assertThat(assignment.subjectId()).isEqualTo(subject.id());
        assertThat(assignment.turmaId()).isEqualTo(turma.id());
        assertThat(assignment.role()).isEqualTo(AssignmentRole.TEACHER);

        assertThat(get(admin, "/api/staff/assignments?academicYearId=" + year.id(),
                new TypeReference<List<AssignmentResponse>>() {}))
                .extracting(AssignmentResponse::id).contains(assignment.id());
    }

    @Test
    @DisplayName("a head-teacher assignment has no subject")
    void headTeacherAssignmentHasNoSubject() throws Exception {
        Actor admin = admin();
        AcademicYearResponse year = createAcademicYear(admin);
        TurmaResponse turma = createTurma(admin, year.id());
        StaffResponse staff = createTeacherRecord(admin, false);

        AssignmentResponse assignment = post(admin, "/api/staff/assignments",
                new CreateAssignment(staff.id(), turma.id(), null, year.id(), AssignmentRole.HEAD_TEACHER),
                AssignmentResponse.class);

        assertThat(assignment.subjectId()).isNull();
        assertThat(assignment.role()).isEqualTo(AssignmentRole.HEAD_TEACHER);
    }

    @Test
    @DisplayName("a teacher sees only their own assignments, not the whole school's")
    void teacherSeesOnlyOwnAssignments() throws Exception {
        Actor admin = admin();
        AcademicYearResponse year = createAcademicYear(admin);
        TurmaResponse turma = createTurma(admin, year.id());
        SubjectResponse subject = createSubject(admin, "Matemática");

        StaffResponse ana = createTeacherRecord(admin, true);
        StaffResponse colleague = createTeacherRecord(admin, true);

        AssignmentResponse anas = post(admin, "/api/staff/assignments",
                new CreateAssignment(ana.id(), turma.id(), subject.id(), year.id(), AssignmentRole.TEACHER),
                AssignmentResponse.class);
        AssignmentResponse colleagues = post(admin, "/api/staff/assignments",
                new CreateAssignment(colleague.id(), turma.id(), subject.id(), year.id(), AssignmentRole.ASSISTANT),
                AssignmentResponse.class);

        Actor asAna = actor(ana.userId(), "Ana Silva", "TEACHER");
        List<AssignmentResponse> mine = get(asAna,
                "/api/staff/me/assignments?academicYearId=" + year.id(), new TypeReference<>() {});

        assertThat(mine).extracting(AssignmentResponse::id)
                .contains(anas.id())
                .doesNotContain(colleagues.id());
    }

    @Test
    @DisplayName("hiring is admin/secretary work; assigning classes is admin/director work")
    void staffEndpointsAreRoleGated() throws Exception {
        Actor admin = admin();
        AcademicYearResponse year = createAcademicYear(admin);
        TurmaResponse turma = createTurma(admin, year.id());
        StaffResponse staff = createTeacherRecord(admin, false);

        Actor teacher = teacher();
        assertThat(postStatus(teacher, "/api/staff",
                new CreateStaff("Intruso", null, null, null, freshEmail(), null, null, false)))
                .isEqualTo(403);
        // A secretary can hire, but must not hand out teaching assignments.
        assertThat(postStatus(secretary(), "/api/staff/assignments",
                new CreateAssignment(staff.id(), turma.id(), null, year.id(), AssignmentRole.TEACHER)))
                .isEqualTo(403);
        assertThat(postStatus(admin, "/api/staff/assignments",
                new CreateAssignment(staff.id(), turma.id(), null, year.id(), AssignmentRole.TEACHER)))
                .isEqualTo(200);
    }
}
