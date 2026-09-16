package ao.skool.fees.internal.web;

import ao.skool.common.security.Roles;
import ao.skool.fees.internal.service.FeeService;
import ao.skool.fees.internal.web.dto.FeeDtos.BillingResult;
import ao.skool.fees.internal.web.dto.FeeDtos.CreateFeeSchedule;
import ao.skool.fees.internal.web.dto.FeeDtos.CreateScholarship;
import ao.skool.fees.internal.web.dto.FeeDtos.FeeScheduleResponse;
import ao.skool.fees.internal.web.dto.FeeDtos.InvoiceResponse;
import ao.skool.fees.internal.web.dto.FeeDtos.ScholarshipResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fees")
public class FeeController {

    private static final String ADMIN_ROLES = "hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "')";

    private final FeeService service;

    public FeeController(FeeService service) { this.service = service; }

    @PostMapping("/schedules")
    @PreAuthorize(ADMIN_ROLES)
    public FeeScheduleResponse createSchedule(@Valid @RequestBody CreateFeeSchedule cmd) {
        return service.createSchedule(cmd);
    }

    @GetMapping("/schedules")
    @PreAuthorize(ADMIN_ROLES)
    public List<FeeScheduleResponse> listSchedules(@RequestParam UUID academicYearId) {
        return service.listSchedules(academicYearId);
    }

    @PostMapping("/schedules/{id}/run-billing")
    @PreAuthorize(ADMIN_ROLES)
    public BillingResult runBilling(@PathVariable UUID id) {
        return service.runBilling(id);
    }

    @PostMapping("/scholarships")
    @PreAuthorize(ADMIN_ROLES)
    public ScholarshipResponse createScholarship(@Valid @RequestBody CreateScholarship cmd) {
        return service.createScholarship(cmd);
    }

    @GetMapping("/scholarships")
    @PreAuthorize(ADMIN_ROLES)
    public List<ScholarshipResponse> listScholarships() {
        return service.listScholarships();
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "','" + Roles.GUARDIAN + "')")
    public List<InvoiceResponse> invoicesForStudent(@RequestParam UUID studentId) {
        return service.listInvoicesForStudent(studentId);
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAnyRole('" + Roles.ADMIN + "','" + Roles.DIRECTOR + "','" + Roles.SECRETARY + "','" + Roles.GUARDIAN + "')")
    public InvoiceResponse getInvoice(@PathVariable UUID id) {
        return service.getInvoice(id);
    }
}
