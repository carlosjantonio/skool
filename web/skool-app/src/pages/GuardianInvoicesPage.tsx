import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import * as fees from '../api/fees';
import type { Invoice, PaymentInitiation, PaymentMethod } from '../api/types';

/**
 * Guardian view of their children's invoices. The list itself is straight-forward;
 * the "Pagar" flow talks to a payment adapter and either records the payment
 * immediately (stub) or shows the ATM reference (Multicaixa) / USSD instructions
 * (Unitel / Africell / bank).
 */
export function GuardianInvoicesPage() {
  const { t } = useTranslation();
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [payingId, setPayingId] = useState<string | null>(null);
  const [method, setMethod] = useState<PaymentMethod>('MULTICAIXA_EXPRESS');
  const [initiation, setInitiation] = useState<PaymentInitiation | null>(null);

  useEffect(() => {
    load();
  }, []);

  async function load() {
    try {
      setLoading(true);
      setInvoices(await fees.guardianInvoices());
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  }

  async function initiate(invoice: Invoice) {
    setInitiation(null);
    try {
      const result = await fees.initiatePayment({ invoiceId: invoice.id, method });
      setInitiation(result);
      if (method === 'STUB_MANUAL') {
        await load();
      }
    } catch (err) {
      setError((err as Error).message);
    }
  }

  if (loading) return <div>…</div>;
  if (error) return <div className="error">{error}</div>;

  const totalOutstanding = invoices
    .reduce((sum, i) => sum + Number(i.outstanding), 0)
    .toFixed(2);

  return (
    <div>
      <h1 style={{ marginTop: 0 }}>{t('guardian.invoices.title')}</h1>
      <div className="card" style={{ marginBottom: '1rem' }}>
        <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)' }}>
          {t('guardian.invoices.outstanding')}
        </div>
        <div style={{ fontSize: '1.75rem', fontWeight: 700 }}>
          {formatKz(totalOutstanding)}
        </div>
      </div>

      {invoices.length === 0 ? (
        <div className="card" style={{ color: 'var(--color-text-muted)' }}>
          {t('guardian.invoices.empty')}
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
          {invoices.map((inv) => (
            <div key={inv.id} className="card">
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: '1rem', flexWrap: 'wrap' }}>
                <div style={{ flex: 1 }}>
                  <div style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>
                    {inv.reference} · {inv.studentName ?? ''}
                  </div>
                  <div style={{ fontWeight: 600 }}>{inv.title}</div>
                  <div style={{ fontSize: '0.85rem', color: 'var(--color-text-muted)', marginTop: '0.25rem' }}>
                    {t('guardian.invoices.due', { date: new Date(inv.dueAt).toLocaleDateString('pt-AO') })}
                    {' · '}
                    <span style={statusStyle(inv.status)}>{t(`invoice.status.${inv.status}`)}</span>
                  </div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)' }}>
                    {t('guardian.invoices.amount_due')}
                  </div>
                  <div style={{ fontSize: '1.35rem', fontWeight: 700 }}>
                    {formatKz(inv.outstanding)}
                  </div>
                  {Number(inv.amountDiscount) > 0 && (
                    <div style={{ fontSize: '0.75rem', color: 'var(--color-accent)' }}>
                      {t('guardian.invoices.discount', { amount: formatKz(inv.amountDiscount) })}
                    </div>
                  )}
                </div>
              </div>

              {inv.status !== 'PAID' && inv.status !== 'CANCELLED' && (
                <div style={{ marginTop: '0.75rem', display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
                  <select value={method} onChange={(e) => setMethod(e.target.value as PaymentMethod)}>
                    <option value="MULTICAIXA_EXPRESS">{t('payment.method.MULTICAIXA_EXPRESS')}</option>
                    <option value="UNITEL_MONEY">{t('payment.method.UNITEL_MONEY')}</option>
                    <option value="AFRICELL_MONEY">{t('payment.method.AFRICELL_MONEY')}</option>
                    <option value="BANK_TRANSFER">{t('payment.method.BANK_TRANSFER')}</option>
                  </select>
                  <button
                    onClick={() => { setPayingId(inv.id); void initiate(inv); }}
                    disabled={payingId === inv.id}
                  >
                    {t('guardian.invoices.pay')}
                  </button>
                </div>
              )}

              {initiation && payingId === inv.id && (
                <div className="card" style={{ marginTop: '0.75rem', background: 'var(--color-bg-alt, #f5f5f5)' }}>
                  <div style={{ fontSize: '0.8rem', color: 'var(--color-text-muted)', marginBottom: '0.25rem' }}>
                    {t('guardian.invoices.instructions')}
                  </div>
                  <div style={{ fontWeight: 600, marginBottom: '0.5rem' }}>
                    {t('guardian.invoices.ref')}: {initiation.vendorReference}
                  </div>
                  <div style={{ whiteSpace: 'pre-wrap' }}>{initiation.instructions}</div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function statusStyle(status: string): React.CSSProperties {
  const map: Record<string, string> = {
    ISSUED: 'var(--color-text-muted)',
    PARTIAL: 'var(--color-accent)',
    PAID: 'var(--color-accent)',
    OVERDUE: 'var(--color-danger)',
    CANCELLED: 'var(--color-text-muted)',
  };
  return { color: map[status] ?? 'var(--color-text)', fontWeight: 500 };
}

function formatKz(amount: string): string {
  const n = Number(amount);
  if (Number.isNaN(n)) return `${amount} Kz`;
  return `${n.toLocaleString('pt-AO', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} Kz`;
}
