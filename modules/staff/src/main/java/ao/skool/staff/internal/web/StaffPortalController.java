package ao.skool.staff.internal.web;

import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.security.Roles;
import ao.skool.common.web.error.ApplicationException;
import ao.skool.staff.internal.service.StaffService;
import ao.skool.staff.internal.web.dto.StaffDtos.AssignmentResponse;
import ao.skool.staff.internal.web.dto.StaffDtos.StaffResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints scoped to the currently authenticated staff member (teacher, director, etc.).
 * The staff record is looked up by the JWT's userId → the {@code user_id} column on
 * the staff table.
 */
@RestController
@RequestMapping("/api/staff/me")
public class StaffPortalController {

    private final StaffService service;

    public StaffPortalController(StaffService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('" + Roles.TEACHER + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "','" + Roles.ADMIN + "')")
    public StaffResponse me(@AuthenticationPrincipal SkoolPrincipal principal) {
        return service.findByUserId(UUID.fromString(principal.userId()))
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "error.not_found"));
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasAnyRole('" + Roles.TEACHER + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "','" + Roles.ADMIN + "')")
    public List<AssignmentResponse> myAssignments(
            @AuthenticationPrincipal SkoolPrincipal principal,
            @RequestParam UUID academicYearId) {
        var staff = service.findByUserId(UUID.fromString(principal.userId()))
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "error.not_found"));
        return service.assignmentsForStaff(staff.id(), academicYearId);
    }
}
