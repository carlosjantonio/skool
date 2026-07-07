package ao.skool.academic_structure.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only lookup for cross-module rendering (boletim, timetables, etc.).
 * Structural mutations stay behind the academic-structure controllers.
 */
public interface AcademicStructureQuery {

    Optional<SubjectView> findSubject(UUID subjectId);

    Optional<AcademicYearView> findAcademicYear(UUID academicYearId);

    Optional<TurmaView> findTurma(UUID turmaId);

    Optional<SchoolView> findSchool(UUID schoolId);

    record SchoolView(UUID id, String name, String code) {}

    record SubjectView(UUID id, String name, String code, String gradeLevel) {}

    record TrimesterView(UUID id, String key, LocalDate startDate, LocalDate endDate) {}

    record AcademicYearView(
            UUID id,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            boolean current,
            List<TrimesterView> trimesters
    ) {}

    record TurmaView(UUID id, String name, String gradeLevel, String track) {}
}
