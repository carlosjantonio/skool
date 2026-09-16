package ao.skool.app.phase3;

import ao.skool.academic_structure.internal.web.dto.AcademicYearDtos.AcademicYearResponse;
import ao.skool.academic_structure.internal.web.dto.TurmaDtos.TurmaResponse;
import ao.skool.app.support.SchoolFixture;
import ao.skool.attendance.api.event.AttendanceMarkedAbsent;
import ao.skool.attendance.internal.domain.AttendanceStatus;
import ao.skool.attendance.internal.web.dto.AttendanceDtos.AttendanceEntry;
import ao.skool.attendance.internal.web.dto.AttendanceDtos.AttendanceResponse;
import ao.skool.attendance.internal.web.dto.AttendanceDtos.BatchRequest;
import ao.skool.attendance.internal.web.dto.AttendanceDtos.BatchResult;
import ao.skool.attendance.internal.web.dto.AttendanceDtos.FailedEntry;
import ao.skool.attendance.internal.web.dto.AttendanceDtos.StudentAttendanceSummary;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 3 — offline-tolerant attendance. The contract the PWA relies on is that the
 * batch endpoint is an <em>upsert keyed by the client-supplied id</em>: a teacher who
 * marks a student present, loses signal, corrects it to late, and syncs later must end
 * up with one row holding the correction — not two rows, and not a silently dropped edit.
 */
class AttendanceTest extends SchoolFixture {

    private static final LocalDate DAY = LocalDate.of(2025, 10, 6);

    private record Classroom(UUID turmaId, List<UUID> studentIds) {}

    private Classroom classroomOf(Actor admin, int studentCount) throws Exception {
        AcademicYearResponse year = createAcademicYear(admin);
        TurmaResponse turma = createTurma(admin, year.id());
        List<UUID> students = new java.util.ArrayList<>();
        for (int i = 0; i < studentCount; i++) {
            var student = createStudent(admin, "Aluno " + (i + 1));
            enrol(admin, student.id(), year.id(), turma.id());
            students.add(student.id());
        }
        return new Classroom(turma.id(), students);
    }

    private AttendanceEntry mark(UUID id, UUID turmaId, UUID studentId, AttendanceStatus status) {
        return new AttendanceEntry(id, turmaId, studentId, DAY, status, null);
    }

    @Test
    @DisplayName("a whole class syncs in one batch")
    void ingestsABatch() throws Exception {
        Actor teacher = teacher();
        Classroom room = classroomOf(admin(), 5);

        List<AttendanceEntry> entries = room.studentIds().stream()
                .map(s -> mark(UUID.randomUUID(), room.turmaId(), s, AttendanceStatus.PRESENT))
                .toList();

        BatchResult result = post(teacher, "/api/attendance/batch", new BatchRequest(entries), BatchResult.class);

        assertThat(result.accepted()).hasSize(5);
        assertThat(result.failed()).isEmpty();

        List<AttendanceResponse> stored = get(teacher,
                "/api/attendance?turmaId=" + room.turmaId() + "&date=" + DAY, new TypeReference<>() {});
        assertThat(stored).hasSize(5)
                .allSatisfy(r -> assertThat(r.status()).isEqualTo(AttendanceStatus.PRESENT));
    }

    @Test
    @DisplayName("re-sending the same id updates the row instead of duplicating it")
    void sameIdUpserts() throws Exception {
        Actor teacher = teacher();
        Classroom room = classroomOf(admin(), 1);
        UUID studentId = room.studentIds().getFirst();
        UUID stableId = UUID.randomUUID();   // what the PWA's deterministic v5 UUID gives us

        post(teacher, "/api/attendance/batch",
                new BatchRequest(List.of(mark(stableId, room.turmaId(), studentId, AttendanceStatus.PRESENT))),
                BatchResult.class);

        // Teacher corrects herself while offline; the queue flushes the same id later.
        BatchResult second = post(teacher, "/api/attendance/batch",
                new BatchRequest(List.of(mark(stableId, room.turmaId(), studentId, AttendanceStatus.LATE))),
                BatchResult.class);

        assertThat(second.accepted()).containsExactly(stableId);
        assertThat(second.failed()).isEmpty();

        List<AttendanceResponse> stored = get(teacher,
                "/api/attendance?turmaId=" + room.turmaId() + "&date=" + DAY, new TypeReference<>() {});
        assertThat(stored).singleElement()
                .satisfies(r -> assertThat(r.status()).isEqualTo(AttendanceStatus.LATE));
    }

    @Test
    @DisplayName("replaying an unchanged batch is safe — the retry is accepted, not failed")
    void replayingAnUnchangedBatchIsIdempotent() throws Exception {
        Actor teacher = teacher();
        Classroom room = classroomOf(admin(), 3);
        List<AttendanceEntry> entries = room.studentIds().stream()
                .map(s -> mark(UUID.randomUUID(), room.turmaId(), s, AttendanceStatus.PRESENT))
                .toList();

        post(teacher, "/api/attendance/batch", new BatchRequest(entries), BatchResult.class);
        BatchResult replay = post(teacher, "/api/attendance/batch", new BatchRequest(entries), BatchResult.class);

        assertThat(replay.accepted()).hasSize(3);
        assertThat(replay.failed()).isEmpty();
        assertThat(get(teacher, "/api/attendance?turmaId=" + room.turmaId() + "&date=" + DAY,
                new TypeReference<List<AttendanceResponse>>() {})).hasSize(3);
    }

    @Test
    @DisplayName("a second id for the same student/turma/date is reported as failed, not stored")
    void rejectsDuplicateStudentTurmaDate() throws Exception {
        Actor teacher = teacher();
        Classroom room = classroomOf(admin(), 1);
        UUID studentId = room.studentIds().getFirst();

        post(teacher, "/api/attendance/batch",
                new BatchRequest(List.of(mark(UUID.randomUUID(), room.turmaId(), studentId, AttendanceStatus.PRESENT))),
                BatchResult.class);

        // A mis-behaving client ships a *fresh* UUID for the same logical event.
        BatchResult second = post(teacher, "/api/attendance/batch",
                new BatchRequest(List.of(mark(UUID.randomUUID(), room.turmaId(), studentId, AttendanceStatus.ABSENT))),
                BatchResult.class);

        assertThat(second.accepted()).isEmpty();
        assertThat(second.failed()).singleElement()
                .extracting(FailedEntry::reason).isEqualTo("duplicate_student_turma_date");

        // The original mark is untouched.
        assertThat(get(teacher, "/api/attendance?turmaId=" + room.turmaId() + "&date=" + DAY,
                new TypeReference<List<AttendanceResponse>>() {}))
                .singleElement().extracting(AttendanceResponse::status).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    @DisplayName("marking absent publishes AttendanceMarkedAbsent — once, on the transition")
    void publishesAbsenceOnTransitionOnly() throws Exception {
        Actor teacher = teacher();
        Classroom room = classroomOf(admin(), 1);
        UUID studentId = room.studentIds().getFirst();
        UUID stableId = UUID.randomUUID();

        events.clear();
        post(teacher, "/api/attendance/batch",
                new BatchRequest(List.of(mark(stableId, room.turmaId(), studentId, AttendanceStatus.ABSENT))),
                BatchResult.class);

        AttendanceMarkedAbsent event = events.onlyOne(AttendanceMarkedAbsent.class);
        assertThat(event.studentId()).isEqualTo(studentId);
        assertThat(event.turmaId()).isEqualTo(room.turmaId());
        assertThat(event.date()).isEqualTo(DAY);
        assertThat(event.tenantId().value()).isEqualTo(tenant);

        // Syncing the same ABSENT again must not spam the guardian with a second SMS.
        post(teacher, "/api/attendance/batch",
                new BatchRequest(List.of(mark(stableId, room.turmaId(), studentId, AttendanceStatus.ABSENT))),
                BatchResult.class);
        assertThat(events.ofType(AttendanceMarkedAbsent.class)).hasSize(1);
    }

    @Test
    @DisplayName("correcting present → absent does publish; absent → present does not")
    void publishesOnlyWhenBecomingAbsent() throws Exception {
        Actor teacher = teacher();
        Classroom room = classroomOf(admin(), 1);
        UUID studentId = room.studentIds().getFirst();
        UUID stableId = UUID.randomUUID();

        events.clear();
        post(teacher, "/api/attendance/batch",
                new BatchRequest(List.of(mark(stableId, room.turmaId(), studentId, AttendanceStatus.PRESENT))),
                BatchResult.class);
        assertThat(events.ofType(AttendanceMarkedAbsent.class)).isEmpty();

        post(teacher, "/api/attendance/batch",
                new BatchRequest(List.of(mark(stableId, room.turmaId(), studentId, AttendanceStatus.ABSENT))),
                BatchResult.class);
        assertThat(events.ofType(AttendanceMarkedAbsent.class)).hasSize(1);

        // The student turns up after all — correcting back must not fire another absence.
        post(teacher, "/api/attendance/batch",
                new BatchRequest(List.of(mark(stableId, room.turmaId(), studentId, AttendanceStatus.PRESENT))),
                BatchResult.class);
        assertThat(events.ofType(AttendanceMarkedAbsent.class)).hasSize(1);
    }

    @Test
    @DisplayName("the per-student summary counts each status across a date range")
    void summarisesAStudentsAttendance() throws Exception {
        Actor teacher = teacher();
        Classroom room = classroomOf(admin(), 1);
        UUID studentId = room.studentIds().getFirst();

        record Day(LocalDate date, AttendanceStatus status) {}
        List<Day> week = List.of(
                new Day(LocalDate.of(2025, 10, 6), AttendanceStatus.PRESENT),
                new Day(LocalDate.of(2025, 10, 7), AttendanceStatus.PRESENT),
                new Day(LocalDate.of(2025, 10, 8), AttendanceStatus.ABSENT),
                new Day(LocalDate.of(2025, 10, 9), AttendanceStatus.LATE),
                new Day(LocalDate.of(2025, 10, 10), AttendanceStatus.EXCUSED));

        for (Day d : week) {
            post(teacher, "/api/attendance/batch",
                    new BatchRequest(List.of(new AttendanceEntry(UUID.randomUUID(), room.turmaId(),
                            studentId, d.date(), d.status(), null))), BatchResult.class);
        }

        StudentAttendanceSummary summary = get(teacher,
                "/api/attendance/students/" + studentId + "/summary?from=2025-10-06&to=2025-10-10",
                StudentAttendanceSummary.class);

        assertThat(summary.presentCount()).isEqualTo(2);
        assertThat(summary.absentCount()).isEqualTo(1);
        assertThat(summary.lateCount()).isEqualTo(1);
        assertThat(summary.excusedCount()).isEqualTo(1);

        // A narrower window must exclude the days outside it.
        StudentAttendanceSummary firstTwo = get(teacher,
                "/api/attendance/students/" + studentId + "/summary?from=2025-10-06&to=2025-10-07",
                StudentAttendanceSummary.class);
        assertThat(firstTwo.presentCount()).isEqualTo(2);
        assertThat(firstTwo.absentCount()).isZero();
    }

    @Test
    @DisplayName("an empty batch is rejected rather than silently accepted")
    void rejectsEmptyBatch() throws Exception {
        assertThat(postStatus(teacher(), "/api/attendance/batch", new BatchRequest(List.of())))
                .isEqualTo(400);
    }

    @Test
    @DisplayName("students and guardians cannot write attendance")
    void attendanceWritesAreStaffOnly() throws Exception {
        Classroom room = classroomOf(admin(), 1);
        var entry = List.of(mark(UUID.randomUUID(), room.turmaId(), room.studentIds().getFirst(),
                AttendanceStatus.PRESENT));

        assertThat(postStatus(student(UUID.randomUUID()), "/api/attendance/batch", new BatchRequest(entry)))
                .isEqualTo(403);
        assertThat(postStatus(guardian(UUID.randomUUID()), "/api/attendance/batch", new BatchRequest(entry)))
                .isEqualTo(403);
        // A guardian may still read the summary for their child.
        assertThat(getStatus(guardian(UUID.randomUUID()),
                "/api/attendance/students/" + room.studentIds().getFirst()
                        + "/summary?from=2025-10-06&to=2025-10-10")).isEqualTo(200);
    }
}
