package ao.skool.fees.internal.persistence;

import ao.skool.fees.internal.domain.Scholarship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScholarshipRepository extends JpaRepository<Scholarship, UUID> {
    List<Scholarship> findByStudentIdAndActiveTrue(UUID studentId);
    List<Scholarship> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
