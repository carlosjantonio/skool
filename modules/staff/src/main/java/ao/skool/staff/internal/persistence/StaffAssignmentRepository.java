package ao.skool.staff.internal.persistence;

import ao.skool.staff.internal.domain.StaffAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, UUID> {
    List<StaffAssignment> findByTenantIdAndAcademicYearId(UUID tenantId, UUID academicYearId);
    List<StaffAssignment> findByStaffIdAndAcademicYearId(UUID staffId, UUID academicYearId);
    List<StaffAssignment> findByTurmaId(UUID turmaId);
}
