package ao.skool.staff.internal.web;

import ao.skool.common.security.Roles;
import ao.skool.staff.internal.service.StaffService;
import ao.skool.staff.internal.web.dto.StaffDtos.AssignmentResponse;
import ao.skool.staff.internal.web.dto.StaffDtos.CreateAssignment;
import ao.skool.staff.internal.web.dto.StaffDtos.CreateStaff;
import ao.skool.staff.internal.web.dto.StaffDtos.StaffResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService service;

    public StaffController(StaffService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "')")
    public StaffResponse create(@Valid @RequestBody CreateStaff cmd) {
        return service.create(cmd);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "')")
    public List<StaffResponse> list() {
        return service.list();
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "')")
    public AssignmentResponse assign(@Valid @RequestBody CreateAssignment cmd) {
        return service.assign(cmd);
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "','" + Roles.TEACHER + "')")
    public List<AssignmentResponse> listAssignments(@RequestParam UUID academicYearId) {
        return service.assignments(academicYearId);
    }
}
