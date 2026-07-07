package ao.skool.attendance.internal.persistence;

import ao.skool.attendance.internal.domain.AttendanceRecord;
import ao.skool.attendance.internal.domain.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, UUID> {

    List<AttendanceRecord> findByTurmaIdAndDateRecordedOrderByStudentIdAsc(UUID turmaId, LocalDate date);

    List<AttendanceRecord> findByStudentIdAndDateRecordedBetween(UUID studentId, LocalDate from, LocalDate to);

    long countByStudentIdAndStatusAndDateRecordedBetween(UUID studentId, AttendanceStatus status,
                                                          LocalDate from, LocalDate to);
}
