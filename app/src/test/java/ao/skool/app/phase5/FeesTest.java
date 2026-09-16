package ao.skool.app.phase5;

import ao.skool.academic_structure.internal.domain.CurricularTrack;
import ao.skool.academic_structure.internal.domain.GradeLevel;
import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.AcademicYearResponse;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.CreateTurma;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.TurmaResponse;
import ao.skool.app.support.SchoolFixture;
import ao.skool.fees.api.PaymentMethod;
import ao.skool.fees.api.event.InvoiceIssued;
import ao.skool.fees.api.event.InvoiceOverdue;
import ao.skool.fees.api.event.PaymentReceived;
import ao.skool.fees.internal.domain.FeeKind;
import ao.skool.fees.internal.domain.InvoiceStatus;
import ao.skool.fees.internal.domain.ScholarshipKind;
import ao.skool.fees.internal.web.dto.FeeDtos.BillingResult;
import ao.skool.fees.internal.web.dto.FeeDtos.CreateFeeSchedule;
import ao.skool.fees.internal.web.dto.FeeDtos.CreatePayment;
import ao.skool.fees.internal.web.dto.FeeDtos.CreateScholarship;
import ao.skool.fees.internal.web.dto.FeeDtos.DefaulterRow;
import ao.skool.fees.internal.web.dto.FeeDtos.FeeScheduleResponse;
import ao.skool.fees.internal.web.dto.FeeDtos.InitiatePayment;
import ao.skool.fees.internal.web.dto.FeeDtos.InvoiceResponse;
import ao.skool.fees.internal.web.dto.FeeDtos.PaymentInitiation;
import ao.skool.fees.internal.web.dto.FeeDtos.PaymentResponse;
import ao.skool.fees.internal.web.dto.FeeDtos.ScholarshipResponse;
import ao.skool.sis.internal.domain.GuardianRelationship;
import ao.skool.sis.internal.web.dto.EnrollmentDtos.EnrollmentResponse;
import ao.skool.sis.internal.web.dto.GuardianDtos.CreateGuardian;
import ao.skool.sis.internal.web.dto.GuardianDtos.GuardianResponse;
import ao.skool.sis.internal.web.dto.StudentDtos.LinkGuardian;
import ao.skool.sis.internal.web.dto.StudentDtos.StudentResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 5 — propinas, bolsas, Kwanza invoicing, payments and the defaulter report.
 * <p>
 * Money that a school will chase a family for, so the arithmetic is worked by hand and
 * every state transition is pinned. The exit criterion from the plan — bill every
 * enrolled student in one run, then settle one invoice through the stub adapter and
 * see {@code PaymentReceived} fire — is {@link #billingIssuesOneInvoicePerEnrolledStudent}
 * plus {@link #initiateViaStubSettlesImmediately}.
 */
class FeesTest extends SchoolFixture {

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final BigDecimal PROPINA = new BigDecimal("25000.00");

    @Autowired private JdbcTemplate jdbc;

    // ------------------------------------------------------------- fixtures

    private record Cohort(UUID yearId, UUID turmaId, List<StudentResponse> students) {}

    private Cohort cohort(Actor admin, int size) throws Exception {
        AcademicYearResponse year = createAcademicYear(admin);
        TurmaResponse turma = createTurma(admin, year.id());
        List<StudentResponse> students = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            StudentResponse s = createStudent(admin, "Aluno " + i + " Neto");
            enrol(admin, s.id(), year.id(), turma.id());
            students.add(s);
        }
        return new Cohort(year.id(), turma.id(), students);
    }

    private FeeScheduleResponse schedule(Actor admin, UUID yearId, FeeKind kind, BigDecimal amount,
                                         String gradeLevel, Instant dueAt) throws Exception {
        var cmd = new CreateFeeSchedule(yearId, kind.name() + " " + SEQ.incrementAndGet(), kind, amount,
                gradeLevel, null, kind == FeeKind.PROPINA_MENSAL ? 9 : null, dueAt);
        return post(admin, "/api/fees/schedules", cmd, FeeScheduleResponse.class);
    }

    private FeeScheduleResponse propina(Actor admin, UUID yearId) throws Exception {
        return schedule(admin, yearId, FeeKind.PROPINA_MENSAL, PROPINA, null, Instant.now().plus(30, ChronoUnit.DAYS));
    }

    private BillingResult bill(Actor admin, UUID scheduleId) throws Exception {
        return post(admin, "/api/fees/schedules/" + scheduleId + "/run-billing", null, BillingResult.class);
    }

    private ScholarshipResponse scholarship(Actor admin, UUID studentId, ScholarshipKind kind,
                                            String percentage, String fixed, LocalDate validTo) throws Exception {
        var cmd = new CreateScholarship(studentId, kind,
                percentage == null ? null : new BigDecimal(percentage),
                fixed == null ? null : new BigDecimal(fixed),
                LocalDate.now().minusDays(1), validTo, "Bolsa de mérito");
        return post(admin, "/api/fees/scholarships", cmd, ScholarshipResponse.class);
    }

    private InvoiceResponse invoice(Actor admin, UUID invoiceId) throws Exception {
        return get(admin, "/api/fees/invoices/" + invoiceId, InvoiceResponse.class);
    }

    /** {@code POST /sweep-overdue} answers {@code {"flipped": n}}. */
    private int sweep(Actor admin) throws Exception {
        return post(admin, "/api/payments/sweep-overdue", null,
                new TypeReference<java.util.Map<String, Integer>>() {}).get("flipped");
    }

    private PaymentResponse pay(Actor admin, UUID invoiceId, String amount) throws Exception {
        return post(admin, "/api/payments",
                new CreatePayment(invoiceId, new BigDecimal(amount), PaymentMethod.STUB_MANUAL, null, null),
                PaymentResponse.class);
    }

    /** One billed invoice for a single enrolled student — the common starting point. */
    private InvoiceResponse oneBilledInvoice(Actor admin, Instant dueAt) throws Exception {
        Cohort c = cohort(admin, 1);
        FeeScheduleResponse fs = schedule(admin, c.yearId(), FeeKind.PROPINA_MENSAL, PROPINA, null, dueAt);
        return bill(admin, fs.id()).issued().getFirst();
    }

    // ------------------------------------------------------------ schedules

    @Test
    @DisplayName("a fee schedule is created in Kwanza and listed per academic year")
    void createsScheduleAndListsByYear() throws Exception {
        Actor admin = admin();
        AcademicYearResponse year = createAcademicYear(admin);
        AcademicYearResponse otherYear = createAcademicYear(admin);

        FeeScheduleResponse fs = propina(admin, year.id());

        assertThat(fs.kind()).isEqualTo(FeeKind.PROPINA_MENSAL);
        assertThat(fs.amount()).isEqualByComparingTo(PROPINA);
        assertThat(fs.currency()).isEqualTo("AOA");
        assertThat(fs.periodMonth()).isEqualTo(9);
        assertThat(fs.active()).isTrue();

        assertThat(get(admin, "/api/fees/schedules?academicYearId=" + year.id(),
                new TypeReference<List<FeeScheduleResponse>>() {}))
                .extracting(FeeScheduleResponse::id).contains(fs.id());
        assertThat(get(admin, "/api/fees/schedules?academicYearId=" + otherYear.id(),
                new TypeReference<List<FeeScheduleResponse>>() {})).isEmpty();
    }

    @Test
    @DisplayName("a negative amount or an out-of-range month is rejected")
    void validatesScheduleInput() throws Exception {
        Actor admin = admin();
        AcademicYearResponse year = createAcademicYear(admin);
        Instant due = Instant.now().plus(1, ChronoUnit.DAYS);

        assertThat(postStatus(admin, "/api/fees/schedules", new CreateFeeSchedule(year.id(), "Neg",
                FeeKind.PROPINA_MENSAL, new BigDecimal("-1.00"), null, null, 9, due))).isEqualTo(400);
        assertThat(postStatus(admin, "/api/fees/schedules", new CreateFeeSchedule(year.id(), "Mês 13",
                FeeKind.PROPINA_MENSAL, PROPINA, null, null, 13, due))).isEqualTo(400);
    }

    // -------------------------------------------------------------- billing

    @Test
    @DisplayName("running billing issues exactly one invoice per enrolled student")
    void billingIssuesOneInvoicePerEnrolledStudent() throws Exception {
        Actor admin = admin();
        Cohort c = cohort(admin, 3);
        FeeScheduleResponse fs = propina(admin, c.yearId());

        events.clear();
        BillingResult result = bill(admin, fs.id());

        assertThat(result.issuedCount()).isEqualTo(3);
        assertThat(result.skippedCount()).isZero();
        assertThat(result.failedCount()).isZero();
        assertThat(result.issued()).extracting(InvoiceResponse::studentId)
                .containsExactlyInAnyOrderElementsOf(c.students().stream().map(StudentResponse::id).toList());
        assertThat(result.issued()).allSatisfy(i -> {
            assertThat(i.amountGross()).isEqualByComparingTo(PROPINA);
            assertThat(i.amountDiscount()).isEqualByComparingTo("0");
            assertThat(i.amountNet()).isEqualByComparingTo(PROPINA);
            assertThat(i.amountPaid()).isEqualByComparingTo("0");
            assertThat(i.outstanding()).isEqualByComparingTo(PROPINA);
            assertThat(i.status()).isEqualTo(InvoiceStatus.ISSUED);
            assertThat(i.currency()).isEqualTo("AOA");
            assertThat(i.reference()).matches("INV-\\d{8}-[0-9A-F]{4}");
            assertThat(i.studentName()).startsWith("Aluno ");
        });

        assertThat(events.ofType(InvoiceIssued.class)).hasSize(3)
                .allSatisfy(e -> assertThat(e.amountNet()).isEqualByComparingTo(PROPINA));

        Long audited = jdbc.queryForObject(
                "select count(*) from audit_log where tenant_id = ? and action = 'invoice.issue'", Long.class, tenant);
        assertThat(audited).isEqualTo(3);
    }

    @Test
    @DisplayName("running billing twice skips everyone already billed")
    void billingIsIdempotent() throws Exception {
        Actor admin = admin();
        Cohort c = cohort(admin, 3);
        FeeScheduleResponse fs = propina(admin, c.yearId());
        bill(admin, fs.id());

        events.clear();
        BillingResult again = bill(admin, fs.id());

        assertThat(again.issuedCount()).isZero();
        assertThat(again.skippedCount()).isEqualTo(3);
        assertThat(again.failedCount()).isZero();
        assertThat(events.ofType(InvoiceIssued.class)).as("no second invoice, no second event").isEmpty();

        // A different schedule for the same students is a fresh bill, not a skip.
        FeeScheduleResponse matricula = schedule(admin, c.yearId(), FeeKind.MATRICULA,
                new BigDecimal("50000.00"), null, Instant.now().plus(10, ChronoUnit.DAYS));
        assertThat(bill(admin, matricula.id()).issuedCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("a schedule targeting one grade level bills only that grade")
    void billingRespectsGradeLevel() throws Exception {
        Actor admin = admin();
        Cohort tenth = cohort(admin, 2);                                   // CLASSE_10 turma
        TurmaResponse seventh = post(admin, "/api/turmas", new CreateTurma(UUID.randomUUID(), tenth.yearId(),
                "7ª A", GradeLevel.CLASSE_7, CurricularTrack.GERAL, 30), TurmaResponse.class);
        StudentResponse younger = createStudent(admin, "Aluno Sétima");
        enrol(admin, younger.id(), tenth.yearId(), seventh.id());

        FeeScheduleResponse onlyTenth = schedule(admin, tenth.yearId(), FeeKind.EXAME,
                new BigDecimal("5000.00"), "CLASSE_10", Instant.now().plus(5, ChronoUnit.DAYS));
        BillingResult result = bill(admin, onlyTenth.id());

        assertThat(result.issuedCount()).isEqualTo(2);
        assertThat(result.issued()).extracting(InvoiceResponse::studentId).doesNotContain(younger.id());
    }

    @Test
    @DisplayName("a withdrawn student is not billed")
    void billingSkipsWithdrawnStudents() throws Exception {
        Actor admin = admin();
        Cohort c = cohort(admin, 3);
        StudentResponse leaver = c.students().getFirst();
        EnrollmentResponse enrolment = get(admin, "/api/enrollments?academicYearId=" + c.yearId(),
                new TypeReference<List<EnrollmentResponse>>() {}).stream()
                .filter(e -> e.studentId().equals(leaver.id())).findFirst().orElseThrow();
        post(admin, "/api/enrollments/" + enrolment.id() + "/withdraw", null, EnrollmentResponse.class);

        BillingResult result = bill(admin, propina(admin, c.yearId()).id());

        assertThat(result.issuedCount()).isEqualTo(2);
        assertThat(result.issued()).extracting(InvoiceResponse::studentId).doesNotContain(leaver.id());
    }

    @Test
    @DisplayName("billing another school's schedule is not found")
    void billingIsTenantScoped() throws Exception {
        Actor admin = admin();
        Cohort c = cohort(admin, 1);
        FeeScheduleResponse fs = propina(admin, c.yearId());

        Actor outsider = actorInOtherTenant("Admin Alheio", "ADMIN");
        assertThat(postStatus(outsider, "/api/fees/schedules/" + fs.id() + "/run-billing", null)).isEqualTo(404);
    }

    // --------------------------------------------------------- scholarships

    @Test
    @DisplayName("a percentage bolsa discounts the invoice")
    void percentageScholarship() throws Exception {
        Actor admin = admin();
        Cohort c = cohort(admin, 1);
        UUID student = c.students().getFirst().id();
        scholarship(admin, student, ScholarshipKind.PERCENTAGE, "50.00", null, null);

        InvoiceResponse inv = bill(admin, propina(admin, c.yearId()).id()).issued().getFirst();

        assertThat(inv.amountDiscount()).isEqualByComparingTo("12500.00");   // 50% of 25 000
        assertThat(inv.amountNet()).isEqualByComparingTo("12500.00");
        assertThat(inv.outstanding()).isEqualByComparingTo("12500.00");
    }

    @Test
    @DisplayName("a full bolsa brings the invoice to zero")
    void fullScholarship() throws Exception {
        Actor admin = admin();
        Cohort c = cohort(admin, 1);
        scholarship(admin, c.students().getFirst().id(), ScholarshipKind.FULL, null, null, null);

        InvoiceResponse inv = bill(admin, propina(admin, c.yearId()).id()).issued().getFirst();

        assertThat(inv.amountDiscount()).isEqualByComparingTo(PROPINA);
        assertThat(inv.amountNet()).isEqualByComparingTo("0");
        assertThat(events.onlyOne(InvoiceIssued.class).amountNet()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("a fixed bolsa never exceeds the invoice — no negative propinas")
    void fixedScholarshipIsCapped() throws Exception {
        Actor admin = admin();
        Cohort c = cohort(admin, 1);
        scholarship(admin, c.students().getFirst().id(), ScholarshipKind.FIXED, null, "40000.00", null);

        InvoiceResponse inv = bill(admin, propina(admin, c.yearId()).id()).issued().getFirst();

        assertThat(inv.amountDiscount()).isEqualByComparingTo(PROPINA);
        assertThat(inv.amountNet()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("an expired bolsa is ignored")
    void expiredScholarshipIsIgnored() throws Exception {
        Actor admin = admin();
        Cohort c = cohort(admin, 1);
        scholarship(admin, c.students().getFirst().id(), ScholarshipKind.FULL, null, null, LocalDate.now().minusDays(1));

        InvoiceResponse inv = bill(admin, propina(admin, c.yearId()).id()).issued().getFirst();

        assertThat(inv.amountDiscount()).isEqualByComparingTo("0");
        assertThat(inv.amountNet()).isEqualByComparingTo(PROPINA);
    }

    @Test
    @DisplayName("with two active bolsas the student gets whichever discounts this invoice more")
    void bestScholarshipWinsOnTheActualAmount() throws Exception {
        Actor admin = admin();
        Cohort c = cohort(admin, 1);
        UUID student = c.students().getFirst().id();
        // On a 25 000 propina: 40% = 10 000, fixed 30 000 caps to 25 000 → fixed must win.
        scholarship(admin, student, ScholarshipKind.PERCENTAGE, "40.00", null, null);
        scholarship(admin, student, ScholarshipKind.FIXED, null, "30000.00", null);

        InvoiceResponse inv = bill(admin, propina(admin, c.yearId()).id()).issued().getFirst();

        assertThat(inv.amountDiscount())
                .as("the comparison must be made on the real invoice amount, not a probe")
                .isEqualByComparingTo("25000.00");
    }

    // ------------------------------------------------------------- payments

    @Test
    @DisplayName("a partial payment leaves the invoice PARTIAL; the balance closes it")
    void partialThenFullPayment() throws Exception {
        Actor admin = admin();
        InvoiceResponse inv = oneBilledInvoice(admin, Instant.now().plus(30, ChronoUnit.DAYS));

        events.clear();
        PaymentResponse first = pay(admin, inv.id(), "10000.00");
        assertThat(first.amount()).isEqualByComparingTo("10000.00");
        assertThat(first.currency()).isEqualTo("AOA");
        assertThat(first.method()).isEqualTo(PaymentMethod.STUB_MANUAL);

        InvoiceResponse afterFirst = invoice(admin, inv.id());
        assertThat(afterFirst.status()).isEqualTo(InvoiceStatus.PARTIAL);
        assertThat(afterFirst.amountPaid()).isEqualByComparingTo("10000.00");
        assertThat(afterFirst.outstanding()).isEqualByComparingTo("15000.00");

        PaymentReceived firstEvent = events.onlyOne(PaymentReceived.class);
        assertThat(firstEvent.invoiceId()).isEqualTo(inv.id());
        assertThat(firstEvent.amount()).isEqualByComparingTo("10000.00");
        assertThat(firstEvent.invoiceFullyPaid()).isFalse();

        events.clear();
        pay(admin, inv.id(), "15000.00");

        InvoiceResponse settled = invoice(admin, inv.id());
        assertThat(settled.status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(settled.outstanding()).isEqualByComparingTo("0");
        assertThat(events.onlyOne(PaymentReceived.class).invoiceFullyPaid()).isTrue();

        assertThat(get(admin, "/api/payments?invoiceId=" + inv.id(), new TypeReference<List<PaymentResponse>>() {}))
                .extracting(PaymentResponse::amount)
                .satisfiesExactly(
                        a -> assertThat((BigDecimal) a).isEqualByComparingTo("10000.00"),
                        a -> assertThat((BigDecimal) a).isEqualByComparingTo("15000.00"));

        Long audited = jdbc.queryForObject(
                "select count(*) from audit_log where target_id = ? and action = 'payment.record'",
                Long.class, inv.id().toString());
        assertThat(audited).isEqualTo(2);
    }

    @Test
    @DisplayName("overpayment is refused and the invoice is untouched")
    void overpaymentIsRefused() throws Exception {
        Actor admin = admin();
        InvoiceResponse inv = oneBilledInvoice(admin, Instant.now().plus(30, ChronoUnit.DAYS));

        events.clear();
        assertThat(postStatus(admin, "/api/payments",
                new CreatePayment(inv.id(), new BigDecimal("25000.01"), PaymentMethod.STUB_MANUAL, null, null)))
                .isEqualTo(400);

        InvoiceResponse unchanged = invoice(admin, inv.id());
        assertThat(unchanged.status()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(unchanged.amountPaid()).isEqualByComparingTo("0");
        assertThat(events.ofType(PaymentReceived.class)).isEmpty();
        // Paying to the cêntimo is fine.
        assertThat(postStatus(admin, "/api/payments",
                new CreatePayment(inv.id(), new BigDecimal("25000.00"), PaymentMethod.STUB_MANUAL, null, null)))
                .isEqualTo(200);
    }

    @Test
    @DisplayName("a zero or negative payment is rejected")
    void rejectsNonPositivePayment() throws Exception {
        Actor admin = admin();
        InvoiceResponse inv = oneBilledInvoice(admin, Instant.now().plus(30, ChronoUnit.DAYS));

        assertThat(postStatus(admin, "/api/payments",
                new CreatePayment(inv.id(), BigDecimal.ZERO, PaymentMethod.STUB_MANUAL, null, null))).isEqualTo(400);
        assertThat(postStatus(admin, "/api/payments",
                new CreatePayment(inv.id(), new BigDecimal("-5.00"), PaymentMethod.STUB_MANUAL, null, null))).isEqualTo(400);
    }

    @Test
    @DisplayName("initiating via the stub settles the invoice immediately — the plan's exit criterion")
    void initiateViaStubSettlesImmediately() throws Exception {
        Actor admin = admin();
        InvoiceResponse inv = oneBilledInvoice(admin, Instant.now().plus(30, ChronoUnit.DAYS));

        events.clear();
        PaymentInitiation init = post(admin, "/api/payments/initiate",
                new InitiatePayment(inv.id(), PaymentMethod.STUB_MANUAL, null), PaymentInitiation.class);

        assertThat(init.method()).isEqualTo(PaymentMethod.STUB_MANUAL);
        assertThat(init.vendorReference()).startsWith("MANUAL-");
        assertThat(invoice(admin, inv.id()).status()).isEqualTo(InvoiceStatus.PAID);

        PaymentReceived event = events.onlyOne(PaymentReceived.class);
        assertThat(event.invoiceFullyPaid()).isTrue();
        assertThat(event.amount()).isEqualByComparingTo(PROPINA);
        assertThat(event.method()).isEqualTo(PaymentMethod.STUB_MANUAL);
    }

    @Test
    @DisplayName("Multicaixa Express returns an ATM reference and leaves the invoice open")
    void initiateViaMulticaixaReturnsReferenceWithoutSettling() throws Exception {
        Actor admin = admin();
        InvoiceResponse inv = oneBilledInvoice(admin, Instant.now().plus(30, ChronoUnit.DAYS));

        events.clear();
        PaymentInitiation init = post(admin, "/api/payments/initiate",
                new InitiatePayment(inv.id(), PaymentMethod.MULTICAIXA_EXPRESS, "+244923000111"),
                PaymentInitiation.class);

        assertThat(init.vendorReference()).matches("MCX-\\d{9}");
        assertThat(init.instructions()).contains("Entidade 11333").contains(init.vendorReference()).contains("25000.00 Kz");
        assertThat(invoice(admin, inv.id()).status())
                .as("nothing is captured until the vendor confirms").isEqualTo(InvoiceStatus.ISSUED);
        assertThat(events.ofType(PaymentReceived.class)).isEmpty();
    }

    @Test
    @DisplayName("every skeleton adapter yields a reference and instructions without settling")
    void skeletonAdaptersYieldReferences() throws Exception {
        Actor admin = admin();
        InvoiceResponse inv = oneBilledInvoice(admin, Instant.now().plus(30, ChronoUnit.DAYS));

        for (PaymentMethod method : List.of(PaymentMethod.UNITEL_MONEY, PaymentMethod.AFRICELL_MONEY,
                PaymentMethod.BANK_TRANSFER)) {
            PaymentInitiation init = post(admin, "/api/payments/initiate",
                    new InitiatePayment(inv.id(), method, "+244923000111"), PaymentInitiation.class);
            assertThat(init.method()).isEqualTo(method);
            assertThat(init.vendorReference()).as(method.name()).isNotBlank();
            assertThat(init.instructions()).as(method.name()).isNotBlank();
        }
        PaymentInitiation bank = post(admin, "/api/payments/initiate",
                new InitiatePayment(inv.id(), PaymentMethod.BANK_TRANSFER, null), PaymentInitiation.class);
        assertThat(bank.instructions()).contains("IBAN").contains(inv.reference());

        assertThat(invoice(admin, inv.id()).status()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(events.ofType(PaymentReceived.class)).isEmpty();
    }

    @Test
    @DisplayName("a paid invoice cannot be initiated again")
    void initiateOnPaidInvoiceIsConflict() throws Exception {
        Actor admin = admin();
        InvoiceResponse inv = oneBilledInvoice(admin, Instant.now().plus(30, ChronoUnit.DAYS));
        pay(admin, inv.id(), "25000.00");

        assertThat(postStatus(admin, "/api/payments/initiate",
                new InitiatePayment(inv.id(), PaymentMethod.MULTICAIXA_EXPRESS, null))).isEqualTo(409);
    }

    // -------------------------------------------------------------- overdue

    @Test
    @DisplayName("the sweep flips past-due invoices to OVERDUE and publishes once")
    void sweepFlipsPastDueOnce() throws Exception {
        Actor admin = admin();
        InvoiceResponse inv = oneBilledInvoice(admin, Instant.now().minus(3, ChronoUnit.DAYS));

        events.clear();
        Integer flipped = sweep(admin);
        assertThat(flipped).isEqualTo(1);
        assertThat(invoice(admin, inv.id()).status()).isEqualTo(InvoiceStatus.OVERDUE);

        InvoiceOverdue event = events.onlyOne(InvoiceOverdue.class);
        assertThat(event.invoiceId()).isEqualTo(inv.id());
        assertThat(event.outstanding()).isEqualByComparingTo(PROPINA);
        assertThat(event.daysPastDue()).isGreaterThanOrEqualTo(3);

        // Hourly re-runs must not spam the guardian.
        events.clear();
        assertThat(sweep(admin)).isZero();
        assertThat(events.ofType(InvoiceOverdue.class)).isEmpty();
    }

    @Test
    @DisplayName("the sweep leaves paid and not-yet-due invoices alone")
    void sweepIgnoresPaidAndFuture() throws Exception {
        Actor admin = admin();
        InvoiceResponse paidLate = oneBilledInvoice(admin, Instant.now().minus(3, ChronoUnit.DAYS));
        pay(admin, paidLate.id(), "25000.00");
        InvoiceResponse future = oneBilledInvoice(admin, Instant.now().plus(3, ChronoUnit.DAYS));

        assertThat(sweep(admin)).isZero();
        assertThat(invoice(admin, paidLate.id()).status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice(admin, future.id()).status()).isEqualTo(InvoiceStatus.ISSUED);
    }

    @Test
    @DisplayName("paying an overdue invoice clears it")
    void payingAnOverdueInvoiceClearsIt() throws Exception {
        Actor admin = admin();
        InvoiceResponse inv = oneBilledInvoice(admin, Instant.now().minus(3, ChronoUnit.DAYS));
        sweep(admin);

        pay(admin, inv.id(), "25000.00");

        assertThat(invoice(admin, inv.id()).status()).isEqualTo(InvoiceStatus.PAID);
        assertThat(get(admin, "/api/payments/defaulters", new TypeReference<List<DefaulterRow>>() {}))
                .extracting(DefaulterRow::studentId).doesNotContain(inv.studentId());
    }

    // ----------------------------------------------------------- defaulters

    @Test
    @DisplayName("the defaulter report aggregates per student, biggest debt first")
    void defaultersAggregatePerStudent() throws Exception {
        Actor admin = admin();
        Cohort c = cohort(admin, 3);
        UUID big = c.students().get(0).id();
        UUID small = c.students().get(1).id();
        UUID clean = c.students().get(2).id();

        // Stored as timestamp(6): microsecond precision, same as Postgres timestamptz.
        Instant pastA = Instant.now().minus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
        Instant pastB = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
        FeeScheduleResponse propina = schedule(admin, c.yearId(), FeeKind.PROPINA_MENSAL, PROPINA, null, pastA);
        FeeScheduleResponse matricula = schedule(admin, c.yearId(), FeeKind.MATRICULA, new BigDecimal("50000.00"), null, pastB);
        List<InvoiceResponse> propinas = bill(admin, propina.id()).issued();
        List<InvoiceResponse> matriculas = bill(admin, matricula.id()).issued();

        // small pays the matrícula; clean pays everything; big pays nothing.
        pay(admin, matriculas.stream().filter(i -> i.studentId().equals(small)).findFirst().orElseThrow().id(), "50000.00");
        for (InvoiceResponse i : propinas) if (i.studentId().equals(clean)) pay(admin, i.id(), "25000.00");
        for (InvoiceResponse i : matriculas) if (i.studentId().equals(clean)) pay(admin, i.id(), "50000.00");

        List<DefaulterRow> rows = get(admin, "/api/payments/defaulters", new TypeReference<>() {});

        assertThat(rows).extracting(DefaulterRow::studentId).containsExactly(big, small);
        DefaulterRow bigRow = rows.getFirst();
        assertThat(bigRow.overdueInvoiceCount()).isEqualTo(2);
        assertThat(bigRow.totalOutstanding()).isEqualByComparingTo("75000.00");
        assertThat(bigRow.oldestDueAt()).isEqualTo(pastA);
        assertThat(bigRow.studentName()).isEqualTo("Aluno 1 Neto");
        DefaulterRow smallRow = rows.get(1);
        assertThat(smallRow.overdueInvoiceCount()).isEqualTo(1);
        assertThat(smallRow.totalOutstanding()).isEqualByComparingTo("25000.00");
    }

    @Test
    @DisplayName("a partial payment reduces the defaulter's outstanding, not the count")
    void partialPaymentReducesOutstanding() throws Exception {
        Actor admin = admin();
        InvoiceResponse inv = oneBilledInvoice(admin, Instant.now().minus(3, ChronoUnit.DAYS));
        pay(admin, inv.id(), "5000.00");

        List<DefaulterRow> rows = get(admin, "/api/payments/defaulters", new TypeReference<>() {});

        assertThat(rows).singleElement().satisfies(r -> {
            assertThat(r.overdueInvoiceCount()).isEqualTo(1);
            assertThat(r.totalOutstanding()).isEqualByComparingTo("20000.00");
        });
    }

    // ------------------------------------------------------ guardian portal

    @Test
    @DisplayName("a guardian sees invoices for their own children only")
    void guardianSeesOnlyOwnChildrensInvoices() throws Exception {
        Actor admin = admin();
        Cohort c = cohort(admin, 2);
        UUID mine = c.students().get(0).id();
        UUID someoneElses = c.students().get(1).id();
        List<InvoiceResponse> all = bill(admin, propina(admin, c.yearId()).id()).issued();

        GuardianResponse guardian = post(admin, "/api/guardians", new CreateGuardian("Maria Neto",
                "+244923000111", "maria" + SEQ.incrementAndGet() + "@encarregado.test", null, true),
                GuardianResponse.class);
        post(admin, "/api/students/" + mine + "/guardians",
                new LinkGuardian(guardian.id(), GuardianRelationship.MAE, true, true), StudentResponse.class);

        List<InvoiceResponse> seen = get(guardian(guardian.userId()), "/api/guardian/fees/invoices",
                new TypeReference<>() {});

        assertThat(seen).extracting(InvoiceResponse::studentId).containsExactly(mine).doesNotContain(someoneElses);
        assertThat(seen.getFirst().id()).isEqualTo(all.stream().filter(i -> i.studentId().equals(mine))
                .findFirst().orElseThrow().id());
    }

    @Test
    @DisplayName("a guardian with no portal record gets an empty list, not an error")
    void unlinkedGuardianSeesNothing() throws Exception {
        assertThat(get(guardian(UUID.randomUUID()), "/api/guardian/fees/invoices",
                new TypeReference<List<InvoiceResponse>>() {})).isEmpty();
    }

    // ------------------------------------------------ tenant + role gating

    @Test
    @DisplayName("another school cannot read a student's invoices or an invoice's payments")
    void invoiceReadsAreTenantScoped() throws Exception {
        Actor admin = admin();
        InvoiceResponse inv = oneBilledInvoice(admin, Instant.now().plus(30, ChronoUnit.DAYS));
        pay(admin, inv.id(), "5000.00");

        Actor outsider = actorInOtherTenant("Admin Alheio", "ADMIN");

        assertThat(getStatus(outsider, "/api/fees/invoices/" + inv.id())).isEqualTo(404);
        assertThat(getStatus(outsider, "/api/fees/invoices?studentId=" + inv.studentId()))
                .as("listing by a foreign student id must not return that student's invoices")
                .isEqualTo(404);
        assertThat(getStatus(outsider, "/api/payments?invoiceId=" + inv.id()))
                .as("listing payments by a foreign invoice id must not return them")
                .isEqualTo(404);
        assertThat(postStatus(outsider, "/api/payments",
                new CreatePayment(inv.id(), new BigDecimal("1.00"), PaymentMethod.STUB_MANUAL, null, null)))
                .isEqualTo(404);
        assertThat(postStatus(outsider, "/api/payments/initiate",
                new InitiatePayment(inv.id(), PaymentMethod.STUB_MANUAL, null))).isEqualTo(404);
    }

    @Test
    @DisplayName("billing and defaulter reports are for the secretaria, not teachers or families")
    void feesAreAdminWork() throws Exception {
        Actor admin = admin();
        Cohort c = cohort(admin, 1);
        FeeScheduleResponse fs = propina(admin, c.yearId());
        InvoiceResponse inv = bill(admin, fs.id()).issued().getFirst();

        Actor teacher = teacher();
        Actor student = student(UUID.randomUUID());
        Actor guardian = guardian(UUID.randomUUID());

        assertThat(postStatus(teacher, "/api/fees/schedules", new CreateFeeSchedule(c.yearId(), "X",
                FeeKind.PROPINA_MENSAL, PROPINA, null, null, 9, Instant.now()))).isEqualTo(403);
        assertThat(postStatus(guardian, "/api/fees/schedules/" + fs.id() + "/run-billing", null)).isEqualTo(403);
        assertThat(getStatus(student, "/api/payments/defaulters")).isEqualTo(403);
        assertThat(postStatus(guardian, "/api/payments",
                new CreatePayment(inv.id(), new BigDecimal("1.00"), PaymentMethod.STUB_MANUAL, null, null)))
                .as("a guardian cannot mark their own invoice paid").isEqualTo(403);
        // …but a guardian may start a real payment, and a secretary may do all of it.
        assertThat(postStatus(guardian, "/api/payments/initiate",
                new InitiatePayment(inv.id(), PaymentMethod.MULTICAIXA_EXPRESS, null))).isEqualTo(200);
        assertThat(postStatus(secretary(), "/api/payments",
                new CreatePayment(inv.id(), new BigDecimal("1.00"), PaymentMethod.STUB_MANUAL, null, null)))
                .isEqualTo(200);
    }
}
