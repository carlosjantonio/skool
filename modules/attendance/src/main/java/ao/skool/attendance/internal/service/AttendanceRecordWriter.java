package ao.skool.attendance.internal.service;

import ao.skool.attendance.internal.domain.AttendanceRecord;
import ao.skool.attendance.internal.persistence.AttendanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Companion service whose sole job is to insert one new attendance record in its own
 * transaction, flushing immediately.
 * <p>
 * Two things force this. First, a plain {@code save()} does not flush, so a violation of
 * {@code uk_attendance_student_turma_date} would only surface at commit — long after the
 * {@code catch} in {@link AttendanceService#ingest} has been passed, turning one bad row
 * into a 500 for the whole batch. Second, once a constraint violation has been raised the
 * persistence context is poisoned and the transaction is rollback-only, so even the rows
 * that were fine would be lost.
 * <p>
 * Running each insert in {@code REQUIRES_NEW} isolates the failure to its own row: the
 * caller's transaction stays clean and can report that single entry as {@code failed}
 * while the rest of the batch commits. This mirrors
 * {@code assessment.internal.service.AttemptFactory}, which solves the same problem for
 * concurrent quiz-attempt creation.
 */
@Service
public class AttendanceRecordWriter {

    private final AttendanceRepository attendance;

    public AttendanceRecordWriter(AttendanceRepository attendance) {
        this.attendance = attendance;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AttendanceRecord insertFresh(AttendanceRecord record) {
        return attendance.saveAndFlush(record);
    }
}
