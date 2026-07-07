package ao.skool.grading.internal.persistence;

import ao.skool.grading.internal.domain.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GradeRepository extends JpaRepository<Grade, UUID> {
    List<Grade> findByStudentIdAndAcademicYearIdOrderByTrimesterKeyAscSubjectIdAsc(
            UUID studentId, UUID academicYearId);
    List<Grade> findByTurmaIdAndSubjectIdAndTrimesterKey(UUID turmaId, UUID subjectId, String trimesterKey);
}
