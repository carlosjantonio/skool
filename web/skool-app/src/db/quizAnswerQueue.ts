import { v5 as uuidv5 } from 'uuid';
import { saveDraft, submitAttempt } from '../api/quizzes';
import type { AttemptResult, SubmitAnswerPayload } from '../api/types';
import { offlineDb, type PendingQuizAnswer } from './offlineDb';

const NAMESPACE = '6ba7b810-9dad-11d1-80b4-00c04fd430c8';

/**
 * Deterministic answer id — same (attempt, question) → same UUID, so a retried
 * enqueue after a network blip updates the same DB row rather than creating a
 * duplicate.
 */
export function stableAnswerId(attemptId: string, questionId: string): string {
  return uuidv5(`quiz-answer:${attemptId}:${questionId}`, NAMESPACE);
}

export async function upsertAnswer(attemptId: string, questionId: string, response: unknown, final = false): Promise<PendingQuizAnswer> {
  const id = stableAnswerId(attemptId, questionId);
  const row: PendingQuizAnswer = {
    id,
    attemptId,
    questionId,
    response: JSON.stringify(response),
    queuedAt: Date.now(),
    final: final ? 1 : 0,
  };
  await offlineDb.quizAnswers.put(row);
  return row;
}

export async function pendingForAttempt(attemptId: string): Promise<PendingQuizAnswer[]> {
  return offlineDb.quizAnswers.where({ attemptId }).toArray();
}

export async function countPending(): Promise<number> {
  return offlineDb.quizAnswers.count();
}

async function clearForAttempt(attemptId: string): Promise<void> {
  const rows = await pendingForAttempt(attemptId);
  await offlineDb.quizAnswers.bulkDelete(rows.map((r) => r.id));
}

function toPayload(rows: PendingQuizAnswer[]): SubmitAnswerPayload[] {
  return rows.map((r) => ({
    id: r.id,
    questionId: r.questionId,
    response: JSON.parse(r.response) as Record<string, unknown>,
  }));
}

export async function flushDraft(attemptId: string, tabSwitchCount: number): Promise<AttemptResult | null> {
  const rows = await pendingForAttempt(attemptId);
  if (rows.length === 0) return null;
  const result = await saveDraft(attemptId, toPayload(rows), tabSwitchCount);
  await clearForAttempt(attemptId);
  return result;
}

export async function finalizeAttempt(attemptId: string, tabSwitchCount: number): Promise<AttemptResult> {
  const rows = await pendingForAttempt(attemptId);
  const result = await submitAttempt(attemptId, toPayload(rows), tabSwitchCount);
  await clearForAttempt(attemptId);
  return result;
}
