import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import * as student from '../api/student';
import * as academic from '../api/academic';
import * as assignments from '../api/assignments';
import type { Assignment, AssignmentSubmission, StudentSelf, Subject } from '../api/types';

/**
 * List of assignments for the student's turma, with an inline notes-based submission
 * form. Document upload is deferred — a link to /api/documents/upload could be added
 * once the file-picker UX is designed. Note-only submission works today.
 */
export function StudentAssignmentsPage() {
  const { t } = useTranslation();
  const [me, setMe] = useState<StudentSelf | null>(null);
  const [list, setList] = useState<Assignment[]>([]);
  const [mine, setMine] = useState<Record<string, AssignmentSubmission>>({});
  const [subjects, setSubjects] = useState<Record<string, Subject>>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const selfInfo = await student.getMe();
        setMe(selfInfo);
        if (!selfInfo.turmaId) { setLoading(false); return; }
        const [asgn, subs, mySubs] = await Promise.all([
          assignments.listStudentAssignments(selfInfo.turmaId),
          academic.listSubjects(),
          assignments.mySubmissions(),
        ]);
        setList(asgn);
        setSubjects(Object.fromEntries(subs.map((s) => [s.id, s])));
        setMine(Object.fromEntries(mySubs.map((s) => [s.assignmentId, s])));
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    }
    void load();
  }, []);

  async function onSubmit(assignmentId: string, notes: string) {
    try {
      const sub = await assignments.submitAssignment(assignmentId, notes);
      setMine((prev) => ({ ...prev, [assignmentId]: sub }));
    } catch (err) {
      setError((err as Error).message);
    }
  }

  if (loading) return <div>…</div>;
  if (error) return <div className="error">{error}</div>;
  if (!me) return null;
  if (!me.turmaId) return <div>{t('student.no_enrolment')}</div>;

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>{t('student.assignments.title')}</h1>
      {list.length === 0 ? (
        <div className="card" style={{ color: 'var(--color-text-muted)' }}>{t('student.assignments.empty')}</div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {list.map((a) => (
            <AssignmentCard
              key={a.id}
              assignment={a}
              subjectName={subjects[a.subjectId]?.name ?? '—'}
              mine={mine[a.id]}
              onSubmit={(notes) => void onSubmit(a.id, notes)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function AssignmentCard({ assignment, subjectName, mine, onSubmit }: {
  assignment: Assignment;
  subjectName: string;
  mine: AssignmentSubmission | undefined;
  onSubmit: (notes: string) => void;
}) {
  const { t } = useTranslation();
  const [notes, setNotes] = useState(mine?.notes ?? '');
  const [open, setOpen] = useState(!mine);
  const overdue = !mine && new Date(assignment.dueAt).getTime() < Date.now();

  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: '1rem', flexWrap: 'wrap' }}>
        <div>
          <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>{subjectName}</div>
          <div style={{ fontWeight: 600 }}>{assignment.title}</div>
          {assignment.description && (
            <div style={{ fontSize: '0.9rem', color: 'var(--color-text-muted)', marginTop: '0.25rem' }}>{assignment.description}</div>
          )}
          <div style={{ fontSize: '0.85rem', color: overdue ? 'var(--color-danger)' : 'var(--color-accent)', marginTop: '0.25rem' }}>
            {t('student.assignments.due', { date: new Date(assignment.dueAt).toLocaleString('pt-AO') })}
          </div>
        </div>
        <div style={{ textAlign: 'right', minWidth: '120px' }}>
          {mine ? (
            <div>
              <div style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>
                {t(`assignment.status.${mine.status}`)}
              </div>
              <div style={{ fontSize: '1.2rem', fontWeight: 700 }}>
                {mine.score != null ? `${mine.score} / ${assignment.maxScore}` : '—'}
              </div>
              {mine.isLate && <div style={{ fontSize: '0.75rem', color: 'var(--color-danger)' }}>{t('student.assignments.late')}</div>}
            </div>
          ) : (
            <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>{t('student.assignments.not_submitted')}</div>
          )}
        </div>
      </div>

      {mine?.feedback && (
        <div className="card" style={{ marginTop: '0.75rem', background: 'var(--color-bg-alt, #f5f5f5)' }}>
          <div style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)', marginBottom: '0.25rem' }}>
            {t('student.assignments.feedback')}
          </div>
          <div style={{ whiteSpace: 'pre-wrap' }}>{mine.feedback}</div>
        </div>
      )}

      {!open ? (
        <div style={{ marginTop: '0.75rem' }}>
          <button className="secondary" onClick={() => setOpen(true)}>
            {t('student.assignments.resubmit')}
          </button>
        </div>
      ) : (
        <div style={{ marginTop: '0.75rem' }}>
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={4}
            placeholder={t('student.assignments.notes_placeholder')}
            style={{ width: '100%', boxSizing: 'border-box' }}
          />
          <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
            <button onClick={() => { onSubmit(notes); setOpen(false); }} disabled={!notes.trim()}>
              {t('student.assignments.submit')}
            </button>
            {mine && <button className="secondary" onClick={() => setOpen(false)}>{t('student.assignments.cancel')}</button>}
          </div>
        </div>
      )}
    </div>
  );
}
