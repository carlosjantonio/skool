package ao.skool.sis.internal.web;

import ao.skool.common.domain.SkoolPrincipal;
import ao.skool.common.security.Roles;
import ao.skool.common.web.error.ApplicationException;
import ao.skool.sis.api.StudentDirectory;
import ao.skool.sis.internal.domain.Enrollment;
import ao.skool.sis.internal.domain.EnrollmentStatus;
import ao.skool.sis.internal.persistence.EnrollmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * Endpoints scoped to the currently authenticated student. The student profile is
 * resolved via {@link StudentDirectory#findStudentByUserId(UUID)} — no {@code studentId}
 * ever appears in the URL, so a student can never poke at another student's record.
 */
@RestController
@RequestMapping("/api/student/me")
public class StudentPortalController {

    private final StudentDirectory studentDirectory;
    private final EnrollmentRepository enrollments;

    public StudentPortalController(StudentDirectory studentDirectory, EnrollmentRepository enrollments) {
        this.studentDirectory = studentDirectory;
        this.enrollments = enrollments;
    }

    @GetMapping
    @PreAuthorize("hasRole('" + Roles.STUDENT + "')")
    public StudentSelf me(@AuthenticationPrincipal SkoolPrincipal principal) {
        var student = studentDirectory.findStudentByUserId(UUID.fromString(principal.userId()))
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "error.not_found"));
        Enrollment current = enrollments.findByStudentIdOrderByEnrolledAtDesc(student.id()).stream()
                .filter(e -> e.status() == EnrollmentStatus.ENROLLED)
                .findFirst().orElse(null);
        return new StudentSelf(
                student.id(),
                student.fullName(),
                student.dateOfBirth() == null ? null : student.dateOfBirth().toString(),
                student.sex(),
                current == null ? null : current.turmaId(),
                current == null ? null : current.academicYearId(),
                current == null ? null : current.enrolledAt());
    }

    public record StudentSelf(
            UUID studentId,
            String fullName,
            String dateOfBirth,
            String sex,
            UUID turmaId,
            UUID academicYearId,
            Instant enrolledAt
    ) {}
}
