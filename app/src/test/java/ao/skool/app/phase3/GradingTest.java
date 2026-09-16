package ao.skool.app.phase3;

import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.AcademicYearResponse;
import ao.skool.academic_structure.internal.web.dto.SubjectDtos.SubjectResponse;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.TurmaResponse;
import ao.skool.app.support.SchoolFixture;
import ao.skool.grading.api.event.GradePosted;
import ao.skool.grading.internal.domain.GradeCategory;
import ao.skool.grading.internal.web.dto.GradeDtos.BatchGrades;
import ao.skool.grading.internal.web.dto.GradeDtos.CreateGrade;
import ao.skool.grading.internal.web.dto.GradeDtos.GradeResponse;
import ao.skool.grading.internal.web.dto.GradeDtos.StudentGradeSummary;
import ao.skool.grading.internal.web.dto.GradeDtos.TrimesterSubjectAverage;
import ao.skool.sis.internal.web.dto.StudentDtos.StudentResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 3 — the Angolan 0–20 grading scale, weighted averages, and the boletim.
 * <p>
 * The arithmetic here ends up on a report card a parent signs, so the expected values
 * below are worked out by hand rather than copied from a run.
 */
class GradingTest extends SchoolFixture {

    @Autowired private JdbcTemplate jdbc;

    private record Scenario(UUID studentId, UUID turmaId, UUID yearId, UUID matematica, UUID portugues) {}

    private Scenario scenario(Actor admin) throws Exception {
        AcademicYearResponse year = createAcademicYear(admin);
        TurmaResponse turma = createTurma(admin, year.id());
        StudentResponse student = createStudent(admin, "João Baptista");
        enrol(admin, student.id(), year.id(), turma.id());
        SubjectResponse mat = createSubject(admin, "Matemática");
        SubjectResponse por = createSubject(admin, "Português");
        return new Scenario(student.id(), turma.id(), year.id(), mat.id(), por.id());
    }

    private CreateGrade grade(Scenario s, UUID subjectId, String trimester,
                               String value, String weight, GradeCategory category) {
        return new CreateGrade(s.studentId(), subjectId, s.turmaId(), s.yearId(), trimester,
                new BigDecimal(value), weight == null ? null : new BigDecimal(weight), category, null);
    }

    @Test
    @DisplayName("a grade is recorded on the 0–20 scale and echoed back")
    void recordsAGrade() throws Exception {
        Actor teacher = teacher();
        Scenario s = scenario(admin());

        GradeResponse posted = post(teacher, "/api/grades",
                grade(s, s.matematica(), "T1", "17.50", "2.00", GradeCategory.TEST), GradeResponse.class);

        assertThat(posted.id()).isNotNull();
        assertThat(posted.value()).isEqualByComparingTo("17.50");
        assertThat(posted.weight()).isEqualByComparingTo("2.00");
        assertThat(posted.category()).isEqualTo(GradeCategory.TEST);
        assertThat(posted.trimesterKey()).isEqualTo("T1");
    }

    @Test
    @DisplayName("weight defaults to 1 when the teacher does not set one")
    void weightDefaultsToOne() throws Exception {
        Scenario s = scenario(admin());

        GradeResponse posted = post(teacher(), "/api/grades",
                grade(s, s.matematica(), "T1", "12.00", null, GradeCategory.PARTICIPATION),
                GradeResponse.class);

        assertThat(posted.weight()).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("grades outside 0–20 are rejected")
    void rejectsGradesOutsideTheScale() throws Exception {
        Actor teacher = teacher();
        Scenario s = scenario(admin());

        assertThat(postStatus(teacher, "/api/grades",
                grade(s, s.matematica(), "T1", "20.01", null, GradeCategory.TEST))).isEqualTo(400);
        assertThat(postStatus(teacher, "/api/grades",
                grade(s, s.matematica(), "T1", "-0.01", null, GradeCategory.TEST))).isEqualTo(400);
        // The boundaries themselves are valid marks.
        assertThat(postStatus(teacher, "/api/grades",
                grade(s, s.matematica(), "T1", "0.00", null, GradeCategory.TEST))).isEqualTo(200);
        assertThat(postStatus(teacher, "/api/grades",
                grade(s, s.portugues(), "T1", "20.00", null, GradeCategory.TEST))).isEqualTo(200);
    }

    @Test
    @DisplayName("only T1, T2 and T3 are accepted as trimester keys")
    void rejectsUnknownTrimester() throws Exception {
        Scenario s = scenario(admin());

        assertThat(postStatus(teacher(), "/api/grades",
                grade(s, s.matematica(), "T4", "15.00", null, GradeCategory.TEST))).isEqualTo(400);
        assertThat(postStatus(teacher(), "/api/grades",
                grade(s, s.matematica(), "Q1", "15.00", null, GradeCategory.TEST))).isEqualTo(400);
    }

    @Test
    @DisplayName("the summary computes weighted subject averages, trimester averages and a final")
    void computesWeightedAverages() throws Exception {
        Actor teacher = teacher();
        Scenario s = scenario(admin());

        post(teacher, "/api/grades/batch", new BatchGrades(List.of(
                // Matemática T1: (14×1 + 18×3) / (1+3) = 68/4 = 17.00
                grade(s, s.matematica(), "T1", "14.00", "1.00", GradeCategory.PARTICIPATION),
                grade(s, s.matematica(), "T1", "18.00", "3.00", GradeCategory.EXAM),
                // Português T1: (12 + 16) / 2 = 14.00
                grade(s, s.portugues(), "T1", "12.00", "1.00", GradeCategory.TEST),
                grade(s, s.portugues(), "T1", "16.00", "1.00", GradeCategory.TEST),
                // Matemática T2: single mark = 10.00
                grade(s, s.matematica(), "T2", "10.00", "1.00", GradeCategory.EXAM))),
                new TypeReference<List<GradeResponse>>() {});

        StudentGradeSummary summary = get(teacher,
                "/api/grades/students/" + s.studentId() + "/summary?academicYearId=" + s.yearId(),
                StudentGradeSummary.class);

        assertThat(summary.studentName()).isEqualTo("João Baptista");

        assertThat(summary.subjectAverages())
                .filteredOn(a -> a.trimesterKey().equals("T1") && a.subjectId().equals(s.matematica()))
                .singleElement().extracting(TrimesterSubjectAverage::average)
                .satisfies(avg -> assertThat((BigDecimal) avg).isEqualByComparingTo("17.00"));
        assertThat(summary.subjectAverages())
                .filteredOn(a -> a.trimesterKey().equals("T1") && a.subjectId().equals(s.portugues()))
                .singleElement().extracting(TrimesterSubjectAverage::average)
                .satisfies(avg -> assertThat((BigDecimal) avg).isEqualByComparingTo("14.00"));

        // T1 = mean of the two subject averages: (17.00 + 14.00) / 2 = 15.50
        assertThat(summary.trimesterAverages().get("T1")).isEqualByComparingTo("15.50");
        // T2 has only Matemática: 10.00
        assertThat(summary.trimesterAverages().get("T2")).isEqualByComparingTo("10.00");
        // Final = mean of the trimesters present: (15.50 + 10.00) / 2 = 12.75
        assertThat(summary.finalAverage()).isEqualByComparingTo("12.75");
    }

    @Test
    @DisplayName("the subject name is resolved for the report card, not just its id")
    void summaryCarriesSubjectNames() throws Exception {
        Actor teacher = teacher();
        Scenario s = scenario(admin());
        post(teacher, "/api/grades", grade(s, s.matematica(), "T1", "15.00", null, GradeCategory.TEST),
                GradeResponse.class);

        StudentGradeSummary summary = get(teacher,
                "/api/grades/students/" + s.studentId() + "/summary?academicYearId=" + s.yearId(),
                StudentGradeSummary.class);

        assertThat(summary.subjectAverages()).singleElement()
                .extracting(TrimesterSubjectAverage::subjectName).isEqualTo("Matemática");
    }

    @Test
    @DisplayName("a student with no grades gets an empty summary, not an error")
    void emptySummaryForUngradedStudent() throws Exception {
        Scenario s = scenario(admin());

        StudentGradeSummary summary = get(teacher(),
                "/api/grades/students/" + s.studentId() + "/summary?academicYearId=" + s.yearId(),
                StudentGradeSummary.class);

        assertThat(summary.subjectAverages()).isEmpty();
        assertThat(summary.trimesterAverages()).isEmpty();
        assertThat(summary.finalAverage()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("posting a grade publishes GradePosted")
    void publishesGradePosted() throws Exception {
        Actor teacher = teacher();
        Scenario s = scenario(admin());

        events.clear();
        GradeResponse posted = post(teacher, "/api/grades",
                grade(s, s.matematica(), "T2", "16.00", null, GradeCategory.EXAM), GradeResponse.class);

        GradePosted event = events.onlyOne(GradePosted.class);
        assertThat(event.gradeId()).isEqualTo(posted.id());
        assertThat(event.studentId()).isEqualTo(s.studentId());
        assertThat(event.subjectId()).isEqualTo(s.matematica());
        assertThat(event.trimesterKey()).isEqualTo("T2");
        assertThat(event.value()).isEqualByComparingTo("16.00");
    }

    @Test
    @DisplayName("every grade write leaves an audit row naming the actor")
    void writesAnAuditRow() throws Exception {
        Actor teacher = teacher();
        Scenario s = scenario(admin());

        GradeResponse posted = post(teacher, "/api/grades",
                grade(s, s.matematica(), "T1", "13.00", null, GradeCategory.TEST), GradeResponse.class);

        List<java.util.Map<String, Object>> rows = jdbc.queryForList(
                "select actor_id, action, target_type, target_id from audit_log "
                        + "where target_id = ? and tenant_id = ?", posted.id().toString(), tenant);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.get("action")).isEqualTo("grade.post");
            assertThat(row.get("target_type")).isEqualTo("grade");
            assertThat(row.get("actor_id")).isEqualTo(teacher.userId().toString());
        });
    }

    @Test
    @DisplayName("a batch of grades writes one audit row per grade")
    void batchAuditsEveryEntry() throws Exception {
        Actor teacher = teacher();
        Scenario s = scenario(admin());

        post(teacher, "/api/grades/batch", new BatchGrades(List.of(
                grade(s, s.matematica(), "T1", "11.00", null, GradeCategory.TEST),
                grade(s, s.portugues(), "T1", "12.00", null, GradeCategory.TEST),
                grade(s, s.matematica(), "T2", "13.00", null, GradeCategory.TEST))),
                new TypeReference<List<GradeResponse>>() {});

        Long count = jdbc.queryForObject(
                "select count(*) from audit_log where tenant_id = ? and action = 'grade.post'",
                Long.class, tenant);
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("the boletim downloads as a real PDF")
    void generatesBoletimPdf() throws Exception {
        Actor teacher = teacher();
        Scenario s = scenario(admin());
        post(teacher, "/api/grades/batch", new BatchGrades(List.of(
                grade(s, s.matematica(), "T1", "14.00", null, GradeCategory.TEST),
                grade(s, s.portugues(), "T1", "16.00", null, GradeCategory.TEST))),
                new TypeReference<List<GradeResponse>>() {});

        var result = exec(teacher, MockMvcRequestBuilders.get("/api/grades/boletim"
                + "?studentId=" + s.studentId() + "&academicYearId=" + s.yearId()), null);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentType()).isEqualTo("application/pdf");
        assertThat(result.getResponse().getHeader("Content-Disposition"))
                .contains("attachment").contains("boletim-" + s.studentId() + ".pdf");

        byte[] pdf = result.getResponse().getContentAsByteArray();
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.ISO_8859_1))
                .as("PDF magic number").startsWith("%PDF-");
    }

    @Test
    @DisplayName("a boletim for an unknown student is a 404, not a blank PDF")
    void boletimForUnknownStudentIs404() throws Exception {
        Scenario s = scenario(admin());

        assertThat(getStatus(teacher(), "/api/grades/boletim"
                + "?studentId=" + UUID.randomUUID() + "&academicYearId=" + s.yearId())).isEqualTo(404);
    }

    @Test
    @DisplayName("students and guardians can read grades but never write them")
    void gradeWritesAreTeacherOnly() throws Exception {
        Scenario s = scenario(admin());
        CreateGrade cmd = grade(s, s.matematica(), "T1", "20.00", null, GradeCategory.EXAM);

        assertThat(postStatus(student(UUID.randomUUID()), "/api/grades", cmd)).isEqualTo(403);
        assertThat(postStatus(guardian(UUID.randomUUID()), "/api/grades", cmd)).isEqualTo(403);
        assertThat(postStatus(secretary(), "/api/grades", cmd)).isEqualTo(403);

        String summaryPath = "/api/grades/students/" + s.studentId() + "/summary?academicYearId=" + s.yearId();
        assertThat(getStatus(student(UUID.randomUUID()), summaryPath)).isEqualTo(200);
        assertThat(getStatus(guardian(UUID.randomUUID()), summaryPath)).isEqualTo(200);
    }
}
