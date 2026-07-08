import { api } from './client';
import type {
  QuestionPayload,
  QuestionResponse,
  QuestionType,
  Quiz,
  QuizAnalytics,
  QuizAttemptRow,
} from './types';

export function listQuizzes(turmaId: string): Promise<Quiz[]> {
  return api(`/api/quizzes?turmaId=${turmaId}`);
}

export function getQuiz(quizId: string): Promise<Quiz> {
  return api(`/api/quizzes/${quizId}`);
}

export function listQuestions(subjectId: string, gradeLevel?: string): Promise<QuestionResponse[]> {
  const q = gradeLevel ? `?subjectId=${subjectId}&gradeLevel=${gradeLevel}` : `?subjectId=${subjectId}`;
  return api(`/api/questions${q}`);
}

export function createQuestion(input: {
  subjectId: string;
  gradeLevel: string;
  prompt: string;
  questionType: QuestionType;
  payload: QuestionPayload;
  points?: number;
}): Promise<QuestionResponse> {
  return api('/api/questions', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function createQuiz(input: {
  subjectId: string;
  turmaId: string;
  academicYearId: string;
  trimesterKey: string;
  title: string;
  instructions?: string;
  timeLimitSeconds?: number;
  randomizeQuestions?: boolean;
  randomizeOptions?: boolean;
  opensAt?: string;
  closesAt?: string;
  questionIds: string[];
}): Promise<Quiz> {
  return api('/api/quizzes', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function publishQuiz(quizId: string): Promise<Quiz> {
  return api(`/api/quizzes/${quizId}/publish`, { method: 'POST' });
}

export function closeQuiz(quizId: string): Promise<Quiz> {
  return api(`/api/quizzes/${quizId}/close`, { method: 'POST' });
}

export function quizAttempts(quizId: string): Promise<QuizAttemptRow[]> {
  return api(`/api/quizzes/${quizId}/attempts`);
}

export function quizAnalytics(quizId: string): Promise<QuizAnalytics> {
  return api(`/api/quizzes/${quizId}/analytics`);
}
