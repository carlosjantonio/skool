import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import * as academic from '../api/academic';
import * as fees from '../api/fees';
import type { AcademicYear, BillingResult, FeeKind, FeeSchedule } from '../api/types';

/**
 * Admin surface for fee schedules + monthly propina runs. Creating a schedule is a
 * simple form; "Executar cobrança" fans out invoices to every enrolled student
 * (respecting scholarships), and the result banner shows issued vs. skipped so
 * running twice by mistake is a no-op.
 */
export function AdminFeesPage() {
  const { t } = useTranslation();
  const [year, setYear] = useState<AcademicYear | null>(null);
  const [schedules, setSchedules] = useState<FeeSchedule[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [lastResult, setLastResult] = useState<BillingResult | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const [name, setName] = useState('');
  const [kind, setKind] = useState<FeeKind>('PROPINA_MENSAL');
  const [amount, setAmount] = useState('25000.00');
  const [gradeLevel, setGradeLevel] = useState('');
  const [periodMonth, setPeriodMonth] = useState<number | ''>(new Date().getMonth() + 1);
  const [dueAt, setDueAt] = useState(defaultDueDate());

  useEffect(() => { void load(); }, []);

  async function load() {
    try {
      setLoading(true);
      const years = await academic.listAcademicYears();
      const current = years.find((y) => y.current) ?? years[0];
      setYear(current);
      if (current) setSchedules(await fees.listSchedules(current.id));
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  }

  async function createSchedule(e: React.FormEvent) {
    e.preventDefault();
    if (!year) return;
    setCreating(true);
    setError(null);
    try {
      await fees.createSchedule({
        academicYearId: year.id,
        name: name.trim(),
        kind,
        amount,
        gradeLevel: gradeLevel || undefined,
        periodMonth: kind === 'PROPINA_MENSAL' ? (periodMonth === '' ? undefined : Number(periodMonth)) : undefined,
        dueAt,
      });
      setName('');
      await load();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setCreating(false);
    }
  }

  async function runBilling(scheduleId: string) {
    setBusyId(scheduleId);
    setError(null);
    try {
      setLastResult(await fees.runBilling(scheduleId));
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusyId(null);
    }
  }

  const totalPending = useMemo(
    () => schedules.reduce((sum, s) => sum + Number(s.amount), 0).toFixed(2),
    [schedules]
  );

  if (loading) return <div>…</div>;
  if (!year) return <div>{t('teacher.no_year')}</div>;

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>{t('admin.fees.title', { year: year.name })}</h1>
      {error && <div className="error" style={{ marginBottom: '1rem' }}>{error}</div>}

      {lastResult && (
        <div className="card" style={{ marginBottom: '1rem', background: 'var(--color-bg-alt, #f5f5f5)' }}>
          {t('admin.fees.result', {
            issued: lastResult.issuedCount,
            skipped: lastResult.skippedCount,
            failed: lastResult.failedCount,
          })}
        </div>
      )}

      <section className="card" style={{ marginBottom: '1.5rem' }}>
        <h2 style={{ marginTop: 0, fontSize: '1.05rem' }}>{t('admin.fees.new_schedule')}</h2>
        <form onSubmit={createSchedule} style={{ display: 'grid', gap: '0.5rem', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))' }}>
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder={t('admin.fees.name_placeholder')} required />
          <select value={kind} onChange={(e) => setKind(e.target.value as FeeKind)}>
            <option value="PROPINA_MENSAL">{t('fee.kind.PROPINA_MENSAL')}</option>
            <option value="MATRICULA">{t('fee.kind.MATRICULA')}</option>
            <option value="EXAME">{t('fee.kind.EXAME')}</option>
            <option value="UNIFORME">{t('fee.kind.UNIFORME')}</option>
            <option value="MATERIAL">{t('fee.kind.MATERIAL')}</option>
            <option value="OUTRO">{t('fee.kind.OUTRO')}</option>
          </select>
          <input type="number" step="0.01" min="0" value={amount} onChange={(e) => setAmount(e.target.value)} required />
          <select value={gradeLevel} onChange={(e) => setGradeLevel(e.target.value)}>
            <option value="">{t('admin.fees.all_grades')}</option>
            {['CLASSE_10', 'CLASSE_11', 'CLASSE_12', 'CLASSE_13'].map((g) => (
              <option key={g} value={g}>{t(`gradeLevel.${g}`, g)}</option>
            ))}
          </select>
          {kind === 'PROPINA_MENSAL' && (
            <select value={periodMonth} onChange={(e) => setPeriodMonth(e.target.value === '' ? '' : Number(e.target.value))}>
              {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
                <option key={m} value={m}>{t(`month.${m}`)}</option>
              ))}
            </select>
          )}
          <input type="datetime-local" value={dueAt} onChange={(e) => setDueAt(e.target.value)} required />
          <button type="submit" disabled={creating || !name.trim()}>
            {creating ? t('admin.fees.creating') : t('admin.fees.create')}
          </button>
        </form>
      </section>

      <h2 style={{ fontSize: '1.05rem' }}>{t('admin.fees.schedules', { total: formatKz(totalPending) })}</h2>
      {schedules.length === 0 ? (
        <div className="card" style={{ color: 'var(--color-text-muted)' }}>{t('admin.fees.empty')}</div>
      ) : (
        <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))' }}>
          {schedules.map((s) => (
            <div key={s.id} className="card">
              <div style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>
                {t(`fee.kind.${s.kind}`)}
                {s.periodMonth ? ` · ${t(`month.${s.periodMonth}`)}` : ''}
                {s.gradeLevel ? ` · ${t(`gradeLevel.${s.gradeLevel}`, s.gradeLevel)}` : ''}
              </div>
              <div style={{ fontWeight: 600, marginTop: '0.25rem' }}>{s.name}</div>
              <div style={{ fontSize: '1.4rem', fontWeight: 700, marginTop: '0.5rem' }}>{formatKz(s.amount)}</div>
              <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', marginTop: '0.25rem' }}>
                {t('admin.fees.due', { date: new Date(s.dueAt).toLocaleDateString('pt-AO') })}
              </div>
              <button
                style={{ width: '100%', marginTop: '0.75rem' }}
                onClick={() => void runBilling(s.id)}
                disabled={busyId === s.id}
              >
                {busyId === s.id ? t('admin.fees.running') : t('admin.fees.run_billing')}
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function defaultDueDate(): string {
  const d = new Date();
  d.setDate(d.getDate() + 14);
  d.setSeconds(0);
  d.setMilliseconds(0);
  return d.toISOString().slice(0, 16);
}

function formatKz(amount: string): string {
  const n = Number(amount);
  if (Number.isNaN(n)) return `${amount} Kz`;
  return `${n.toLocaleString('pt-AO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} Kz`;
}
