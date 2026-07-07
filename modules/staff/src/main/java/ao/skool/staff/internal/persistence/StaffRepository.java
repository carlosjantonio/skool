package ao.skool.staff.internal.persistence;

import ao.skool.staff.internal.domain.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StaffRepository extends JpaRepository<Staff, UUID> {
    List<Staff> findByTenantIdOrderByFullNameAsc(UUID tenantId);
}
