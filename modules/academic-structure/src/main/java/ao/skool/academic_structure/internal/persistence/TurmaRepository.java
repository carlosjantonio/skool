package ao.skool.academic_structure.internal.persistence;

import ao.skool.academic_structure.internal.domain.Turma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TurmaRepository extends JpaRepository<Turma, UUID> {
    List<Turma> findByTenantIdAndAcademicYearIdOrderByGradeLevelAscNameAsc(UUID tenantId, UUID academicYearId);
}
