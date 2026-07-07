package ao.skool.academic_structure.internal.web;

import ao.skool.academic_structure.internal.service.TurmaService;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.CreateTurma;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.TurmaResponse;
import ao.skool.common.security.Roles;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/turmas")
public class TurmaController {

    private final TurmaService service;

    public TurmaController(TurmaService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "')")
    public TurmaResponse create(@Valid @RequestBody CreateTurma cmd) {
        return service.create(cmd);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<TurmaResponse> list(@RequestParam UUID academicYearId) {
        return service.list(academicYearId);
    }
}
