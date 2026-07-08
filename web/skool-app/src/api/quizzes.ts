import { api } from './client';
import type { AttemptResult, AttemptView, Quiz, SubmitAnswerPayload } from './types';

export function listAvailableQuizzes(turmaId: string): Promise<Quiz[]> {
  return api(`/api/student/quizzes/available?turmaId=${turmaId}`);
}

export function startAttempt(quizId: string): Promise<AttemptView> {
  return api(`/api/student/quizzes/${quizId}/attempts`, { method: 'POST' });
}

export function saveDraft(attemptId: string, answers: SubmitAnswerPayload[], tabSwitchCount: number): Promise<AttemptResult> {
  return api(`/api/student/quizzes/attempts/${attemptId}`, {
    method: 'PUT',
    body: JSON.stringify({ answers, tabSwitchCount }),
  });
}

export function submitAttempt(attemptId: string, answers: SubmitAnswerPayload[], tabSwitchCount: number): Promise<AttemptResult> {
  return api(`/api/student/quizzes/attempts/${attemptId}/submit`, {
    method: 'POST',
    body: JSON.stringify({ answers, tabSwitchCount }),
  });
}
