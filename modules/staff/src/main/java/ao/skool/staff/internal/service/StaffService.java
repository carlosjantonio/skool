package ao.skool.staff.internal.service;

import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.common.web.error.ApplicationException;
import ao.skool.common.web.error.NotFoundException;
import ao.skool.identity.api.IdentityUserService;
import ao.skool.staff.internal.domain.Staff;
import ao.skool.staff.internal.domain.StaffAssignment;
import ao.skool.staff.internal.persistence.StaffAssignmentRepository;
import ao.skool.staff.internal.persistence.StaffRepository;
import ao.skool.staff.internal.web.dto.StaffDtos.AssignmentResponse;
import ao.skool.staff.internal.web.dto.StaffDtos.CreateAssignment;
import ao.skool.staff.internal.web.dto.StaffDtos.CreateStaff;
import ao.skool.staff.internal.web.dto.StaffDtos.StaffResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class StaffService {

    private final StaffRepository staff;
    private final StaffAssignmentRepository assignments;
    private final IdentityUserService identity;
    private final TenantContext tenant;

    public StaffService(StaffRepository staff, StaffAssignmentRepository assignments,
                        IdentityUserService identity, TenantContext tenant) {
        this.staff = staff;
        this.assignments = assignments;
        this.identity = identity;
        this.tenant = tenant;
    }

    public StaffResponse create(CreateStaff cmd) {
        var tenantId = tenant.current();
        Staff s = new Staff(UUID.randomUUID(), tenantId.value(), cmd.fullName());
        s.setBi(cmd.bi());
        s.setNif(cmd.nif());
        s.setPhone(cmd.phone());
        s.setEmail(cmd.email());
        s.setQualification(cmd.qualification());
        s.setHireDate(cmd.hireDate());

        if (cmd.provisionPortalUser() && cmd.email() != null && !cmd.email().isBlank()) {
            try {
                UUID userId = identity.createTeacherUser(tenantId, cmd.email(), cmd.fullName());
                s.linkUser(userId);
            } catch (IdentityUserService.UserAlreadyExistsException e) {
                throw new ApplicationException(HttpStatus.CONFLICT, "error.conflict");
            }
        }
        staff.save(s);
        return toResponse(s);
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> list() {
        return staff.findByTenantIdOrderByFullNameAsc(tenant.current().value())
                .stream().map(this::toResponse).toList();
    }

    public AssignmentResponse assign(CreateAssignment cmd) {
        var tenantId = tenant.current();
        Staff member = staff.findById(cmd.staffId()).orElseThrow(NotFoundException::new);
        if (!member.tenantId().equals(tenantId.value())) throw new NotFoundException();

        StaffAssignment assignment = new StaffAssignment(
                UUID.randomUUID(), tenantId.value(),
                cmd.staffId(), cmd.turmaId(), cmd.subjectId(),
                cmd.academicYearId(), cmd.role());
        assignments.save(assignment);
        return toResponse(assignment);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> assignments(UUID academicYearId) {
        return assignments.findByTenantIdAndAcademicYearId(tenant.current().value(), academicYearId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<StaffResponse> findByUserId(UUID userId) {
        return staff.findByUserId(userId).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> assignmentsForStaff(UUID staffId, UUID academicYearId) {
        return assignments.findByStaffIdAndAcademicYearId(staffId, academicYearId)
                .stream().map(this::toResponse).toList();
    }

    private StaffResponse toResponse(Staff s) {
        return new StaffResponse(s.id(), s.fullName(), s.bi(), s.nif(), s.phone(), s.email(),
                s.qualification(), s.hireDate(), s.userId(), s.active());
    }

    private AssignmentResponse toResponse(StaffAssignment a) {
        return new AssignmentResponse(a.id(), a.staffId(), a.turmaId(), a.subjectId(),
                a.academicYearId(), a.role());
    }
}
