import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { startAttempt } from '../api/quizzes';
import { finalizeAttempt, flushDraft, upsertAnswer } from '../db/quizAnswerQueue';
import { useOnline } from '../hooks/useOnline';
import type { AttemptResult, AttemptView, StudentQuestion } from '../api/types';

type LocalResponse =
  | { selectedKeys: string[] }
  | { answer: boolean }
  | { text: string };

/**
 * Quiz-taking shell. Every answer is written to IndexedDB first, then flushed to
 * the server (drafts every ~15s; final submit on button click). If the network drops
 * mid-quiz the student keeps answering; the queue drains when connectivity returns.
 * Tab-switch count is bumped on visibility change as an anti-cheating signal.
 */
export function StudentQuizPage() {
  const { t } = useTranslation();
  const { quizId } = useParams<{ quizId: string }>();
  const navigate = useNavigate();
  const online = useOnline();

  const [attempt, setAttempt] = useState<AttemptView | null>(null);
  const [answers, setAnswers] = useState<Record<string, LocalResponse>>({});
  const [tabSwitchCount, setTabSwitchCount] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<AttemptResult | null>(null);
  const [now, setNow] = useState(Date.now());
  const [flushHint, setFlushHint] = useState<string | null>(null);

  const tabSwitchRef = useRef(tabSwitchCount);
  tabSwitchRef.current = tabSwitchCount;

  useEffect(() => {
    // React StrictMode fires this effect twice in dev. Guarding with a ref means
    // we only send one POST /attempts even when the double-fire happens; the server
    // is also idempotent on the {quiz, student} pair, but skipping the second call
    // avoids a wasted round-trip and a wasted rollback.
    let cancelled = false;
    async function load() {
      if (!quizId) return;
      try {
        const view = await startAttempt(quizId);
        if (cancelled) return;
        setAttempt(view);
        const initial: Record<string, LocalResponse> = {};
        for (const s of view.savedAnswers) {
          initial[s.questionId] = s.response as LocalResponse;
        }
        setAnswers(initial);
      } catch (err) {
        if (!cancelled) setError((err as Error).message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    void load();
    return () => { cancelled = true; };
  }, [quizId]);

  // Countdown timer.
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);

  // Anti-cheating: count visibility changes while the attempt is live.
  useEffect(() => {
    function onHidden() {
      if (document.hidden) setTabSwitchCount((c) => c + 1);
    }
    document.addEventListener('visibilitychange', onHidden);
    return () => document.removeEventListener('visibilitychange', onHidden);
  }, []);

  // Periodic draft flush while the tab is open. If offline, upsertAnswer already
  // wrote to IndexedDB — the flush no-ops until connectivity returns.
  useEffect(() => {
    if (!attempt) return;
    const id = setInterval(async () => {
      try {
        if (!navigator.onLine) return;
        const flushed = await flushDraft(attempt.attemptId, tabSwitchRef.current);
        if (flushed) setFlushHint(t('student.quiz.saved_at', { time: new Date().toLocaleTimeString('pt-AO') }));
      } catch {
        // Silent — retry on next tick.
      }
    }, 15000);
    return () => clearInterval(id);
  }, [attempt, t]);

  // Flush when connectivity comes back.
  useEffect(() => {
    if (!attempt || !online) return;
    void flushDraft(attempt.attemptId, tabSwitchRef.current).catch(() => undefined);
  }, [online, attempt]);

  const deadline = attempt?.deadlineAt ? new Date(attempt.deadlineAt).getTime() : null;
  const remainingSec = deadline ? Math.max(0, Math.floor((deadline - now) / 1000)) : null;

  useEffect(() => {
    if (remainingSec === 0 && attempt && !result && !submitting) {
      void submit();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [remainingSec]);

  async function onAnswer(question: StudentQuestion, response: LocalResponse) {
    if (!attempt) return;
    setAnswers((prev) => ({ ...prev, [question.id]: response }));
    await upsertAnswer(attempt.attemptId, question.id, response, false);
  }

  async function submit() {
    if (!attempt || submitting) return;
    setSubmitting(true);
    // Persist anything not yet queued.
    for (const q of attempt.questions) {
      const r = answers[q.id];
      if (r !== undefined) await upsertAnswer(attempt.attemptId, q.id, r, true);
    }
    try {
      const finalized = await finalizeAttempt(attempt.attemptId, tabSwitchRef.current);
      setResult(finalized);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSubmitting(false);
    }
  }

  const answeredCount = useMemo(() => Object.keys(answers).length, [answers]);

  if (loading) return <div>…</div>;
  if (error) return <div className="error">{error}</div>;
  if (!attempt) return null;

  if (result) {
    return (
      <div className="card" style={{ marginTop: '1rem' }}>
        <h2 style={{ marginTop: 0 }}>{t('student.quiz.result.title')}</h2>
        <p>
          {t('student.quiz.result.auto', { score: result.autoScore ?? '0', total: result.totalPoints ?? '0' })}
        </p>
        {result.needsManualGrading && <p>{t('student.quiz.result.needs_manual')}</p>}
        <button onClick={() => navigate('/student')}>{t('student.quiz.result.back')}</button>
      </div>
    );
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
        <h1 style={{ margin: 0, fontSize: '1.4rem' }}>{attempt.title}</h1>
        {remainingSec !== null && (
          <div style={{ fontVariantNumeric: 'tabular-nums', fontWeight: 600, color: remainingSec < 60 ? 'var(--color-danger)' : 'var(--color-text)' }}>
            {formatDuration(remainingSec)}
          </div>
        )}
      </div>
      {attempt.instructions && (
        <p style={{ color: 'var(--color-text-muted)', marginTop: '0.5rem' }}>{attempt.instructions}</p>
      )}
      <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', marginBottom: '1rem' }}>
        {t('student.quiz.progress', { answered: answeredCount, total: attempt.questions.length })}
        {flushHint ? ` · ${flushHint}` : ''}
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        {attempt.questions.map((q, idx) => (
          <QuestionCard
            key={q.id}
            index={idx + 1}
            question={q}
            value={answers[q.id]}
            onChange={(r) => void onAnswer(q, r)}
          />
        ))}
      </div>

      <div style={{ marginTop: '1.5rem', display: 'flex', gap: '0.75rem' }}>
        <button onClick={() => void submit()} disabled={submitting} style={{ flex: 1 }}>
          {submitting ? t('student.quiz.submitting') : t('student.quiz.submit', { count: answeredCount, total: attempt.questions.length })}
        </button>
      </div>
    </div>
  );
}

function formatDuration(sec: number): string {
  const mm = Math.floor(sec / 60).toString().padStart(2, '0');
  const ss = (sec % 60).toString().padStart(2, '0');
  return `${mm}:${ss}`;
}

interface QuestionCardProps {
  index: number;
  question: StudentQuestion;
  value: LocalResponse | undefined;
  onChange: (r: LocalResponse) => void;
}

function QuestionCard({ index, question, value, onChange }: QuestionCardProps) {
  const { t } = useTranslation();
  return (
    <div className="card">
      <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', marginBottom: '0.25rem' }}>
        {t('student.quiz.question_n', { n: index })} · {t(`question.type.${question.questionType}`)}
      </div>
      <div style={{ fontWeight: 500, marginBottom: '0.75rem' }}>{question.prompt}</div>
      {question.questionType === 'MULTIPLE_CHOICE' && (
        <MultipleChoice
          options={question.payload.options ?? []}
          selected={(value as { selectedKeys?: string[] } | undefined)?.selectedKeys ?? []}
          onChange={(selectedKeys) => onChange({ selectedKeys })}
        />
      )}
      {question.questionType === 'TRUE_FALSE' && (
        <TrueFalse
          value={(value as { answer?: boolean } | undefined)?.answer}
          onChange={(answer) => onChange({ answer })}
        />
      )}
      {(question.questionType === 'SHORT_ANSWER' || question.questionType === 'ESSAY') && (
        <textarea
          value={(value as { text?: string } | undefined)?.text ?? ''}
          onChange={(e) => onChange({ text: e.target.value })}
          rows={question.questionType === 'ESSAY' ? 6 : 2}
          style={{ width: '100%', boxSizing: 'border-box' }}
        />
      )}
    </div>
  );
}

function MultipleChoice({ options, selected, onChange }:{
  options: { key: string; text: string }[];
  selected: string[];
  onChange: (keys: string[]) => void;
}) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem' }}>
      {options.map((o) => {
        const active = selected.includes(o.key);
        return (
          <button
            key={o.key}
            type="button"
            onClick={() => onChange(active ? selected.filter((k) => k !== o.key) : [...selected, o.key])}
            className={active ? '' : 'secondary'}
            style={{ textAlign: 'left', width: '100%' }}
          >
            <span style={{ fontWeight: 600, marginRight: '0.5rem' }}>{o.key}.</span>{o.text}
          </button>
        );
      })}
    </div>
  );
}

function TrueFalse({ value, onChange }: { value: boolean | undefined; onChange: (v: boolean) => void }) {
  const { t } = useTranslation();
  return (
    <div style={{ display: 'flex', gap: '0.5rem' }}>
      <button className={value === true ? '' : 'secondary'} onClick={() => onChange(true)}>
        {t('question.true')}
      </button>
      <button className={value === false ? '' : 'secondary'} onClick={() => onChange(false)}>
        {t('question.false')}
      </button>
    </div>
  );
}
