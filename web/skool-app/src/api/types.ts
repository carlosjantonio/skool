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
