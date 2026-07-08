import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import * as academic from '../api/academic';
import * as staff from '../api/staff';
import type { AcademicYear, StaffAssignment, Subject, Turma } from '../api/types';

export function TeacherClassesPage() {
  const { t } = useTranslation();
  const [year, setYear] = useState<AcademicYear | null>(null);
  const [assignments, setAssignments] = useState<StaffAssignment[]>([]);
  const [turmas, setTurmas] = useState<Record<string, Turma>>({});
  const [subjects, setSubjects] = useState<Record<string, Subject>>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const years = await academic.listAcademicYears();
        const current = years.find((y) => y.current) ?? years[0];
        if (!current) { setLoading(false); return; }
        setYear(current);

        const [mine, ts, subs] = await Promise.all([
          staff.getMyAssignments(current.id),
          academic.listTurmas(current.id),
          academic.listSubjects(),
        ]);
        setAssignments(mine);
        setTurmas(Object.fromEntries(ts.map((x) => [x.id, x])));
        setSubjects(Object.fromEntries(subs.map((x) => [x.id, x])));
      } catch (err) {
        setError((err as Error).message);
      } finally {
        setLoading(false);
      }
    }
    void load();
  }, []);

  // Group by turma so a teacher who teaches 3 subjects in 10A sees one card with 3 subjects.
  const grouped = useMemo(() => {
    const map = new Map<string, StaffAssignment[]>();
    for (const a of assignments) {
      const list = map.get(a.turmaId) ?? [];
      list.push(a);
      map.set(a.turmaId, list);
    }
    return Array.from(map.entries());
  }, [assignments]);

  if (loading) return <div>…</div>;
  if (error) return <div className="error">{error}</div>;
  if (!year) return <div>{t('teacher.no_year')}</div>;

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>{t('teacher.classes.title', { year: year.name })}</h1>
      {grouped.length === 0 && (
        <div className="card" style={{ marginTop: '1rem', color: 'var(--color-text-muted)' }}>
          {t('teacher.classes.empty')}
        </div>
      )}
      <div style={{ display: 'grid', gap: '1rem', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', marginTop: '1.5rem' }}>
        {grouped.map(([turmaId, group]) => {
          const turma = turmas[turmaId];
          if (!turma) return null;
          return (
            <div key={turmaId} className="card">
              <h2 style={{ marginTop: 0, fontSize: '1.15rem' }}>{turma.name}</h2>
              <div style={{ color: 'var(--color-text-muted)', fontSize: '0.9rem', marginBottom: '0.75rem' }}>
                {t(`gradeLevel.${turma.gradeLevel}`, turma.gradeLevel)} · {t(`track.${turma.track}`, turma.track)}
              </div>
              <div style={{ marginBottom: '1rem' }}>
                {group.map((a) => (
                  <div key={a.id} style={{ fontSize: '0.9rem', marginBottom: '0.25rem' }}>
                    {a.role === 'HEAD_TEACHER'
                      ? t('teacher.role.head')
                      : subjects[a.subjectId ?? '']?.name ?? '—'}
                  </div>
                ))}
              </div>
              <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                <Link to={`/classes/${turmaId}/attendance`} style={{ flex: '1 1 45%' }}>
                  <button style={{ width: '100%' }}>{t('teacher.classes.attendance')}</button>
                </Link>
                <Link to={`/classes/${turmaId}/grades`} style={{ flex: '1 1 45%' }}>
                  <button className="secondary" style={{ width: '100%' }}>{t('teacher.classes.grades')}</button>
                </Link>
                <Link to={`/classes/${turmaId}/quizzes`} style={{ flex: '1 1 100%' }}>
                  <button className="secondary" style={{ width: '100%' }}>{t('teacher.classes.quizzes')}</button>
                </Link>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
