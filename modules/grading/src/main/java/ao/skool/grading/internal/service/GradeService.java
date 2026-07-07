package ao.skool.grading.internal.service;

import ao.skool.academic_structure.api.AcademicStructureQuery;
import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.common.security.audit.AuditLogWriter;
import ao.skool.common.web.error.ApplicationException;
import ao.skool.grading.api.event.GradePosted;
import ao.skool.grading.internal.domain.Grade;
import ao.skool.grading.internal.persistence.GradeRepository;
import ao.skool.grading.internal.web.dto.GradeDtos.BatchGrades;
import ao.skool.grading.internal.web.dto.GradeDtos.CreateGrade;
import ao.skool.grading.internal.web.dto.GradeDtos.GradeResponse;
import ao.skool.grading.internal.web.dto.GradeDtos.StudentGradeSummary;
import ao.skool.grading.internal.web.dto.GradeDtos.TrimesterSubjectAverage;
import ao.skool.sis.api.StudentDirectory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@Transactional
public class GradeService {

    static final BigDecimal MAX = new BigDecimal("20.00");
    static final BigDecimal MIN = BigDecimal.ZERO;

    private final GradeRepository grades;
    private final TenantContext tenant;
    private final ApplicationEventPublisher events;
    private final AuditLogWriter audit;
    private final StudentDirectory studentDirectory;
    private final AcademicStructureQuery structure;

    public GradeService(GradeRepository grades, TenantContext tenant,
                        ApplicationEventPublisher events, AuditLogWriter audit,
                        StudentDirectory studentDirectory, AcademicStructureQuery structure) {
        this.grades = grades;
        this.tenant = tenant;
        this.events = events;
        this.audit = audit;
        this.studentDirectory = studentDirectory;
        this.structure = structure;
    }

    public GradeResponse record(CreateGrade cmd) {
        var tenantId = tenant.current();
        validate(cmd);
        UUID id = UUID.randomUUID();
        Grade grade = new Grade(id, tenantId.value(), cmd.studentId(), cmd.subjectId(),
                cmd.turmaId(), cmd.academicYearId(), cmd.trimesterKey(),
                cmd.value(), cmd.weight(), cmd.category(), cmd.notes(), currentActor());
        grades.save(grade);
        audit.record("grade.post", "grade", id.toString());
        events.publishEvent(new GradePosted(
                UUID.randomUUID(), tenantId, id, cmd.studentId(), cmd.subjectId(),
                cmd.trimesterKey(), cmd.value(), Instant.now()));
        return toResponse(grade);
    }

    public List<GradeResponse> recordBatch(BatchGrades cmd) {
        List<GradeResponse> results = new ArrayList<>();
        for (CreateGrade g : cmd.grades()) results.add(record(g));
        return results;
    }

    @Transactional(readOnly = true)
    public StudentGradeSummary summaryFor(UUID studentId, UUID academicYearId) {
        var student = studentDirectory.findStudent(studentId)
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "error.not_found"));
        var year = structure.findAcademicYear(academicYearId)
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "error.not_found"));

        List<Grade> all = grades.findByStudentIdAndAcademicYearIdOrderByTrimesterKeyAscSubjectIdAsc(
                studentId, academicYearId);

        // Group by (trimester, subject) → list of grades
        Map<String, Map<UUID, List<Grade>>> byTrimSubj = new LinkedHashMap<>();
        for (Grade g : all) {
            byTrimSubj.computeIfAbsent(g.trimesterKey(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(g.subjectId(), k -> new ArrayList<>())
                    .add(g);
        }

        List<TrimesterSubjectAverage> subjectAverages = new ArrayList<>();
        Map<String, BigDecimal> trimesterAverages = new TreeMap<>();
        int trimesterCount = 0;
        BigDecimal totalOverAllTrimesters = BigDecimal.ZERO;

        for (var trimEntry : byTrimSubj.entrySet()) {
            String trim = trimEntry.getKey();
            BigDecimal trimTotal = BigDecimal.ZERO;
            int subjectCount = 0;
            for (var subjEntry : trimEntry.getValue().entrySet()) {
                UUID subjectId = subjEntry.getKey();
                var avg = weightedAverage(subjEntry.getValue());
                String subjectName = structure.findSubject(subjectId).map(AcademicStructureQuery.SubjectView::name).orElse("?");
                subjectAverages.add(new TrimesterSubjectAverage(trim, subjectId, subjectName, avg));
                trimTotal = trimTotal.add(avg);
                subjectCount++;
            }
            if (subjectCount > 0) {
                BigDecimal trimAvg = trimTotal.divide(BigDecimal.valueOf(subjectCount), 2, RoundingMode.HALF_UP);
                trimesterAverages.put(trim, trimAvg);
                totalOverAllTrimesters = totalOverAllTrimesters.add(trimAvg);
                trimesterCount++;
            }
        }
        BigDecimal finalAverage = trimesterCount == 0
                ? BigDecimal.ZERO
                : totalOverAllTrimesters.divide(BigDecimal.valueOf(trimesterCount), 2, RoundingMode.HALF_UP);

        return new StudentGradeSummary(
                student.id(), student.fullName(),
                year.id(), year.name(),
                subjectAverages, trimesterAverages, finalAverage);
    }

    @Transactional(readOnly = true)
    List<Grade> rawGradesFor(UUID studentId, UUID academicYearId) {
        return grades.findByStudentIdAndAcademicYearIdOrderByTrimesterKeyAscSubjectIdAsc(studentId, academicYearId);
    }

    private BigDecimal weightedAverage(List<Grade> entries) {
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal weightedSum = BigDecimal.ZERO;
        for (Grade g : entries) {
            weightedSum = weightedSum.add(g.value().multiply(g.weight()));
            totalWeight = totalWeight.add(g.weight());
        }
        return totalWeight.signum() == 0
                ? BigDecimal.ZERO
                : weightedSum.divide(totalWeight, 2, RoundingMode.HALF_UP);
    }

    private void validate(CreateGrade cmd) {
        if (cmd.value().compareTo(MIN) < 0 || cmd.value().compareTo(MAX) > 0) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "error.validation");
        }
    }

    private UUID currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SkoolPrincipal p) {
            try { return UUID.fromString(p.userId()); } catch (IllegalArgumentException ignored) { return null; }
        }
        return null;
    }

    private GradeResponse toResponse(Grade g) {
        return new GradeResponse(g.id(), g.studentId(), g.subjectId(), g.turmaId(),
                g.trimesterKey(), g.value(), g.weight(), g.category(), g.notes(), g.recordedAt());
    }
}
