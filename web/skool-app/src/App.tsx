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
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AuthProvider>
  );
}
