package ao.skool.academic_structure.internal.web;

import ao.skool.academic_structure.internal.service.SubjectService;
import ao.skool.academic_structure.internal.web.dto.SubjectDtos.CreateSubject;
import ao.skool.academic_structure.internal.web.dto.SubjectDtos.SubjectResponse;
import ao.skool.common.security.Roles;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService service;

    public SubjectController(SubjectService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "')")
    public SubjectResponse create(@Valid @RequestBody CreateSubject cmd) {
        return service.create(cmd);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<SubjectResponse> list() {
        return service.list();
    }
}
