import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import * as academic from '../api/academic';
import * as attendance from '../api/attendance';
import * as students from '../api/students';
import { enqueue, stableAttendanceId } from '../db/attendanceQueue';
import { useAttendanceSync } from '../hooks/useAttendanceSync';
import type { AcademicYear, AttendanceStatus, Student, Turma } from '../api/types';

const STATUSES: AttendanceStatus[] = ['PRESENT', 'ABSENT', 'LATE', 'EXCUSED'];

export function TeacherAttendancePage() {
  const { t } = useTranslation();
  const { turmaId } = useParams<{ turmaId: string }>();
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [year, setYear] = useState<AcademicYear | null>(null);
  const [turma, setTurma] = useState<Turma | null>(null);
  const [roster, setRoster] = useState<Student[]>([]);
  // client-visible status map keyed by student id. May reflect either a persisted
  // server record or a queued mutation not yet flushed.
  const [statuses, setStatuses] = useState<Record<string, AttendanceStatus>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { state, syncNow } = useAttendanceSync();

  useEffect(() => {
    async function load() {
      if (!turmaId) return;
      setLoading(true);
      setError(null);
      try {
        const years = await academic.listAcademicYears();
        const current = years.find((y) => y.current) ?? years[0];
        if (!current) throw new Error('no academic year');
        setYear(current);

        const [ts, enrolls, allStudents] = await Promise.all([
          academic.listTurmas(current.id),
          students.listEnrollments(current.id, turmaId),
          students.listStudents(),
        ]);
        const thisTurma = ts.find((x) => x.id === turmaId) ?? null;
        setTurma(thisTurma);

        const enrolledIds = new Set(enrolls.filter((e) => e.status === 'ENROLLED').map((e) => e.studentId));
        const inTurma = allStudents.filter((s) => enrolledIds.has(s.id))
          .sort((a, b) => a.fullName.localeCompare(b.fullName, 'pt-AO'));
        setRoster(inTurma);

        // Load server-side attendance for the selected date; queued mutations override.
        const existing = await attendance.listAttendance(turmaId, date);
        const map: Record<string, AttendanceStatus> = {};
        for (const r of existing) map[r.studentId] = r.status;
        setStatuses(map);
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    }
    void load();
  }, [turmaId, date]);

  const setStatus = async (studentId: string, status: AttendanceStatus) => {
    if (!turmaId) return;
    setStatuses((prev) => ({ ...prev, [studentId]: status }));
    await enqueue({ turmaId, studentId, date, status });
    // Try to flush immediately; if offline this is a no-op that stays in the queue.
    void syncNow();
  };

  const summary = useMemo(() => {
    const total = roster.length;
    let present = 0, absent = 0, late = 0, excused = 0, unset = 0;
    for (const s of roster) {
      const st = statuses[s.id];
      if (st === 'PRESENT') present++;
      else if (st === 'ABSENT') absent++;
      else if (st === 'LATE') late++;
      else if (st === 'EXCUSED') excused++;
      else unset++;
    }
    return { total, present, absent, late, excused, unset };
  }, [roster, statuses]);

  return (
    <div>
      <Link to="/classes" style={{ fontSize: '0.9rem' }}>&larr; {t('teacher.back_to_classes')}</Link>
      <h1 style={{ marginTop: '0.5rem', marginBottom: '0.5rem' }}>
        {t('teacher.attendance.title', { turma: turma?.name ?? '' })}
      </h1>
      <div style={{ color: 'var(--color-text-muted)', marginBottom: '1rem' }}>
        {year ? t('teacher.attendance.year', { year: year.name }) : null}
      </div>

      <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap' }}>
        <label htmlFor="date" style={{ margin: 0 }}>{t('teacher.attendance.date')}</label>
        <input id="date" type="date" value={date} onChange={(e) => setDate(e.target.value)} style={{ width: 'auto' }} />
        <div style={{ marginLeft: 'auto', fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>
          {t('teacher.attendance.summary', {
            present: summary.present,
            absent: summary.absent,
            late: summary.late,
            excused: summary.excused,
            unset: summary.unset,
          })}
        </div>
      </div>

      {error && <div className="error">{error}</div>}
      {loading && <div>…</div>}

      {!loading && roster.length === 0 && (
        <div className="card" style={{ color: 'var(--color-text-muted)' }}>{t('teacher.attendance.empty')}</div>
      )}

      {roster.map((student) => (
        <div
          key={student.id}
          className="card"
          data-testid="attendance-row"
          data-student-id={student.id}
          data-queued-id={turmaId ? stableAttendanceId(student.id, turmaId, date) : ''}
          style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '0.5rem', padding: '0.75rem 1rem' }}
        >
          <div style={{ flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis' }}>
            <div style={{ fontWeight: 500 }}>{student.fullName}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)' }}>{student.dateOfBirth}</div>
          </div>
          <div style={{ display: 'flex', gap: '0.35rem', flexWrap: 'wrap' }}>
            {STATUSES.map((st) => {
              const active = statuses[student.id] === st;
              return (
                <button
                  key={st}
                  onClick={() => void setStatus(student.id, st)}
                  className={active ? undefined : 'secondary'}
                  style={{ padding: '0.35rem 0.65rem', fontSize: '0.8rem' }}
                  data-status={st}
                  data-active={active}
                >
                  {t(`attendance.${st}`)}
                </button>
              );
            })}
          </div>
        </div>
      ))}

      {state.queued > 0 && (
        <div className="card" style={{ marginTop: '1rem', background: 'rgba(240, 180, 41, 0.15)' }}>
          {t('teacher.attendance.queue', { count: state.queued })}
        </div>
      )}
    </div>
  );
}
