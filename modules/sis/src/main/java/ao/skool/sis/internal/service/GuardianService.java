package ao.skool.sis.internal.service;

import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.common.web.error.ApplicationException;
import ao.skool.identity.api.IdentityUserService;
import ao.skool.sis.internal.domain.Guardian;
import ao.skool.sis.internal.persistence.GuardianRepository;
import ao.skool.sis.internal.web.dto.GuardianDtos.CreateGuardian;
import ao.skool.sis.internal.web.dto.GuardianDtos.GuardianResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class GuardianService {

    private final GuardianRepository guardians;
    private final IdentityUserService identity;
    private final TenantContext tenant;

    public GuardianService(GuardianRepository guardians,
                           IdentityUserService identity,
                           TenantContext tenant) {
        this.guardians = guardians;
        this.identity = identity;
        this.tenant = tenant;
    }

    public GuardianResponse create(CreateGuardian cmd) {
        var tenantId = tenant.current();
        Guardian guardian = new Guardian(UUID.randomUUID(), tenantId.value(), cmd.fullName(), cmd.phone());
        if (cmd.bi() != null && !cmd.bi().isBlank()) guardian.setBi(cmd.bi());
        if (cmd.email() != null && !cmd.email().isBlank()) guardian.setEmail(cmd.email());

        if (cmd.provisionPortalUser() && cmd.email() != null && !cmd.email().isBlank()) {
            try {
                UUID userId = identity.createGuardianUser(tenantId, cmd.email(), cmd.fullName());
                guardian.linkUser(userId);
            } catch (IdentityUserService.UserAlreadyExistsException e) {
                throw new ApplicationException(HttpStatus.CONFLICT, "error.conflict");
            }
        }
        guardians.save(guardian);
        return toResponse(guardian);
    }

    private GuardianResponse toResponse(Guardian g) {
        return new GuardianResponse(g.id(), g.fullName(), g.phone(), g.email(), g.bi(), g.userId());
    }
}
