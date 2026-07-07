package ao.skool.sis.internal.web;

import ao.skool.common.security.Roles;
import ao.skool.sis.internal.service.GuardianService;
import ao.skool.sis.internal.web.dto.GuardianDtos.CreateGuardian;
import ao.skool.sis.internal.web.dto.GuardianDtos.GuardianResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guardians")
public class GuardianController {

    private final GuardianService service;

    public GuardianController(GuardianService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "')")
    public GuardianResponse create(@Valid @RequestBody CreateGuardian cmd) {
        return service.create(cmd);
    }
}
