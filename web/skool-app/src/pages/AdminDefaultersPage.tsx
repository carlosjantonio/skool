import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import * as fees from '../api/fees';
import type { DefaulterRow } from '../api/types';

export function AdminDefaultersPage() {
  const { t } = useTranslation();
  const [rows, setRows] = useState<DefaulterRow[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [sweeping, setSweeping] = useState(false);

  useEffect(() => { void load(); }, []);

  async function load() {
    try {
      setLoading(true);
      setRows(await fees.listDefaulters());
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  }

  async function sweep() {
    setSweeping(true);
    try {
      const r = await fees.sweepOverdue();
      await load();
      alert(t('admin.defaulters.swept', { count: r.flipped }));
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setSweeping(false);
    }
  }

  if (loading) return <div>…</div>;
  if (error) return <div className="error">{error}</div>;

  const totalOwed = rows.reduce((sum, r) => sum + Number(r.totalOutstanding), 0).toFixed(2);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', flexWrap: 'wrap', gap: '1rem' }}>
        <h1 style={{ margin: 0 }}>{t('admin.defaulters.title')}</h1>
        <button className="secondary" onClick={() => void sweep()} disabled={sweeping}>
          {sweeping ? t('admin.defaulters.sweeping') : t('admin.defaulters.sweep')}
        </button>
      </div>
      <div className="card" style={{ marginTop: '1rem' }}>
        <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>{t('admin.defaulters.total_owed')}</div>
        <div style={{ fontSize: '1.75rem', fontWeight: 700 }}>{formatKz(totalOwed)}</div>
      </div>

      {rows.length === 0 ? (
        <div className="card" style={{ marginTop: '1rem', color: 'var(--color-text-muted)' }}>{t('admin.defaulters.empty')}</div>
      ) : (
        <div style={{ overflowX: 'auto', marginTop: '1rem' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr>
                <th style={{ textAlign: 'left', padding: '0.5rem', borderBottom: '1px solid var(--color-border)' }}>{t('admin.defaulters.student')}</th>
                <th style={{ textAlign: 'right', padding: '0.5rem', borderBottom: '1px solid var(--color-border)' }}>{t('admin.defaulters.count')}</th>
                <th style={{ textAlign: 'right', padding: '0.5rem', borderBottom: '1px solid var(--color-border)' }}>{t('admin.defaulters.outstanding')}</th>
                <th style={{ textAlign: 'right', padding: '0.5rem', borderBottom: '1px solid var(--color-border)' }}>{t('admin.defaulters.oldest')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.studentId}>
                  <td style={{ padding: '0.5rem', borderBottom: '1px solid var(--color-border)' }}>{r.studentName}</td>
                  <td style={{ padding: '0.5rem', borderBottom: '1px solid var(--color-border)', textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>{r.overdueInvoiceCount}</td>
                  <td style={{ padding: '0.5rem', borderBottom: '1px solid var(--color-border)', textAlign: 'right', fontVariantNumeric: 'tabular-nums', color: 'var(--color-danger)', fontWeight: 600 }}>
                    {formatKz(r.totalOutstanding)}
                  </td>
                  <td style={{ padding: '0.5rem', borderBottom: '1px solid var(--color-border)', textAlign: 'right', fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>
                    {new Date(r.oldestDueAt).toLocaleDateString('pt-AO')}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function formatKz(amount: string): string {
  const n = Number(amount);
  if (Number.isNaN(n)) return `${amount} Kz`;
  return `${n.toLocaleString('pt-AO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} Kz`;
}
