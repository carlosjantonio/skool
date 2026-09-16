import { api } from './client';
import type {
  BillingResult,
  DefaulterRow,
  FeeKind,
  FeeSchedule,
  Invoice,
  Payment,
  PaymentInitiation,
  PaymentMethod,
  Scholarship,
  ScholarshipKind,
} from './types';

// Admin

export function listSchedules(academicYearId: string): Promise<FeeSchedule[]> {
  return api(`/api/fees/schedules?academicYearId=${academicYearId}`);
}

export function createSchedule(input: {
  academicYearId: string;
  name: string;
  kind: FeeKind;
  amount: string;
  gradeLevel?: string;
  trimesterKey?: string;
  periodMonth?: number;
  dueAt: string;
}): Promise<FeeSchedule> {
  return api('/api/fees/schedules', { method: 'POST', body: JSON.stringify(input) });
}

export function runBilling(scheduleId: string): Promise<BillingResult> {
  return api(`/api/fees/schedules/${scheduleId}/run-billing`, { method: 'POST' });
}

export function listScholarships(): Promise<Scholarship[]> {
  return api('/api/fees/scholarships');
}

export function createScholarship(input: {
  studentId: string;
  kind: ScholarshipKind;
  percentage?: string;
  fixedAmount?: string;
  validFrom: string;
  validTo?: string;
  reason?: string;
}): Promise<Scholarship> {
  return api('/api/fees/scholarships', { method: 'POST', body: JSON.stringify(input) });
}

export function invoicesForStudent(studentId: string): Promise<Invoice[]> {
  return api(`/api/fees/invoices?studentId=${studentId}`);
}

export function getInvoice(id: string): Promise<Invoice> {
  return api(`/api/fees/invoices/${id}`);
}

// Payments

export function recordPayment(input: {
  invoiceId: string;
  amount: string;
  method: PaymentMethod;
  externalReference?: string;
  notes?: string;
}): Promise<Payment> {
  return api('/api/payments', { method: 'POST', body: JSON.stringify(input) });
}

export function initiatePayment(input: {
  invoiceId: string;
  method: PaymentMethod;
  payerPhone?: string;
}): Promise<PaymentInitiation> {
  return api('/api/payments/initiate', { method: 'POST', body: JSON.stringify(input) });
}

export function paymentsForInvoice(invoiceId: string): Promise<Payment[]> {
  return api(`/api/payments?invoiceId=${invoiceId}`);
}

export function listDefaulters(): Promise<DefaulterRow[]> {
  return api('/api/payments/defaulters');
}

export function sweepOverdue(): Promise<{ flipped: number }> {
  return api('/api/payments/sweep-overdue', { method: 'POST' });
}

// Guardian portal

export function guardianInvoices(): Promise<Invoice[]> {
  return api('/api/guardian/fees/invoices');
}
