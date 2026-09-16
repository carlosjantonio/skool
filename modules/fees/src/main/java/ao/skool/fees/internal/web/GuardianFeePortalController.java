package ao.skool.fees.internal.web;

import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.security.Roles;
import ao.skool.fees.internal.service.FeeService;
import ao.skool.fees.internal.web.dto.FeeDtos.InvoiceResponse;
import ao.skool.sis.api.GuardianDirectory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Guardian-scoped invoice view. The guardian never passes a studentId — we resolve
 * their linked children server-side, so a guardian can only see invoices for kids
 * they're actually attached to.
 */
@RestController
@RequestMapping("/api/guardian/fees")
public class GuardianFeePortalController {

    private final FeeService feeService;
    private final GuardianDirectory guardianDirectory;

    public GuardianFeePortalController(FeeService feeService, GuardianDirectory guardianDirectory) {
        this.feeService = feeService;
        this.guardianDirectory = guardianDirectory;
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasRole('" + Roles.GUARDIAN + "')")
    public List<InvoiceResponse> myChildrensInvoices(@AuthenticationPrincipal SkoolPrincipal principal) {
        List<UUID> childIds = guardianDirectory.childrenOf(UUID.fromString(principal.userId()));
        List<InvoiceResponse> all = new ArrayList<>();
        for (UUID studentId : childIds) all.addAll(feeService.listInvoicesForStudent(studentId));
        return all;
    }
}
