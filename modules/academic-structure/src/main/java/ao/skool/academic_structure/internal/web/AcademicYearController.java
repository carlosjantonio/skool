package ao.skool.academic_structure.internal.web;

import ao.skool.academic_structure.internal.service.AcademicYearService;
import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.AcademicYearResponse;
import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.CreateAcademicYear;
import ao.skool.common.security.Roles;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/academic-years")
public class AcademicYearController {

    private final AcademicYearService service;

    public AcademicYearController(AcademicYearService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "')")
    public AcademicYearResponse create(@Valid @RequestBody CreateAcademicYear cmd) {
        return service.create(cmd);
    }

    @PostMapping("/{id}/current")
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "')")
    public AcademicYearResponse markCurrent(@PathVariable UUID id) {
        return service.markCurrent(id);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<AcademicYearResponse> list() {
        return service.list();
    }
}
