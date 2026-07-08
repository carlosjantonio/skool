import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import * as quizzes from '../api/teacherQuizzes';
import type { Quiz, QuizAnalytics, QuizAttemptRow } from '../api/types';

export function TeacherQuizResultsPage() {
  const { t } = useTranslation();
  const { quizId } = useParams<{ quizId: string }>();
  const [quiz, setQuiz] = useState<Quiz | null>(null);
  const [analytics, setAnalytics] = useState<QuizAnalytics | null>(null);
  const [attempts, setAttempts] = useState<QuizAttemptRow[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    async function load() {
      if (!quizId) return;
      try {
        const [q, a, atts] = await Promise.all([
          quizzes.getQuiz(quizId),
          quizzes.quizAnalytics(quizId),
          quizzes.quizAttempts(quizId),
        ]);
        setQuiz(q);
        setAnalytics(a);
        setAttempts(atts);
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    }
    void load();
  }, [quizId]);

  async function publish() {
    if (!quizId) return;
    setBusy(true);
    try { setQuiz(await quizzes.publishQuiz(quizId)); }
    catch (err) { setError((err as Error).message); }
    finally { setBusy(false); }
  }

  async function close() {
    if (!quizId) return;
    setBusy(true);
    try { setQuiz(await quizzes.closeQuiz(quizId)); }
    catch (err) { setError((err as Error).message); }
    finally { setBusy(false); }
  }

  if (loading) return <div>…</div>;
  if (error) return <div className="error">{error}</div>;
  if (!quiz || !analytics) return null;

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: '1rem', alignItems: 'baseline', flexWrap: 'wrap' }}>
        <div>
          <h1 style={{ margin: 0 }}>{quiz.title}</h1>
          <div style={{ fontSize: '0.9rem', color: 'var(--color-text-muted)' }}>
            {t(`quiz.status.${quiz.status}`)} · {t('teacher.quizzes.n_questions', { count: quiz.questionCount })}
          </div>
        </div>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          {quiz.status === 'DRAFT' && (
            <button onClick={() => void publish()} disabled={busy}>{t('teacher.quiz_builder.publish')}</button>
          )}
          {quiz.status === 'PUBLISHED' && (
            <button className="secondary" onClick={() => void close()} disabled={busy}>{t('teacher.results.close')}</button>
          )}
        </div>
      </div>

      <section style={{ marginTop: '1rem' }}>
        <div style={{ display: 'grid', gap: '0.5rem', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))' }}>
          <Stat label={t('teacher.results.total_attempts')} value={String(analytics.totalAttempts)} />
          <Stat label={t('teacher.results.submitted')} value={String(analytics.submittedAttempts)} />
          <Stat label={t('teacher.results.avg_score')} value={`${analytics.averageAutoScore} / ${analytics.averageTotalPoints}`} />
        </div>
      </section>

      <section style={{ marginTop: '1.5rem' }}>
        <h2 style={{ fontSize: '1.05rem' }}>{t('teacher.results.per_question')}</h2>
        {analytics.questionStats.length === 0 ? (
          <div className="card" style={{ color: 'var(--color-text-muted)' }}>{t('teacher.results.no_answers')}</div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr>
                  <th style={{ textAlign: 'left', padding: '0.5rem', borderBottom: '1px solid var(--color-border)' }}>{t('teacher.results.question')}</th>
                  <th style={{ textAlign: 'right', padding: '0.5rem', borderBottom: '1px solid var(--color-border)' }}>{t('teacher.results.answered')}</th>
                  <th style={{ textAlign: 'right', padding: '0.5rem', borderBottom: '1px solid var(--color-border)' }}>{t('teacher.results.correct')}</th>
                  <th style={{ textAlign: 'right', padding: '0.5rem', borderBottom: '1px solid var(--color-border)' }}>{t('teacher.results.rate')}</th>
                </tr>
              </thead>
              <tbody>
                {analytics.questionStats.map((s) => (
                  <tr key={s.questionId}>
                    <td style={{ padding: '0.5rem', borderBottom: '1px solid var(--color-border)' }}>{s.prompt}</td>
                    <td style={{ padding: '0.5rem', borderBottom: '1px solid var(--color-border)', textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>{s.answeredCount}</td>
                    <td style={{ padding: '0.5rem', borderBottom: '1px solid var(--color-border)', textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>{s.correctCount}</td>
                    <td style={{ padding: '0.5rem', borderBottom: '1px solid var(--color-border)', textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>{formatRate(s.correctRate)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section style={{ marginTop: '1.5rem' }}>
        <h2 style={{ fontSize: '1.05rem' }}>{t('teacher.results.attempts')}</h2>
        {attempts.length === 0 ? (
          <div className="card" style={{ color: 'var(--color-text-muted)' }}>{t('teacher.results.no_attempts')}</div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr>
                  <th style={{ textAlign: 'left', padding: '0.5rem', borderBottom: '1px solid var(--color-border)' }}>{t('teacher.results.student')}</th>
                  <th style={{ textAlign: 'left', padding: '0.5rem', borderBottom: '1px solid var(--color-border)' }}>{t('teacher.results.attempt_status')}</th>
                  <th style={{ textAlign: 'right', padding: '0.5rem', borderBottom: '1px solid var(--color-border)' }}>{t('teacher.results.score')}</th>
                  <th style={{ textAlign: 'right', padding: '0.5rem', borderBottom: '1px solid var(--color-border)' }}>{t('teacher.results.tab_switches')}</th>
                </tr>
              </thead>
              <tbody>
                {attempts.map((a) => (
                  <tr key={a.attemptId}>
                    <td style={{ padding: '0.5rem', borderBottom: '1px solid var(--color-border)', fontVariantNumeric: 'tabular-nums', fontSize: '0.85rem' }}>{a.studentId.slice(0, 8)}</td>
                    <td style={{ padding: '0.5rem', borderBottom: '1px solid var(--color-border)' }}>{t(`attempt.status.${a.status}`)}</td>
                    <td style={{ padding: '0.5rem', borderBottom: '1px solid var(--color-border)', textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>
                      {a.autoScore ?? '—'} / {a.totalPoints ?? '—'}
                    </td>
                    <td style={{ padding: '0.5rem', borderBottom: '1px solid var(--color-border)', textAlign: 'right', color: a.tabSwitchCount > 2 ? 'var(--color-danger)' : 'var(--color-text-muted)' }}>
                      {a.tabSwitchCount}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="card">
      <div style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>{label}</div>
      <div style={{ fontSize: '1.5rem', fontWeight: 700, marginTop: '0.25rem' }}>{value}</div>
    </div>
  );
}

function formatRate(raw: string): string {
  const n = Number(raw);
  if (Number.isNaN(n)) return raw;
  return `${Math.round(n * 100)}%`;
}
