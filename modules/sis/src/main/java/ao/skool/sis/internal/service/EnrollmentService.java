package ao.skool.sis.internal.service;

import ao.skool.common.domain.tenant.TenantContext;
import ao.skool.common.web.error.ApplicationException;
import ao.skool.common.web.error.NotFoundException;
import ao.skool.sis.api.event.StudentEnrolled;
import ao.skool.sis.internal.domain.Enrollment;
import ao.skool.sis.internal.persistence.EnrollmentRepository;
import ao.skool.sis.internal.web.dto.EnrollmentDtos.CreateEnrollment;
import ao.skool.sis.internal.web.dto.EnrollmentDtos.EnrollmentResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollments;
    private final TenantContext tenant;
    private final ApplicationEventPublisher events;

    public EnrollmentService(EnrollmentRepository enrollments, TenantContext tenant,
                             ApplicationEventPublisher events) {
        this.enrollments = enrollments;
        this.tenant = tenant;
        this.events = events;
    }

    public EnrollmentResponse enrol(CreateEnrollment cmd) {
        var tenantId = tenant.current();
        if (enrollments.existsByStudentIdAndAcademicYearId(cmd.studentId(), cmd.academicYearId())) {
            throw new ApplicationException(HttpStatus.CONFLICT, "error.conflict");
        }
        Enrollment enrollment = new Enrollment(UUID.randomUUID(), tenantId.value(),
                cmd.studentId(), cmd.academicYearId(), cmd.turmaId());
        enrollment.confirm();
        enrollments.save(enrollment);
        events.publishEvent(new StudentEnrolled(
                UUID.randomUUID(), tenantId, enrollment.studentId(),
                enrollment.academicYearId(), enrollment.turmaId(), Instant.now()));
        return toResponse(enrollment);
    }

    public EnrollmentResponse withdraw(UUID enrollmentId) {
        Enrollment e = enrollments.findById(enrollmentId).orElseThrow(NotFoundException::new);
        e.withdraw();
        return toResponse(e);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> list(UUID academicYearId) {
        return enrollments.findByTenantIdAndAcademicYearId(tenant.current().value(), academicYearId)
                .stream().map(this::toResponse).toList();
    }

    private EnrollmentResponse toResponse(Enrollment e) {
        return new EnrollmentResponse(e.id(), e.studentId(), e.academicYearId(), e.turmaId(),
                e.status(), e.enrolledAt(), e.withdrawnAt());
    }
}
