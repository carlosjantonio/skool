package ao.skool.academic_structure.internal.persistence;

import ao.skool.academic_structure.internal.domain.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {
    List<AcademicYear> findByTenantIdOrderByStartDateDesc(UUID tenantId);
    Optional<AcademicYear> findByTenantIdAndCurrentTrue(UUID tenantId);
}
