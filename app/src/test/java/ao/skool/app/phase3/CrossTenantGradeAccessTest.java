package ao.skool.app.phase3;

import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.AcademicYearResponse;
import ao.skool.academic_structure.internal.web.dto.SubjectDtos.SubjectResponse;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.TurmaResponse;
import ao.skool.app.support.SchoolFixture;
import ao.skool.grading.internal.domain.GradeCategory;
import ao.skool.grading.internal.web.dto.GradeDtos.CreateGrade;
import ao.skool.grading.internal.web.dto.GradeDtos.GradeResponse;
import ao.skool.sis.internal.web.dto.StudentDtos.StudentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A teacher at one school must not be able to read a student at another school, even
 * holding that student's id.
 * <p>
 * This is worth its own test because the read path does not look tenant-scoped on
 * inspection: {@code StudentDirectoryImpl.findStudent} is a plain {@code findById}, and
 * {@code GradeRepository.findByStudentIdAndAcademicYearId…} filters on neither tenant nor
 * school. In practice both ids are opaque UUIDs that an outsider has no way to obtain, so
 * this is a defence-in-depth check rather than a reachable hole — but if the ids ever leak
 * (a shared spreadsheet, a URL in a support ticket) it becomes the only thing standing in
 * the way, so the behaviour should be pinned rather than assumed.
 */
class CrossTenantGradeAccessTest extends SchoolFixture {

    @Test
    @DisplayName("a teacher from another school cannot read a student's grade summary")
    void gradeSummaryIsScopedToTheOwningSchool() throws Exception {
        Actor ourAdmin = admin();
        Actor ourTeacher = teacher();

        AcademicYearResponse year = createAcademicYear(ourAdmin);
        TurmaResponse turma = createTurma(ourAdmin, year.id());
        SubjectResponse subject = createSubject(ourAdmin, "Matemática");
        StudentResponse student = createStudent(ourAdmin, "João Baptista");
        enrol(ourAdmin, student.id(), year.id(), turma.id());

        post(ourTeacher, "/api/grades", new CreateGrade(student.id(), subject.id(), turma.id(),
                year.id(), "T1", new BigDecimal("17.00"), null, GradeCategory.EXAM, null),
                GradeResponse.class);

        // An outsider holding both ids — the worst realistic case.
        Actor outsider = actorInOtherTenant("Professor Alheio", "TEACHER");
        var result = exec(outsider, MockMvcRequestBuilders.get(
                "/api/grades/students/" + student.id() + "/summary?academicYearId=" + year.id()), null);

        assertThat(result.getResponse().getStatus())
                .as("another school's student must not resolve at all")
                .isEqualTo(404);
        assertThat(bodyOf(result))
                .as("not even the student's name may leak")
                .doesNotContain("João Baptista");
    }

    @Test
    @DisplayName("a made-up student id is a 404, not an empty summary")
    void unknownStudentIsNotFound() throws Exception {
        Actor admin = admin();
        AcademicYearResponse year = createAcademicYear(admin);

        assertThat(getStatus(teacher(),
                "/api/grades/students/" + UUID.randomUUID() + "/summary?academicYearId=" + year.id()))
                .isEqualTo(404);
    }
}
