package ao.skool.app.phase1;

import ao.skool.academic_structure.api.AcademicStructureQuery;
import ao.skool.academic_structure.internal.domain.CurricularTrack;
import ao.skool.academic_structure.internal.domain.GradeLevel;
import ao.skool.academic_structure.internal.domain.TrimesterKey;
import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.AcademicYearResponse;
import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.CreateAcademicYear;
import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.TrimesterInput;
import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.TrimesterResponse;
import ao.skool.academic_structure.internal.web.dto.SubjectDtos.CreateSubject;
import ao.skool.academic_structure.internal.web.dto.SubjectDtos.SubjectResponse;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.CreateTurma;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.TurmaResponse;
import ao.skool.app.support.SchoolFixture;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 1 — academic years, trimesters, subjects and turmas: the skeleton every later
 * module hangs records off.
 */
class AcademicStructureTest extends SchoolFixture {

    @Autowired private AcademicStructureQuery structure;

    @Test
    @DisplayName("an academic year is created with its three Angolan trimesters")
    void createsAcademicYearWithTrimesters() throws Exception {
        AcademicYearResponse year = createAcademicYear(admin());

        assertThat(year.id()).isNotNull();
        assertThat(year.current()).isFalse();
        assertThat(year.trimesters())
                .extracting(TrimesterResponse::key)
                .containsExactlyInAnyOrder(TrimesterKey.T1, TrimesterKey.T2, TrimesterKey.T3);

        TrimesterResponse t1 = year.trimesters().stream()
                .filter(t -> t.key() == TrimesterKey.T1).findFirst().orElseThrow();
        assertThat(t1.startDate()).isEqualTo(LocalDate.of(2025, 9, 1));
        assertThat(t1.endDate()).isEqualTo(LocalDate.of(2025, 12, 15));
    }

    @Test
    @DisplayName("marking a year current clears the previous one — only one can be current")
    void onlyOneYearIsCurrent() throws Exception {
        Actor admin = admin();
        AcademicYearResponse first = createAcademicYear(admin);
        AcademicYearResponse second = createAcademicYear(admin);

        post(admin, "/api/academic-years/" + first.id() + "/current", null, AcademicYearResponse.class);
        post(admin, "/api/academic-years/" + second.id() + "/current", null, AcademicYearResponse.class);

        List<AcademicYearResponse> years = get(admin, "/api/academic-years", new TypeReference<>() {});
        assertThat(years).filteredOn(AcademicYearResponse::current)
                .extracting(AcademicYearResponse::id)
                .containsExactly(second.id());
    }

    @Test
    @DisplayName("a year with no trimesters is allowed — schools configure them later")
    void trimestersAreOptional() throws Exception {
        var cmd = new CreateAcademicYear("Ano Sem Trimestres",
                LocalDate.of(2026, 9, 1), LocalDate.of(2027, 7, 31), null);

        AcademicYearResponse year = post(admin(), "/api/academic-years", cmd, AcademicYearResponse.class);

        assertThat(year.trimesters()).isEmpty();
    }

    @Test
    @DisplayName("subjects carry the Angolan grade level and are listed per school")
    void createsAndListsSubjects() throws Exception {
        Actor admin = admin();
        SubjectResponse mat = post(admin, "/api/subjects",
                new CreateSubject("Matemática", "MAT-10", GradeLevel.CLASSE_10), SubjectResponse.class);

        assertThat(mat.gradeLevel()).isEqualTo(GradeLevel.CLASSE_10);
        assertThat(mat.name()).isEqualTo("Matemática");  // accents survive the round trip

        List<SubjectResponse> all = get(admin, "/api/subjects", new TypeReference<>() {});
        assertThat(all).extracting(SubjectResponse::id).contains(mat.id());
    }

    @Test
    @DisplayName("turmas carry capacity and a curricular track, and list per academic year")
    void createsAndListsTurmas() throws Exception {
        Actor admin = admin();
        AcademicYearResponse year = createAcademicYear(admin);
        AcademicYearResponse otherYear = createAcademicYear(admin);

        var cmd = new CreateTurma(UUID.randomUUID(), year.id(), "10ª B",
                GradeLevel.CLASSE_10, CurricularTrack.CIENCIAS_ECONOMICO_JURIDICAS, 32);
        TurmaResponse turma = post(admin, "/api/turmas", cmd, TurmaResponse.class);

        assertThat(turma.capacity()).isEqualTo(32);
        assertThat(turma.track()).isEqualTo(CurricularTrack.CIENCIAS_ECONOMICO_JURIDICAS);

        assertThat(get(admin, "/api/turmas?academicYearId=" + year.id(),
                new TypeReference<List<TurmaResponse>>() {}))
                .extracting(TurmaResponse::id).contains(turma.id());
        // A turma belongs to exactly one year — it must not bleed into another year's list.
        assertThat(get(admin, "/api/turmas?academicYearId=" + otherYear.id(),
                new TypeReference<List<TurmaResponse>>() {}))
                .extracting(TurmaResponse::id).doesNotContain(turma.id());
    }

    @Test
    @DisplayName("the cross-module read API sees what the controllers wrote")
    void structureQueryExposesWhatWasCreated() throws Exception {
        Actor admin = admin();
        AcademicYearResponse year = createAcademicYear(admin);
        SubjectResponse subject = createSubject(admin, "Biologia");
        TurmaResponse turma = createTurma(admin, year.id());

        // This is the contract grading/boletim and assessment rely on.
        assertThat(structure.findSubject(subject.id()))
                .get().extracting(AcademicStructureQuery.SubjectView::name).isEqualTo("Biologia");
        assertThat(structure.findAcademicYear(year.id()))
                .get().extracting(AcademicStructureQuery.AcademicYearView::name).isEqualTo(year.name());
        assertThat(structure.findTurma(turma.id()))
                .get().extracting(AcademicStructureQuery.TurmaView::gradeLevel)
                .isEqualTo(GradeLevel.CLASSE_10.name());
        assertThat(structure.findSubject(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("grade levels and tracks are served as reference data for the PWA pickers")
    void referenceDataIsAvailable() throws Exception {
        Actor admin = admin();

        List<String> levels = get(admin, "/api/reference/grade-levels", new TypeReference<>() {});
        assertThat(levels).hasSize(13).startsWith("CLASSE_1").endsWith("CLASSE_13");

        List<String> tracks = get(admin, "/api/reference/curricular-tracks", new TypeReference<>() {});
        assertThat(tracks).contains("GERAL", "CIENCIAS_FISICAS_BIOLOGICAS", "HUMANIDADES");
    }

    @Test
    @DisplayName("structural changes are admin/director work, not teacher work")
    void structureIsRoleGated() throws Exception {
        Actor teacher = teacher();
        AcademicYearResponse year = createAcademicYear(admin());

        assertThat(postStatus(teacher, "/api/academic-years",
                new CreateAcademicYear("Pirata", LocalDate.of(2030, 9, 1), LocalDate.of(2031, 7, 31), null)))
                .isEqualTo(403);
        assertThat(postStatus(teacher, "/api/turmas",
                new CreateTurma(UUID.randomUUID(), year.id(), "X", GradeLevel.CLASSE_10,
                        CurricularTrack.GERAL, 10))).isEqualTo(403);

        // …but a teacher must still be able to read the structure to teach.
        assertThat(getStatus(teacher, "/api/turmas?academicYearId=" + year.id())).isEqualTo(200);
        assertThat(getStatus(teacher, "/api/subjects")).isEqualTo(200);
    }

    @Test
    @DisplayName("a secretary can create a turma but not an academic year")
    void secretaryHasNarrowerRights() throws Exception {
        Actor secretary = secretary();
        AcademicYearResponse year = createAcademicYear(admin());

        assertThat(postStatus(secretary, "/api/turmas",
                new CreateTurma(UUID.randomUUID(), year.id(), "10ª C", GradeLevel.CLASSE_10,
                        CurricularTrack.GERAL, 30))).isEqualTo(200);
        assertThat(postStatus(secretary, "/api/academic-years",
                new CreateAcademicYear("Ano", LocalDate.of(2031, 9, 1), LocalDate.of(2032, 7, 31), null)))
                .isEqualTo(403);
    }
}
