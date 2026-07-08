import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import * as academic from '../api/academic';
import * as staff from '../api/staff';
import * as quizzes from '../api/teacherQuizzes';
import type { AcademicYear, Quiz, StaffAssignment, Subject, Turma } from '../api/types';

/**
 * Teacher landing for a turma's quizzes. Lists everything (draft + published + closed)
 * so a mid-authoring quiz doesn't get lost, and offers a "new quiz" link.
 */
export function TeacherQuizzesPage() {
  const { t } = useTranslation();
  const { turmaId } = useParams<{ turmaId: string }>();
  const [year, setYear] = useState<AcademicYear | null>(null);
  const [turma, setTurma] = useState<Turma | null>(null);
  const [list, setList] = useState<Quiz[]>([]);
  const [subjects, setSubjects] = useState<Record<string, Subject>>({});
  const [mySubjects, setMySubjects] = useState<StaffAssignment[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      if (!turmaId) return;
      try {
        const years = await academic.listAcademicYears();
        const current = years.find((y) => y.current) ?? years[0];
        setYear(current);
        if (!current) { setLoading(false); return; }
        const [ts, subs, qs, myAsgn] = await Promise.all([
          academic.listTurmas(current.id),
          academic.listSubjects(),
          quizzes.listQuizzes(turmaId),
          staff.getMyAssignments(current.id),
        ]);
        setTurma(ts.find((x) => x.id === turmaId) ?? null);
        setSubjects(Object.fromEntries(subs.map((s) => [s.id, s])));
        setList(qs);
        setMySubjects(myAsgn.filter((a) => a.turmaId === turmaId && a.subjectId));
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    }
    void load();
  }, [turmaId]);

  const relevantSubjects = useMemo(
    () => Array.from(new Set(mySubjects.map((a) => a.subjectId).filter(Boolean))) as string[],
    [mySubjects]
  );

  if (loading) return <div>…</div>;
  if (error) return <div className="error">{error}</div>;
  if (!turma || !year) return <div>{t('teacher.no_year')}</div>;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', gap: '1rem', flexWrap: 'wrap' }}>
        <h1 style={{ margin: 0 }}>{t('teacher.quizzes.title', { turma: turma.name })}</h1>
        <Link to={`/classes/${turmaId}/quizzes/new`}>
          <button>{t('teacher.quizzes.new')}</button>
        </Link>
      </div>

      {relevantSubjects.length === 0 && (
        <div className="card" style={{ marginTop: '1rem', color: 'var(--color-text-muted)' }}>
          {t('teacher.quizzes.no_subject_assignment')}
        </div>
      )}

      {list.length === 0 ? (
        <div className="card" style={{ marginTop: '1rem', color: 'var(--color-text-muted)' }}>
          {t('teacher.quizzes.empty')}
        </div>
      ) : (
        <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', marginTop: '1rem' }}>
          {list.map((q) => (
            <Link key={q.id} to={`/classes/${turmaId}/quizzes/${q.id}/results`}>
              <div className="card">
                <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>
                  {subjects[q.subjectId]?.name ?? '—'} · {t(`domain.trimestre.${q.trimesterKey.slice(1)}`)}
                </div>
                <h3 style={{ margin: '0.35rem 0 0.5rem 0', fontSize: '1.05rem' }}>{q.title}</h3>
                <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                  <span className={`chip chip-${q.status.toLowerCase()}`} style={chipStyle(q.status)}>
                    {t(`quiz.status.${q.status}`)}
                  </span>
                  <span style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>
                    {t('teacher.quizzes.n_questions', { count: q.questionCount })}
                  </span>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}

function chipStyle(status: string): React.CSSProperties {
  const map: Record<string, string> = {
    DRAFT: 'var(--color-text-muted)',
    PUBLISHED: 'var(--color-accent)',
    CLOSED: 'var(--color-text-muted)',
  };
  return {
    fontSize: '0.75rem',
    padding: '0.15rem 0.5rem',
    borderRadius: '999px',
    border: `1px solid ${map[status] ?? 'var(--color-border)'}`,
    color: map[status] ?? 'var(--color-text)',
  };
}
