package ao.skool.sis.api;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only lookup other modules use to render student info without reaching into
 * the SIS internal tables. Never expose mutating operations here — mutations stay
 * behind SIS's own controllers.
 */
public interface StudentDirectory {

    Optional<StudentSummary> findStudent(UUID studentId);

    /** Finds the student profile linked to the given portal user (STUDENT role). */
    Optional<StudentSummary> findStudentByUserId(UUID userId);

    /**
     * All students actively enrolled in the given academic year. Used by fees to
     * fan out invoices when a fee schedule fires. Includes turma + gradeLevel so
     * the caller can filter without joining across modules.
     */
    List<EnrolledStudent> listEnrolledForYear(UUID academicYearId);

    record StudentSummary(UUID id, UUID tenantId, String fullName, LocalDate dateOfBirth, String sex, UUID userId) {}

    record EnrolledStudent(UUID studentId, UUID tenantId, String fullName, UUID turmaId, String gradeLevel) {}
}
