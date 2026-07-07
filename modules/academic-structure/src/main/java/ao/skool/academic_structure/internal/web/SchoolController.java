package ao.skool.academic_structure.internal.web;

import ao.skool.academic_structure.internal.service.SchoolService;
import ao.skool.academic_structure.internal.web.dto.SchoolDtos.CreateSchool;
import ao.skool.academic_structure.internal.web.dto.SchoolDtos.SchoolResponse;
import ao.skool.common.security.Roles;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools")
public class SchoolController {

    private final SchoolService service;

    public SchoolController(SchoolService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('" + Roles.ADMIN + "')")
    public SchoolResponse create(@Valid @RequestBody CreateSchool cmd) {
        return service.create(cmd);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<SchoolResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public SchoolResponse get(@PathVariable UUID id) {
        return service.get(id);
    }
}
