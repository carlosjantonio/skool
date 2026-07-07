package ao.skool.academic_structure.internal.service;

import ao.skool.academic_structure.api.AcademicStructureQuery;
import ao.skool.academic_structure.internal.persistence.AcademicYearRepository;
import ao.skool.academic_structure.internal.persistence.SchoolRepository;
import ao.skool.academic_structure.internal.persistence.SubjectRepository;
import ao.skool.academic_structure.internal.persistence.TurmaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AcademicStructureQueryImpl implements AcademicStructureQuery {

    private final SubjectRepository subjects;
    private final AcademicYearRepository years;
    private final TurmaRepository turmas;
    private final SchoolRepository schools;

    public AcademicStructureQueryImpl(SubjectRepository subjects, AcademicYearRepository years,
                                       TurmaRepository turmas, SchoolRepository schools) {
        this.subjects = subjects;
        this.years = years;
        this.turmas = turmas;
        this.schools = schools;
    }

    @Override
    public Optional<SubjectView> findSubject(UUID subjectId) {
        return subjects.findById(subjectId)
                .map(s -> new SubjectView(s.id(), s.name(), s.code(), s.gradeLevel().name()));
    }

    @Override
    public Optional<AcademicYearView> findAcademicYear(UUID academicYearId) {
        return years.findById(academicYearId).map(y -> new AcademicYearView(
                y.id(), y.name(), y.startDate(), y.endDate(), y.current(),
                y.trimesters().stream()
                        .map(t -> new TrimesterView(t.id(), t.key().name(), t.startDate(), t.endDate()))
                        .toList()
        ));
    }

    @Override
    public Optional<TurmaView> findTurma(UUID turmaId) {
        return turmas.findById(turmaId)
                .map(t -> new TurmaView(t.id(), t.name(), t.gradeLevel().name(), t.track().name()));
    }

    @Override
    public Optional<SchoolView> findSchool(UUID schoolId) {
        return schools.findById(schoolId)
                .map(s -> new SchoolView(s.id(), s.name(), s.code()));
    }
}
