import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import * as academic from '../api/academic';
import * as grades from '../api/grades';
import * as students from '../api/students';
import type { AcademicYear, GradeEntry, Student, Subject, Turma } from '../api/types';

export function TeacherGradesPage() {
  const { t } = useTranslation();
  const { turmaId } = useParams<{ turmaId: string }>();
  const [year, setYear] = useState<AcademicYear | null>(null);
  const [turma, setTurma] = useState<Turma | null>(null);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [subjectId, setSubjectId] = useState<string>('');
  const [trimester, setTrimester] = useState<'T1' | 'T2' | 'T3'>('T1');
  const [roster, setRoster] = useState<Student[]>([]);
  const [drafts, setDrafts] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveMessage, setSaveMessage] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      if (!turmaId) return;
      setLoading(true);
      try {
        const years = await academic.listAcademicYears();
        const current = years.find((y) => y.current) ?? years[0];
        if (!current) throw new Error('no academic year');
        setYear(current);

        const [ts, subs, enrolls, allStudents] = await Promise.all([
          academic.listTurmas(current.id),
          academic.listSubjects(),
          students.listEnrollments(current.id, turmaId),
          students.listStudents(),
        ]);
        const thisTurma = ts.find((x) => x.id === turmaId) ?? null;
        setTurma(thisTurma);
        setSubjects(subs);
        if (subs.length > 0 && !subjectId) setSubjectId(subs[0].id);

        const enrolledIds = new Set(enrolls.filter((e) => e.status === 'ENROLLED').map((e) => e.studentId));
        setRoster(allStudents.filter((s) => enrolledIds.has(s.id))
          .sort((a, b) => a.fullName.localeCompare(b.fullName, 'pt-AO')));
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    }
    void load();
  }, [turmaId]);

  const setDraft = (studentId: string, value: string) =>
    setDrafts((prev) => ({ ...prev, [studentId]: value }));

  const pending = useMemo(() => {
    return Object.entries(drafts)
      .filter(([, v]) => v.trim() !== '')
      .map(([sid, v]) => ({ sid, value: v.trim() }));
  }, [drafts]);

  const save = async () => {
    if (!turmaId || !year || !subjectId) return;
    setSaving(true);
    setSaveMessage(null);
    try {
      const entries: GradeEntry[] = pending.map(({ sid, value }) => ({
        studentId: sid,
        subjectId,
        turmaId,
        academicYearId: year.id,
        trimesterKey: trimester,
        value,
        weight: '1.00',
        category: 'TEST',
      }));
      if (entries.length === 0) {
        setSaveMessage(t('teacher.grades.nothing_to_save'));
        return;
      }
      await grades.submitGrades(entries);
      setDrafts({});
      setSaveMessage(t('teacher.grades.saved', { count: entries.length }));
    } catch (err) {
      setSaveMessage((err as Error).message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <Link to="/classes" style={{ fontSize: '0.9rem' }}>&larr; {t('teacher.back_to_classes')}</Link>
      <h1 style={{ marginTop: '0.5rem', marginBottom: '0.5rem' }}>
        {t('teacher.grades.title', { turma: turma?.name ?? '' })}
      </h1>
      <div style={{ color: 'var(--color-text-muted)', marginBottom: '1rem' }}>
        {year ? t('teacher.grades.year', { year: year.name }) : null}
      </div>

      <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap' }}>
        <div style={{ flex: 1, minWidth: 200 }}>
          <label htmlFor="subject">{t('teacher.grades.subject')}</label>
          <select id="subject" value={subjectId} onChange={(e) => setSubjectId(e.target.value)}>
            <option value="">{t('teacher.grades.pick_subject')}</option>
            {subjects.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        </div>
        <div style={{ minWidth: 140 }}>
          <label htmlFor="trimester">{t('teacher.grades.trimester')}</label>
          <select id="trimester" value={trimester} onChange={(e) => setTrimester(e.target.value as 'T1' | 'T2' | 'T3')}>
            <option value="T1">{t('domain.trimestre.1')}</option>
            <option value="T2">{t('domain.trimestre.2')}</option>
            <option value="T3">{t('domain.trimestre.3')}</option>
          </select>
        </div>
        <button onClick={() => void save()} disabled={saving || pending.length === 0 || !subjectId} style={{ marginTop: '1.4rem' }}>
          {saving ? t('teacher.grades.saving') : t('teacher.grades.save', { count: pending.length })}
        </button>
      </div>

      {error && <div className="error">{error}</div>}
      {saveMessage && <div className="card" style={{ marginBottom: '1rem' }}>{saveMessage}</div>}
      {loading && <div>…</div>}

      {roster.map((student) => (
        <div
          key={student.id}
          className="card"
          data-testid="grade-row"
          data-student-id={student.id}
          style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '0.5rem', padding: '0.75rem 1rem' }}
        >
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 500 }}>{student.fullName}</div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <input
              type="number"
              min={0}
              max={20}
              step={0.1}
              value={drafts[student.id] ?? ''}
              onChange={(e) => setDraft(student.id, e.target.value)}
              placeholder="0 – 20"
              style={{ width: 96, textAlign: 'center' }}
            />
            <span style={{ color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>/ 20</span>
          </div>
        </div>
      ))}
    </div>
  );
}
