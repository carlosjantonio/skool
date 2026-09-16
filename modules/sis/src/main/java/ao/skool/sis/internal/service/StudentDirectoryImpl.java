package ao.skool.sis.internal.service;

import ao.skool.academic_structure.api.AcademicStructureQuery;
import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.sis.api.StudentDirectory;
import ao.skool.sis.internal.domain.Enrollment;
import ao.skool.sis.internal.domain.EnrollmentStatus;
import ao.skool.sis.internal.domain.Student;
import ao.skool.sis.internal.persistence.EnrollmentRepository;
import ao.skool.sis.internal.persistence.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StudentDirectoryImpl implements StudentDirectory {

    private final StudentRepository students;
    private final EnrollmentRepository enrollments;
    private final AcademicStructureQuery structure;
    private final TenantContext tenant;

    public StudentDirectoryImpl(StudentRepository students,
                                 EnrollmentRepository enrollments,
                                 AcademicStructureQuery structure,
                                 TenantContext tenant) {
        this.students = students;
        this.enrollments = enrollments;
        this.structure = structure;
        this.tenant = tenant;
    }

    /**
     * Tenant-filtered on purpose. Callers pass a student id that arrived in a URL
     * ({@code /api/grades/students/{id}/summary}, the assignments grading view, the fees
     * defaulter report), and without this check a teacher at one school who held another
     * school's student id would read that student's name and marks. The ids are opaque
     * UUIDs so this was never trivially reachable, but tenant isolation should not rest on
     * an id being hard to guess — especially for minors' records under Lei 22/11.
     * <p>
     * Returning empty rather than throwing keeps the cross-module contract unchanged:
     * callers already map empty to 404, which is also the right answer here — another
     * school's student should be indistinguishable from one that does not exist.
     */
    @Override
    public Optional<StudentSummary> findStudent(UUID studentId) {
        UUID tenantId = tenant.current().value();
        return students.findById(studentId)
                .filter(s -> s.tenantId().equals(tenantId))
                .map(this::toSummary);
    }

    @Override
    public Optional<StudentSummary> findStudentByUserId(UUID userId) {
        UUID tenantId = tenant.current().value();
        return students.findByUserId(userId)
                .filter(s -> s.tenantId().equals(tenantId))
                .map(this::toSummary);
    }

    @Override
    public List<EnrolledStudent> listEnrolledForYear(UUID academicYearId) {
        UUID tenantId = tenant.current().value();
        List<Enrollment> rows = enrollments.findByTenantIdAndAcademicYearId(tenantId, academicYearId).stream()
                .filter(e -> e.status() == EnrollmentStatus.ENROLLED)
                .toList();
        if (rows.isEmpty()) return List.of();

        List<UUID> studentIds = rows.stream().map(Enrollment::studentId).toList();
        Map<UUID, Student> byId = new HashMap<>();
        for (Student s : students.findAllById(studentIds)) byId.put(s.id(), s);

        Map<UUID, String> turmaGrade = new HashMap<>();
        for (Enrollment e : rows) {
            turmaGrade.computeIfAbsent(e.turmaId(),
                    tid -> structure.findTurma(tid).map(AcademicStructureQuery.TurmaView::gradeLevel).orElse(null));
        }

        List<EnrolledStudent> out = new ArrayList<>();
        for (Enrollment e : rows) {
            Student s = byId.get(e.studentId());
            if (s == null) continue;
            out.add(new EnrolledStudent(s.id(), s.tenantId(), s.fullName(),
                    e.turmaId(), turmaGrade.get(e.turmaId())));
        }
        return out;
    }

    private StudentSummary toSummary(Student s) {
        return new StudentSummary(s.id(), s.tenantId(), s.fullName(),
                s.dateOfBirth(), s.sex().name(), s.userId());
    }
}
