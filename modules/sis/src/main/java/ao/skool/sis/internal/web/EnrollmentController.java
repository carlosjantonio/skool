package ao.skool.sis.internal.web;

import ao.skool.common.security.Roles;
import ao.skool.sis.internal.service.EnrollmentService;
import ao.skool.sis.internal.web.dto.EnrollmentDtos.CreateEnrollment;
import ao.skool.sis.internal.web.dto.EnrollmentDtos.EnrollmentResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService service;

    public EnrollmentController(EnrollmentService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "')")
    public EnrollmentResponse enrol(@Valid @RequestBody CreateEnrollment cmd) {
        return service.enrol(cmd);
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "')")
    public EnrollmentResponse withdraw(@PathVariable UUID id) {
        return service.withdraw(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "','" + Roles.TEACHER + "')")
    public List<EnrollmentResponse> list(@RequestParam UUID academicYearId) {
        return service.list(academicYearId);
    }
}
