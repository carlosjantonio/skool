import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import * as student from '../api/student';
import * as academic from '../api/academic';
import * as quizzes from '../api/quizzes';
import * as forum from '../api/forum';
import * as board from '../api/board';
import type { BoardEntry, Forum, Quiz, StudentSelf, Subject } from '../api/types';

/**
 * Landing page for a STUDENT. Pulls the current enrolment (turma + year) and shows
 * three panels: available quizzes, the class board feed, and subject forums.
 */
export function StudentDashboardPage() {
  const { t } = useTranslation();
  const [me, setMe] = useState<StudentSelf | null>(null);
  const [availableQuizzes, setAvailableQuizzes] = useState<Quiz[]>([]);
  const [entries, setEntries] = useState<BoardEntry[]>([]);
  const [forums, setForums] = useState<Forum[]>([]);
  const [subjects, setSubjects] = useState<Record<string, Subject>>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const selfInfo = await student.getMe();
        setMe(selfInfo);
        if (!selfInfo.turmaId) {
          setLoading(false);
          return;
        }
        const [qs, feed, fs, subs] = await Promise.all([
          quizzes.listAvailableQuizzes(selfInfo.turmaId),
          board.feed(selfInfo.turmaId),
          forum.listForums(selfInfo.turmaId),
          academic.listSubjects(),
        ]);
        setAvailableQuizzes(qs);
        setEntries(feed);
        setForums(fs);
        setSubjects(Object.fromEntries(subs.map((s) => [s.id, s])));
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    }
    void load();
  }, []);

  if (loading) return <div>…</div>;
  if (error) return <div className="error">{error}</div>;
  if (!me) return null;
  if (!me.turmaId) {
    return (
      <div className="card" style={{ marginTop: '1rem', color: 'var(--color-text-muted)' }}>
        {t('student.no_enrolment')}
      </div>
    );
  }

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>{t('student.dashboard.title', { name: me.fullName })}</h1>

      <section style={{ marginTop: '1.5rem' }}>
        <h2 style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>{t('student.quizzes.pending')}</h2>
        {availableQuizzes.length === 0 ? (
          <div className="card" style={{ color: 'var(--color-text-muted)' }}>{t('student.quizzes.empty')}</div>
        ) : (
          <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))' }}>
            {availableQuizzes.map((q) => (
              <div key={q.id} className="card">
                <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>
                  {subjects[q.subjectId]?.name ?? '—'} · {t(`domain.trimestre.${q.trimesterKey.slice(1)}`, q.trimesterKey)}
                </div>
                <h3 style={{ margin: '0.35rem 0 0.5rem 0', fontSize: '1rem' }}>{q.title}</h3>
                <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', marginBottom: '0.75rem' }}>
                  {t('student.quizzes.question_count', { count: q.questionCount })}
                  {q.timeLimitSeconds ? ` · ${t('student.quizzes.time_limit', { minutes: Math.round(q.timeLimitSeconds / 60) })}` : ''}
                </div>
                <Link to={`/student/quizzes/${q.id}`}>
                  <button style={{ width: '100%' }}>{t('student.quizzes.take')}</button>
                </Link>
              </div>
            ))}
          </div>
        )}
      </section>

      <section style={{ marginTop: '1.5rem' }}>
        <h2 style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>{t('student.board.title')}</h2>
        {entries.length === 0 ? (
          <div className="card" style={{ color: 'var(--color-text-muted)' }}>{t('student.board.empty')}</div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {entries.slice(0, 8).map((e) => (
              <div key={e.id} className="card" style={{ padding: '0.75rem 1rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: '0.75rem' }}>
                  <div>
                    <div style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>
                      {subjects[e.subjectId]?.name ?? '—'} · {t(`board.kind.${e.kind}`)}
                    </div>
                    <div style={{ fontWeight: 600 }}>{e.title}</div>
                    {e.body && <div style={{ fontSize: '0.9rem', color: 'var(--color-text-muted)', marginTop: '0.25rem' }}>{e.body}</div>}
                    {e.dueAt && (
                      <div style={{ fontSize: '0.8rem', color: 'var(--color-accent)', marginTop: '0.25rem' }}>
                        {t('student.board.due', { date: new Date(e.dueAt).toLocaleDateString('pt-AO') })}
                      </div>
                    )}
                  </div>
                  {e.pinned && <span style={{ fontSize: '0.75rem', color: 'var(--color-accent)' }}>{t('student.board.pinned')}</span>}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      <section style={{ marginTop: '1.5rem' }}>
        <h2 style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>{t('student.forum.title')}</h2>
        {forums.length === 0 ? (
          <div className="card" style={{ color: 'var(--color-text-muted)' }}>{t('student.forum.empty')}</div>
        ) : (
          <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))' }}>
            {forums.map((f) => (
              <Link key={f.id} to={`/student/forums/${f.id}`}>
                <div className="card">
                  <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>{subjects[f.subjectId]?.name ?? '—'}</div>
                  <div style={{ fontWeight: 600, marginTop: '0.25rem' }}>{f.title}</div>
                  {f.description && (
                    <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', marginTop: '0.25rem' }}>{f.description}</div>
                  )}
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>

      <section style={{ marginTop: '1.5rem', display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
        <Link to="/student/grades">
          <button className="secondary">{t('student.grades.open')}</button>
        </Link>
        <Link to="/student/assignments">
          <button className="secondary">{t('student.assignments.open')}</button>
        </Link>
      </section>
    </div>
  );
}
