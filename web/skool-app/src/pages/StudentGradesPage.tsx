import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import * as student from '../api/student';
import type { StudentGradeSummary, StudentSelf } from '../api/types';

export function StudentGradesPage() {
  const { t } = useTranslation();
  const [me, setMe] = useState<StudentSelf | null>(null);
  const [summary, setSummary] = useState<StudentGradeSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const self = await student.getMe();
        setMe(self);
        if (self.academicYearId) {
          setSummary(await student.getMyGradeSummary(self.studentId, self.academicYearId));
        }
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
  if (!summary) return <div>{t('student.grades.no_data')}</div>;

  const trimesters = ['T1', 'T2', 'T3'] as const;
  const subjects = new Map<string, { name: string; scores: Record<string, string> }>();
  for (const row of summary.subjectAverages) {
    const entry = subjects.get(row.subjectId) ?? { name: row.subjectName, scores: {} };
    entry.scores[row.trimesterKey] = row.average;
    subjects.set(row.subjectId, entry);
  }

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>{t('student.grades.title', { year: summary.academicYearName })}</h1>
      <div className="card" style={{ marginBottom: '1rem' }}>
        <div style={{ fontSize: '0.9rem', color: 'var(--color-text-muted)' }}>{t('student.grades.final')}</div>
        <div style={{ fontSize: '2rem', fontWeight: 700 }}>{summary.finalAverage}</div>
      </div>

      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr>
              <th style={{ textAlign: 'left', padding: '0.5rem', borderBottom: '1px solid var(--color-border)' }}>
                {t('student.grades.subject')}
              </th>
              {trimesters.map((tk) => (
                <th key={tk} style={{ textAlign: 'right', padding: '0.5rem', borderBottom: '1px solid var(--color-border)' }}>
                  {t(`domain.trimestre.${tk.slice(1)}`)}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {[...subjects.entries()].map(([sid, entry]) => (
              <tr key={sid}>
                <td style={{ padding: '0.5rem', borderBottom: '1px solid var(--color-border)' }}>{entry.name}</td>
                {trimesters.map((tk) => (
                  <td key={tk} style={{ textAlign: 'right', padding: '0.5rem', borderBottom: '1px solid var(--color-border)', fontVariantNumeric: 'tabular-nums' }}>
                    {entry.scores[tk] ?? '—'}
                  </td>
                ))}
              </tr>
            ))}
            <tr>
              <td style={{ padding: '0.5rem', fontWeight: 600 }}>{t('student.grades.trim_avg')}</td>
              {trimesters.map((tk) => (
                <td key={tk} style={{ textAlign: 'right', padding: '0.5rem', fontWeight: 600 }}>
                  {summary.trimesterAverages[tk] ?? '—'}
                </td>
              ))}
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}
