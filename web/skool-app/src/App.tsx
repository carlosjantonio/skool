import { Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { Layout } from './components/Layout';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { UnauthorizedPage } from './pages/UnauthorizedPage';
import { TeacherClassesPage } from './pages/TeacherClassesPage';
import { TeacherAttendancePage } from './pages/TeacherAttendancePage';
import { TeacherGradesPage } from './pages/TeacherGradesPage';
import { TeacherQuizzesPage } from './pages/TeacherQuizzesPage';
import { TeacherQuizBuilderPage } from './pages/TeacherQuizBuilderPage';
import { TeacherQuizResultsPage } from './pages/TeacherQuizResultsPage';
import { StudentDashboardPage } from './pages/StudentDashboardPage';
import { StudentQuizPage } from './pages/StudentQuizPage';
import { StudentGradesPage } from './pages/StudentGradesPage';
import { StudentAssignmentsPage } from './pages/StudentAssignmentsPage';
import { ForumPage } from './pages/ForumPage';
import { ForumThreadPage } from './pages/ForumThreadPage';
import { AdminFeesPage } from './pages/AdminFeesPage';
import { AdminDefaultersPage } from './pages/AdminDefaultersPage';
import { GuardianInvoicesPage } from './pages/GuardianInvoicesPage';

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/nao-autorizado" element={<UnauthorizedPage />} />
        <Route
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route path="/" element={<DashboardPage />} />
          <Route
            path="/classes"
            element={
              <ProtectedRoute roles={['TEACHER', 'DIRECTOR', 'ADMIN']}>
                <TeacherClassesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/classes/:turmaId/attendance"
            element={
              <ProtectedRoute roles={['TEACHER', 'DIRECTOR', 'ADMIN']}>
                <TeacherAttendancePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/classes/:turmaId/grades"
            element={
              <ProtectedRoute roles={['TEACHER', 'DIRECTOR', 'ADMIN']}>
                <TeacherGradesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/classes/:turmaId/quizzes"
            element={
              <ProtectedRoute roles={['TEACHER', 'DIRECTOR', 'ADMIN']}>
                <TeacherQuizzesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/classes/:turmaId/quizzes/new"
            element={
              <ProtectedRoute roles={['TEACHER', 'DIRECTOR', 'ADMIN']}>
                <TeacherQuizBuilderPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/classes/:turmaId/quizzes/:quizId/results"
            element={
              <ProtectedRoute roles={['TEACHER', 'DIRECTOR', 'ADMIN']}>
                <TeacherQuizResultsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/student"
            element={
              <ProtectedRoute roles={['STUDENT']}>
                <StudentDashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/student/quizzes/:quizId"
            element={
              <ProtectedRoute roles={['STUDENT']}>
                <StudentQuizPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/student/grades"
            element={
              <ProtectedRoute roles={['STUDENT']}>
                <StudentGradesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/student/assignments"
            element={
              <ProtectedRoute roles={['STUDENT']}>
                <StudentAssignmentsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/student/forums/:forumId"
            element={
              <ProtectedRoute roles={['STUDENT', 'TEACHER', 'DIRECTOR', 'ADMIN']}>
                <ForumPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/student/forums/threads/:threadId"
            element={
              <ProtectedRoute roles={['STUDENT', 'TEACHER', 'DIRECTOR', 'ADMIN']}>
                <ForumThreadPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/fees"
            element={
              <ProtectedRoute roles={['ADMIN', 'DIRECTOR', 'SECRETARY']}>
                <AdminFeesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/defaulters"
            element={
              <ProtectedRoute roles={['ADMIN', 'DIRECTOR', 'SECRETARY']}>
                <AdminDefaultersPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/guardian/invoices"
            element={
              <ProtectedRoute roles={['GUARDIAN']}>
                <GuardianInvoicesPage />
              </ProtectedRoute>
            }
          />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
