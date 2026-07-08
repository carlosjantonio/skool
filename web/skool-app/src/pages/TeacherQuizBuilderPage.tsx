import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import * as academic from '../api/academic';
import * as staff from '../api/staff';
import * as quizzes from '../api/teacherQuizzes';
import type {
  AcademicYear,
  QuestionPayload,
  QuestionType,
  StaffAssignment,
  Subject,
  Turma,
} from '../api/types';

interface DraftQuestion {
  localKey: string;
  prompt: string;
  questionType: QuestionType;
  points: number;
  // MC
  options: { key: string; text: string; correct: boolean }[];
  // T/F
  tfCorrect: boolean;
  // Short
  acceptedAnswers: string[];
  // Essay
  rubric: string;
}

function blank(): DraftQuestion {
  return {
    localKey: crypto.randomUUID(),
    prompt: '',
    questionType: 'MULTIPLE_CHOICE',
    points: 1,
    options: [
      { key: 'A', text: '', correct: false },
      { key: 'B', text: '', correct: false },
    ],
    tfCorrect: true,
    acceptedAnswers: [''],
    rubric: '',
  };
}

/**
 * Inline builder. Every question is authored right here — no library-picker flow.
 * Each Save creates a new Question in the bank AND appends it to the quiz. When the
 * teacher is done, Publish flips the quiz to PUBLISHED and it appears on students'
 * dashboards. Publishing an empty quiz is refused server-side.
 */
export function TeacherQuizBuilderPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { turmaId } = useParams<{ turmaId: string }>();

  const [year, setYear] = useState<AcademicYear | null>(null);
  const [turma, setTurma] = useState<Turma | null>(null);
  const [subjects, setSubjects] = useState<Record<string, Subject>>({});
  const [myAssignments, setMyAssignments] = useState<StaffAssignment[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const [title, setTitle] = useState('');
  const [subjectId, setSubjectId] = useState('');
  const [trimesterKey, setTrimesterKey] = useState<'T1' | 'T2' | 'T3'>('T1');
  const [timeLimitMinutes, setTimeLimitMinutes] = useState<number | ''>(30);
  const [instructions, setInstructions] = useState('');
  const [drafts, setDrafts] = useState<DraftQuestion[]>([blank()]);
  const [saving, setSaving] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [savedQuizId, setSavedQuizId] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      if (!turmaId) return;
      try {
        const years = await academic.listAcademicYears();
        const current = years.find((y) => y.current) ?? years[0];
        setYear(current);
        if (!current) { setLoading(false); return; }
        const [ts, subs, myAsgn] = await Promise.all([
          academic.listTurmas(current.id),
          academic.listSubjects(),
          staff.getMyAssignments(current.id),
        ]);
        setTurma(ts.find((x) => x.id === turmaId) ?? null);
        setSubjects(Object.fromEntries(subs.map((s) => [s.id, s])));
        const mine = myAsgn.filter((a) => a.turmaId === turmaId && a.subjectId);
        setMyAssignments(mine);
        if (mine.length > 0 && mine[0].subjectId) setSubjectId(mine[0].subjectId);
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    }
    void load();
  }, [turmaId]);

  const subjectOptions = useMemo(
    () => myAssignments.map((a) => a.subjectId!).filter((v, i, arr) => arr.indexOf(v) === i),
    [myAssignments]
  );

  function updateDraft(idx: number, patch: Partial<DraftQuestion>) {
    setDrafts((prev) => prev.map((d, i) => (i === idx ? { ...d, ...patch } : d)));
  }

  function addQuestion() {
    setDrafts((prev) => [...prev, blank()]);
  }

  function removeQuestion(idx: number) {
    setDrafts((prev) => prev.filter((_, i) => i !== idx));
  }

  function payloadFor(d: DraftQuestion): QuestionPayload {
    switch (d.questionType) {
      case 'MULTIPLE_CHOICE':
        return { options: d.options };
      case 'TRUE_FALSE':
        return { correct: d.tfCorrect };
      case 'SHORT_ANSWER':
        return { acceptedAnswers: d.acceptedAnswers.filter((s) => s.trim().length > 0) };
      case 'ESSAY':
        return { rubric: d.rubric };
    }
  }

  function isValid(d: DraftQuestion): boolean {
    if (!d.prompt.trim()) return false;
    switch (d.questionType) {
      case 'MULTIPLE_CHOICE':
        return d.options.length >= 2
          && d.options.every((o) => o.text.trim().length > 0)
          && d.options.some((o) => o.correct);
      case 'SHORT_ANSWER':
        return d.acceptedAnswers.some((a) => a.trim().length > 0);
      default:
        return true;
    }
  }

  async function saveDraftQuiz() {
    if (!turma || !year || !subjectId) return;
    if (!title.trim()) { setError(t('teacher.quiz_builder.err.title_required')); return; }
    const valid = drafts.filter(isValid);
    if (valid.length === 0) { setError(t('teacher.quiz_builder.err.no_questions')); return; }

    const gradeLevel = turma.gradeLevel;
    setSaving(true);
    setError(null);
    try {
      // Create each question, collect ids.
      const questionIds: string[] = [];
      for (const d of valid) {
        const q = await quizzes.createQuestion({
          subjectId,
          gradeLevel,
          prompt: d.prompt.trim(),
          questionType: d.questionType,
          payload: payloadFor(d),
          points: d.points,
        });
        questionIds.push(q.id);
      }
      const quiz = await quizzes.createQuiz({
        subjectId,
        turmaId: turma.id,
        academicYearId: year.id,
        trimesterKey,
        title: title.trim(),
        instructions: instructions.trim() || undefined,
        timeLimitSeconds: timeLimitMinutes === '' ? undefined : Number(timeLimitMinutes) * 60,
        questionIds,
      });
      setSavedQuizId(quiz.id);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSaving(false);
    }
  }

  async function publish() {
    if (!savedQuizId || !turmaId) return;
    setPublishing(true);
    try {
      await quizzes.publishQuiz(savedQuizId);
      navigate(`/classes/${turmaId}/quizzes/${savedQuizId}/results`);
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setPublishing(false);
    }
  }

  if (loading) return <div>…</div>;
  if (!turma || !year) return <div>{t('teacher.no_year')}</div>;

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>{t('teacher.quiz_builder.title', { turma: turma.name })}</h1>
      {error && <div className="error" style={{ marginBottom: '0.75rem' }}>{error}</div>}

      <div className="card">
        <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))' }}>
          <label>
            <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', marginBottom: '0.25rem' }}>
              {t('teacher.quiz_builder.field.title')}
            </div>
            <input value={title} onChange={(e) => setTitle(e.target.value)} style={{ width: '100%', boxSizing: 'border-box' }} />
          </label>
          <label>
            <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', marginBottom: '0.25rem' }}>
              {t('teacher.quiz_builder.field.subject')}
            </div>
            <select value={subjectId} onChange={(e) => setSubjectId(e.target.value)} style={{ width: '100%' }}>
              <option value="" disabled>{t('teacher.grades.pick_subject')}</option>
              {subjectOptions.map((sid) => (
                <option key={sid} value={sid}>{subjects[sid]?.name ?? sid}</option>
              ))}
            </select>
          </label>
          <label>
            <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', marginBottom: '0.25rem' }}>
              {t('teacher.quiz_builder.field.trimester')}
            </div>
            <select value={trimesterKey} onChange={(e) => setTrimesterKey(e.target.value as 'T1' | 'T2' | 'T3')} style={{ width: '100%' }}>
              <option value="T1">{t('domain.trimestre.1')}</option>
              <option value="T2">{t('domain.trimestre.2')}</option>
              <option value="T3">{t('domain.trimestre.3')}</option>
            </select>
          </label>
          <label>
            <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', marginBottom: '0.25rem' }}>
              {t('teacher.quiz_builder.field.time_limit')}
            </div>
            <input
              type="number"
              min={1}
              value={timeLimitMinutes}
              onChange={(e) => setTimeLimitMinutes(e.target.value === '' ? '' : Number(e.target.value))}
              style={{ width: '100%', boxSizing: 'border-box' }}
            />
          </label>
        </div>
        <label style={{ display: 'block', marginTop: '0.75rem' }}>
          <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', marginBottom: '0.25rem' }}>
            {t('teacher.quiz_builder.field.instructions')}
          </div>
          <textarea value={instructions} onChange={(e) => setInstructions(e.target.value)} rows={2} style={{ width: '100%', boxSizing: 'border-box' }} />
        </label>
      </div>

      <h2 style={{ marginTop: '1.5rem', fontSize: '1.1rem' }}>{t('teacher.quiz_builder.questions')}</h2>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        {drafts.map((d, i) => (
          <QuestionEditor
            key={d.localKey}
            index={i + 1}
            draft={d}
            onChange={(patch) => updateDraft(i, patch)}
            onRemove={drafts.length > 1 ? () => removeQuestion(i) : undefined}
          />
        ))}
      </div>

      <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1rem', flexWrap: 'wrap' }}>
        <button className="secondary" onClick={addQuestion}>{t('teacher.quiz_builder.add_question')}</button>
        {!savedQuizId && (
          <button onClick={() => void saveDraftQuiz()} disabled={saving || !subjectId}>
            {saving ? t('teacher.quiz_builder.saving') : t('teacher.quiz_builder.save_draft')}
          </button>
        )}
        {savedQuizId && (
          <button onClick={() => void publish()} disabled={publishing}>
            {publishing ? t('teacher.quiz_builder.publishing') : t('teacher.quiz_builder.publish')}
          </button>
        )}
      </div>
    </div>
  );
}

interface EditorProps {
  index: number;
  draft: DraftQuestion;
  onChange: (patch: Partial<DraftQuestion>) => void;
  onRemove?: () => void;
}

function QuestionEditor({ index, draft, onChange, onRemove }: EditorProps) {
  const { t } = useTranslation();
  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: '0.5rem', alignItems: 'center', marginBottom: '0.5rem' }}>
        <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>
          {t('student.quiz.question_n', { n: index })}
        </div>
        {onRemove && (
          <button className="secondary" onClick={onRemove} style={{ fontSize: '0.85rem', padding: '0.15rem 0.5rem' }}>
            {t('teacher.quiz_builder.remove')}
          </button>
        )}
      </div>
      <div style={{ display: 'grid', gap: '0.5rem', gridTemplateColumns: '2fr 1fr 1fr' }}>
        <textarea
          rows={2}
          value={draft.prompt}
          onChange={(e) => onChange({ prompt: e.target.value })}
          placeholder={t('teacher.quiz_builder.prompt_placeholder')}
          style={{ width: '100%', boxSizing: 'border-box' }}
        />
        <select
          value={draft.questionType}
          onChange={(e) => onChange({ questionType: e.target.value as QuestionType })}
          style={{ width: '100%' }}
        >
          <option value="MULTIPLE_CHOICE">{t('question.type.MULTIPLE_CHOICE')}</option>
          <option value="TRUE_FALSE">{t('question.type.TRUE_FALSE')}</option>
          <option value="SHORT_ANSWER">{t('question.type.SHORT_ANSWER')}</option>
          <option value="ESSAY">{t('question.type.ESSAY')}</option>
        </select>
        <input
          type="number"
          min={0.25}
          step={0.25}
          value={draft.points}
          onChange={(e) => onChange({ points: Number(e.target.value) })}
          style={{ width: '100%', boxSizing: 'border-box' }}
        />
      </div>

      {draft.questionType === 'MULTIPLE_CHOICE' && (
        <MultipleChoiceEditor
          options={draft.options}
          onChange={(options) => onChange({ options })}
        />
      )}
      {draft.questionType === 'TRUE_FALSE' && (
        <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
          <button className={draft.tfCorrect ? '' : 'secondary'} onClick={() => onChange({ tfCorrect: true })}>
            {t('question.true')}
          </button>
          <button className={!draft.tfCorrect ? '' : 'secondary'} onClick={() => onChange({ tfCorrect: false })}>
            {t('question.false')}
          </button>
        </div>
      )}
      {draft.questionType === 'SHORT_ANSWER' && (
        <div style={{ marginTop: '0.5rem' }}>
          <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', marginBottom: '0.25rem' }}>
            {t('teacher.quiz_builder.accepted_answers')}
          </div>
          {draft.acceptedAnswers.map((a, i) => (
            <input
              key={i}
              value={a}
              onChange={(e) => {
                const next = [...draft.acceptedAnswers];
                next[i] = e.target.value;
                onChange({ acceptedAnswers: next });
              }}
              placeholder={t('teacher.quiz_builder.accepted_placeholder')}
              style={{ width: '100%', boxSizing: 'border-box', marginBottom: '0.35rem' }}
            />
          ))}
          <button className="secondary" onClick={() => onChange({ acceptedAnswers: [...draft.acceptedAnswers, ''] })}>
            {t('teacher.quiz_builder.add_accepted')}
          </button>
        </div>
      )}
      {draft.questionType === 'ESSAY' && (
        <textarea
          value={draft.rubric}
          onChange={(e) => onChange({ rubric: e.target.value })}
          placeholder={t('teacher.quiz_builder.rubric_placeholder')}
          rows={2}
          style={{ width: '100%', boxSizing: 'border-box', marginTop: '0.5rem' }}
        />
      )}
    </div>
  );
}

function MultipleChoiceEditor({ options, onChange }: {
  options: { key: string; text: string; correct: boolean }[];
  onChange: (v: typeof options) => void;
}) {
  const { t } = useTranslation();
  function set(i: number, patch: Partial<(typeof options)[number]>) {
    onChange(options.map((o, idx) => (idx === i ? { ...o, ...patch } : o)));
  }
  function add() {
    const nextKey = String.fromCharCode(65 + options.length); // A, B, C…
    onChange([...options, { key: nextKey, text: '', correct: false }]);
  }
  function remove(i: number) {
    onChange(options.filter((_, idx) => idx !== i));
  }
  return (
    <div style={{ marginTop: '0.5rem', display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
      {options.map((o, i) => (
        <div key={i} style={{ display: 'flex', gap: '0.35rem', alignItems: 'center' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
            <input
              type="checkbox"
              checked={o.correct}
              onChange={(e) => set(i, { correct: e.target.checked })}
            />
            <span style={{ fontWeight: 600, minWidth: '1rem' }}>{o.key}</span>
          </label>
          <input
            value={o.text}
            onChange={(e) => set(i, { text: e.target.value })}
            placeholder={t('teacher.quiz_builder.option_placeholder')}
            style={{ flex: 1 }}
          />
          {options.length > 2 && (
            <button className="secondary" onClick={() => remove(i)} style={{ padding: '0.15rem 0.5rem' }}>×</button>
          )}
        </div>
      ))}
      <button className="secondary" onClick={add} style={{ alignSelf: 'flex-start' }}>
        {t('teacher.quiz_builder.add_option')}
      </button>
    </div>
  );
}
