package ao.skool.fees.internal.service;

import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.common.security.audit.AuditLogWriter;
import ao.skool.common.web.error.ApplicationException;
import ao.skool.common.web.error.NotFoundException;
import ao.skool.fees.api.event.InvoiceIssued;
import ao.skool.fees.internal.domain.FeeKind;
import ao.skool.fees.internal.domain.FeeSchedule;
import ao.skool.fees.internal.domain.Invoice;
import ao.skool.fees.internal.domain.InvoiceStatus;
import ao.skool.fees.internal.domain.Scholarship;
import ao.skool.fees.internal.persistence.FeeScheduleRepository;
import ao.skool.fees.internal.persistence.InvoiceRepository;
import ao.skool.fees.internal.persistence.ScholarshipRepository;
import ao.skool.fees.internal.web.dto.FeeDtos.BillingResult;
import ao.skool.fees.internal.web.dto.FeeDtos.CreateFeeSchedule;
import ao.skool.fees.internal.web.dto.FeeDtos.CreateScholarship;
import ao.skool.fees.internal.web.dto.FeeDtos.FeeScheduleResponse;
import ao.skool.fees.internal.web.dto.FeeDtos.InvoiceResponse;
import ao.skool.fees.internal.web.dto.FeeDtos.ScholarshipResponse;
import ao.skool.sis.api.StudentDirectory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class FeeService {

    private final FeeScheduleRepository schedules;
    private final ScholarshipRepository scholarships;
    private final InvoiceRepository invoices;
    private final StudentDirectory studentDirectory;
    private final TenantContext tenant;
    private final ApplicationEventPublisher events;
    private final AuditLogWriter audit;

    public FeeService(FeeScheduleRepository schedules, ScholarshipRepository scholarships,
                      InvoiceRepository invoices, StudentDirectory studentDirectory,
                      TenantContext tenant, ApplicationEventPublisher events,
                      AuditLogWriter audit) {
        this.schedules = schedules;
        this.scholarships = scholarships;
        this.invoices = invoices;
        this.studentDirectory = studentDirectory;
        this.tenant = tenant;
        this.events = events;
        this.audit = audit;
    }

    public FeeScheduleResponse createSchedule(CreateFeeSchedule cmd) {
        UUID tenantId = tenant.current().value();
        FeeSchedule fs = new FeeSchedule(UUID.randomUUID(), tenantId, cmd.academicYearId(),
                cmd.name(), cmd.kind(), cmd.amount(), cmd.gradeLevel(), cmd.trimesterKey(),
                cmd.periodMonth(), cmd.dueAt(), currentActor());
        schedules.save(fs);
        return toSchedule(fs);
    }

    @Transactional(readOnly = true)
    public List<FeeScheduleResponse> listSchedules(UUID academicYearId) {
        return schedules.findByTenantIdAndAcademicYearIdOrderByDueAtAsc(tenant.current().value(), academicYearId)
                .stream().map(this::toSchedule).toList();
    }

    public ScholarshipResponse createScholarship(CreateScholarship cmd) {
        UUID tenantId = tenant.current().value();
        Scholarship s = new Scholarship(UUID.randomUUID(), tenantId, cmd.studentId(), cmd.kind(),
                cmd.percentage(), cmd.fixedAmount(), cmd.validFrom(), cmd.validTo(), cmd.reason());
        scholarships.save(s);
        return toScholarship(s);
    }

    @Transactional(readOnly = true)
    public List<ScholarshipResponse> listScholarships() {
        return scholarships.findByTenantIdOrderByCreatedAtDesc(tenant.current().value())
                .stream().map(this::toScholarship).toList();
    }

    /**
     * Fan out one invoice per enrolled student for the given fee schedule. Idempotent
     * on {@code (fee_schedule_id, student_id)} — running the batch twice skips
     * anyone already billed. Applies the student's active scholarship (if any) so
     * the net amount is what they actually owe.
     */
    public BillingResult runBilling(UUID feeScheduleId) {
        FeeSchedule fs = schedules.findById(feeScheduleId).orElseThrow(NotFoundException::new);
        if (!fs.tenantId().equals(tenant.current().value())) throw new NotFoundException();
        if (!fs.active()) {
            throw new ApplicationException(HttpStatus.CONFLICT, "error.conflict");
        }

        List<StudentDirectory.EnrolledStudent> pool = studentDirectory.listEnrolledForYear(fs.academicYearId());

        // Filter by grade level if the schedule targets one.
        if (fs.gradeLevel() != null && !fs.gradeLevel().isBlank()) {
            pool = pool.stream()
                    .filter(s -> fs.gradeLevel().equals(s.gradeLevel()))
                    .toList();
        }

        int issued = 0;
        int skipped = 0;
        int failed = 0;
        List<InvoiceResponse> issuedResponses = new ArrayList<>();

        LocalDate today = LocalDate.now(ZoneId.of("Africa/Luanda"));
        Map<UUID, Scholarship> scholarshipByStudent = new HashMap<>();

        for (StudentDirectory.EnrolledStudent s : pool) {
            try {
                if (invoices.existsByFeeScheduleIdAndStudentId(fs.id(), s.studentId())) {
                    skipped++;
                    continue;
                }
                Scholarship applied = pickScholarship(s.studentId(), today, scholarshipByStudent);
                BigDecimal discount = applied == null ? BigDecimal.ZERO : applied.discountFor(fs.amount());
                String reference = generateReference(today);
                Invoice invoice = new Invoice(UUID.randomUUID(), fs.tenantId(), s.studentId(),
                        fs.id(), reference, fs.name(), fs.amount(), discount, fs.dueAt(),
                        applied == null ? null : applied.id());
                invoices.save(invoice);
                audit.record("invoice.issue", "invoice", invoice.id().toString());
                events.publishEvent(new InvoiceIssued(UUID.randomUUID(), tenant.current(),
                        invoice.id(), invoice.studentId(), invoice.reference(),
                        invoice.amountNet(), invoice.dueAt(), Instant.now()));
                issuedResponses.add(toInvoice(invoice, s.fullName()));
                issued++;
            } catch (RuntimeException ex) {
                failed++;
            }
        }
        return new BillingResult(issued, skipped, failed, issuedResponses);
    }

    /**
     * Choose an active scholarship for a student on the billing date. If the student
     * has more than one active bolsa (edge case — a director stacks a full grant
     * and a percentage), we pick the highest discount so nobody loses out.
     */
    private Scholarship pickScholarship(UUID studentId, LocalDate today, Map<UUID, Scholarship> cache) {
        if (cache.containsKey(studentId)) return cache.get(studentId);
        List<Scholarship> candidates = scholarships.findByStudentIdAndActiveTrue(studentId);
        Scholarship best = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;
        BigDecimal probe = BigDecimal.valueOf(100_000L); // arbitrary — we compare ratios
        for (Scholarship c : candidates) {
            if (!c.isValidOn(today)) continue;
            BigDecimal d = c.discountFor(probe);
            if (d.compareTo(bestDiscount) > 0) {
                best = c;
                bestDiscount = d;
            }
        }
        cache.put(studentId, best);
        return best;
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoicesForStudent(UUID studentId) {
        List<Invoice> rows = invoices.findByStudentIdOrderByIssuedAtDesc(studentId);
        String name = studentDirectory.findStudent(studentId)
                .map(StudentDirectory.StudentSummary::fullName).orElse("?");
        return rows.stream().map(i -> toInvoice(i, name)).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID id) {
        Invoice i = invoices.findById(id).orElseThrow(NotFoundException::new);
        if (!i.tenantId().equals(tenant.current().value())) throw new NotFoundException();
        String name = studentDirectory.findStudent(i.studentId())
                .map(StudentDirectory.StudentSummary::fullName).orElse("?");
        return toInvoice(i, name);
    }

    private String generateReference(LocalDate date) {
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "INV-" + date.toString().replace("-", "") + "-" + suffix;
    }

    private FeeScheduleResponse toSchedule(FeeSchedule s) {
        return new FeeScheduleResponse(s.id(), s.academicYearId(), s.name(), s.kind(),
                s.amount(), s.currency(), s.gradeLevel(), s.trimesterKey(), s.periodMonth(),
                s.dueAt(), s.active(), s.createdAt());
    }

    private ScholarshipResponse toScholarship(Scholarship s) {
        return new ScholarshipResponse(s.id(), s.studentId(), s.kind(), s.percentage(),
                s.fixedAmount(), s.validFrom(), s.validTo(), s.reason(), s.active());
    }

    InvoiceResponse toInvoice(Invoice i, String studentName) {
        return new InvoiceResponse(i.id(), i.studentId(), i.feeScheduleId(), i.reference(),
                i.title(), i.amountGross(), i.amountDiscount(), i.amountNet(), i.amountPaid(),
                i.outstanding(), i.currency(), i.issuedAt(), i.dueAt(), i.status(), studentName);
    }

    private UUID currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SkoolPrincipal p) {
            try { return UUID.fromString(p.userId()); } catch (IllegalArgumentException ignored) { return null; }
        }
        return null;
    }
}
