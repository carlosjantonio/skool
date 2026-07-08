export type Role = 'ADMIN' | 'DIRECTOR' | 'SECRETARY' | 'TEACHER' | 'STUDENT' | 'GUARDIAN' | 'MINISTRY';

export interface TokenResponse {
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
  userId: string;
  tenantId: string;
  email: string;
  fullName: string;
  roles: Role[];
}

export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  timestamp?: string;
  errors?: { field: string; message: string }[];
}

export interface School {
  id: string;
  name: string;
  code: string;
  municipioId: string;
  comunaOuBairro: string | null;
  addressLine1: string | null;
  addressComplement: string | null;
  active: boolean;
}

export interface Trimester {
  id: string;
  key: 'T1' | 'T2' | 'T3';
  startDate: string;
  endDate: string;
}

export interface AcademicYear {
  id: string;
  name: string;
  startDate: string;
  endDate: string;
  current: boolean;
  trimesters: Trimester[];
}

export interface Subject {
  id: string;
  name: string;
  code: string;
  gradeLevel: string;
}

export interface Turma {
  id: string;
  schoolId: string;
  academicYearId: string;
  name: string;
  gradeLevel: string;
  track: string;
  capacity: number;
}

export interface Student {
  id: string;
  fullName: string;
  dateOfBirth: string;
  sex: 'M' | 'F';
  bi: string | null;
  active: boolean;
}

export interface Enrollment {
  id: string;
  studentId: string;
  academicYearId: string;
  turmaId: string;
  status: 'PENDING' | 'ENROLLED' | 'WITHDRAWN' | 'GRADUATED';
  enrolledAt: string | null;
  withdrawnAt: string | null;
}

export interface StaffSummary {
  id: string;
  fullName: string;
  bi: string | null;
  nif: string | null;
  phone: string | null;
  email: string | null;
  qualification: string | null;
  hireDate: string | null;
  userId: string | null;
  active: boolean;
}

export type AssignmentRole = 'TEACHER' | 'HEAD_TEACHER' | 'ASSISTANT';

export interface StaffAssignment {
  id: string;
  staffId: string;
  turmaId: string;
  subjectId: string | null;
  academicYearId: string;
  role: AssignmentRole;
}

export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED';

export interface AttendanceRecord {
  id: string;
  turmaId: string;
  studentId: string;
  date: string;
  status: AttendanceStatus;
  notes: string | null;
  recordedAt: string;
}

export interface AttendanceEntry {
  id: string;
  turmaId: string;
  studentId: string;
  date: string;
  status: AttendanceStatus;
  notes?: string | null;
}

export interface AttendanceBatchResult {
  accepted: string[];
  skippedDuplicates: string[];
  failed: { id: string; reason: string }[];
}

export type GradeCategory = 'TEST' | 'EXAM' | 'ASSIGNMENT' | 'PARTICIPATION' | 'OTHER';

export interface GradeEntry {
  studentId: string;
  subjectId: string;
  turmaId: string;
  academicYearId: string;
  trimesterKey: 'T1' | 'T2' | 'T3';
  value: string;
  weight: string;
  category: GradeCategory;
  notes?: string;
}

export interface GradeResponse {
  id: string;
  studentId: string;
  subjectId: string;
  turmaId: string;
  trimesterKey: string;
  value: string;
  weight: string;
  category: GradeCategory;
  notes: string | null;
  recordedAt: string;
}

export interface StudentGradeSummary {
  studentId: string;
  studentName: string;
  academicYearId: string;
  academicYearName: string;
  subjectAverages: {
    trimesterKey: string;
    subjectId: string;
    subjectName: string;
    average: string;
  }[];
  trimesterAverages: Record<string, string>;
  finalAverage: string;
}

// ---- Phase 4: Student portal ----

export interface StudentSelf {
  studentId: string;
  fullName: string;
  dateOfBirth: string | null;
  sex: string;
  turmaId: string | null;
  academicYearId: string | null;
  enrolledAt: string | null;
}

export type QuestionType = 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'SHORT_ANSWER' | 'ESSAY';
export type QuizStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED';
export type AttemptStatus = 'IN_PROGRESS' | 'SUBMITTED' | 'GRADED';

export interface QuestionPayload {
  options?: { key: string; text: string; correct?: boolean }[];
  correct?: boolean;
  acceptedAnswers?: string[];
  rubric?: string;
}

export interface Quiz {
  id: string;
  subjectId: string;
  turmaId: string;
  academicYearId: string;
  trimesterKey: string;
  title: string;
  instructions: string | null;
  timeLimitSeconds: number | null;
  randomizeQuestions: boolean;
  randomizeOptions: boolean;
  status: QuizStatus;
  opensAt: string | null;
  closesAt: string | null;
  publishedAt: string | null;
  questionCount: number;
}

export interface QuestionResponse {
  id: string;
  subjectId: string;
  gradeLevel: string;
  prompt: string;
  questionType: QuestionType;
  payload: QuestionPayload;
  points: string;
  createdAt: string;
}

export interface QuizAnalytics {
  quizId: string;
  totalAttempts: number;
  submittedAttempts: number;
  averageAutoScore: string;
  averageTotalPoints: string;
  questionStats: {
    questionId: string;
    prompt: string;
    answeredCount: number;
    correctCount: number;
    correctRate: string;
  }[];
}

export interface QuizAttemptRow {
  attemptId: string;
  studentId: string;
  status: AttemptStatus;
  startedAt: string;
  submittedAt: string | null;
  autoScore: string | null;
  manualScore: string | null;
  totalPoints: string | null;
  tabSwitchCount: number;
}

export interface StudentQuestion {
  id: string;
  prompt: string;
  questionType: QuestionType;
  payload: QuestionPayload;
  points: string;
}

export interface AttemptView {
  attemptId: string;
  quizId: string;
  title: string;
  instructions: string | null;
  timeLimitSeconds: number | null;
  startedAt: string;
  deadlineAt: string | null;
  status: AttemptStatus;
  questions: StudentQuestion[];
  savedAnswers: { questionId: string; response: Record<string, unknown> }[];
}

export interface AttemptResult {
  attemptId: string;
  status: AttemptStatus;
  autoScore: string | null;
  manualScore: string | null;
  totalPoints: string | null;
  needsManualGrading: boolean;
}

export interface SubmitAnswerPayload {
  id: string;
  questionId: string;
  response: Record<string, unknown>;
}

export interface Assignment {
  id: string;
  subjectId: string;
  turmaId: string;
  academicYearId: string;
  trimesterKey: string;
  title: string;
  description: string | null;
  rubric: string | null;
  maxScore: string;
  dueAt: string;
  allowLate: boolean;
  status: 'DRAFT' | 'OPEN' | 'CLOSED';
  createdAt: string;
  submissionCount: number;
}

export interface AssignmentSubmission {
  id: string;
  assignmentId: string;
  studentId: string;
  documentId: string | null;
  notes: string | null;
  submittedAt: string;
  isLate: boolean;
  score: string | null;
  feedback: string | null;
  gradedAt: string | null;
  status: 'SUBMITTED' | 'GRADED' | 'RETURNED';
  studentName: string | null;
}

export interface Forum {
  id: string;
  subjectId: string;
  turmaId: string;
  academicYearId: string;
  title: string;
  description: string | null;
  createdAt: string;
}

export interface ForumThread {
  id: string;
  forumId: string;
  title: string;
  body: string;
  authorId: string;
  authorName: string;
  pinned: boolean;
  hidden: boolean;
  upvoteCount: number;
  replyCount: number;
  lastActivityAt: string;
  createdAt: string;
}

export interface ForumPost {
  id: string;
  threadId: string;
  body: string;
  authorId: string;
  authorName: string;
  authorRole: string;
  upvoteCount: number;
  hidden: boolean;
  markedVerified: boolean;
  createdAt: string;
}

export type BoardEntryKind = 'ANNOUNCEMENT' | 'MATERIAL' | 'LINK';

export interface BoardEntry {
  id: string;
  subjectId: string;
  turmaId: string;
  academicYearId: string;
  kind: BoardEntryKind;
  title: string;
  body: string | null;
  documentId: string | null;
  externalUrl: string | null;
  dueAt: string | null;
  pinned: boolean;
  lowBandwidth: boolean;
  authorId: string | null;
  authorName: string | null;
  publishedAt: string;
}
