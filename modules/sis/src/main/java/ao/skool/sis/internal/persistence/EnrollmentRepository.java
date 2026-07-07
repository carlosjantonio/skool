package ao.skool.sis.internal.persistence;

import ao.skool.sis.internal.domain.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {
    List<Enrollment> findByTenantIdAndAcademicYearId(UUID tenantId, UUID academicYearId);
    List<Enrollment> findByTurmaId(UUID turmaId);
    boolean existsByStudentIdAndAcademicYearId(UUID studentId, UUID academicYearId);
}
