package ao.skool.fees.internal.persistence;

import ao.skool.fees.internal.domain.FeeSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeeScheduleRepository extends JpaRepository<FeeSchedule, UUID> {
    List<FeeSchedule> findByTenantIdAndAcademicYearIdOrderByDueAtAsc(UUID tenantId, UUID academicYearId);
}
