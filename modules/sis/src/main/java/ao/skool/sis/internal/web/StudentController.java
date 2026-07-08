package ao.skool.sis.internal.web;

import ao.skool.common.security.Roles;
import ao.skool.sis.internal.service.StudentService;
import ao.skool.sis.internal.web.dto.StudentDtos.CreateStudent;
import ao.skool.sis.internal.web.dto.StudentDtos.LinkGuardian;
import ao.skool.sis.internal.web.dto.StudentDtos.ProvisionStudentUser;
import ao.skool.sis.internal.web.dto.StudentDtos.StudentResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "')")
    public StudentResponse create(@Valid @RequestBody CreateStudent cmd) {
        return service.create(cmd);
    }

    @PostMapping("/{id}/guardians")
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "')")
    public StudentResponse linkGuardian(@PathVariable UUID id, @Valid @RequestBody LinkGuardian cmd) {
        return service.linkGuardian(id, cmd);
    }

    @PostMapping("/{id}/portal-user")
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "')")
    public StudentResponse provisionPortalUser(@PathVariable UUID id, @Valid @RequestBody ProvisionStudentUser cmd) {
        return service.provisionPortalUser(id, cmd);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "','" + Roles.TEACHER + "')")
    public List<StudentResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "','" + Roles.TEACHER + "')")
    public StudentResponse get(@PathVariable UUID id) {
        return service.get(id);
    }
}
