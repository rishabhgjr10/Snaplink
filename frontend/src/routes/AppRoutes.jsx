import { lazy, Suspense } from 'react';
import { Routes, Route } from 'react-router-dom';
import { PrivateRoute, AdminRoute } from '../context/AuthContext';
import { LandingPage } from '../pages/LandingPage';
import { NotFoundPage } from '../pages/NotFound';

// Everything except the landing page is code-split, so first-time visitors
// do not download the dashboard, QR tooling or admin screens.
const LoginPage = lazy(() => import('../pages/Login').then(m => ({ default: m.LoginPage })));
const SignupPage = lazy(() => import('../pages/Signup').then(m => ({ default: m.SignupPage })));
const DashboardPage = lazy(() => import('../pages/Dashboard').then(m => ({ default: m.DashboardPage })));
const AdminPage = lazy(() => import('../pages/Admin').then(m => ({ default: m.AdminPage })));
const ReportPage = lazy(() => import('../pages/Report').then(m => ({ default: m.ReportPage })));
const PrivacyPage = lazy(() => import('../pages/Privacy').then(m => ({ default: m.PrivacyPage })));
const TermsPage = lazy(() => import('../pages/Terms').then(m => ({ default: m.TermsPage })));

export function AppRoutes({ toast }) {
  return (
    <Suspense fallback={null}>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage toast={toast} />} />
        <Route path="/signup" element={<SignupPage toast={toast} />} />
        <Route path="/dashboard" element={<PrivateRoute><DashboardPage toast={toast} /></PrivateRoute>} />
        <Route path="/admin" element={<AdminRoute><AdminPage toast={toast} /></AdminRoute>} />
        <Route path="/report" element={<ReportPage />} />
        <Route path="/privacy" element={<PrivacyPage />} />
        <Route path="/terms" element={<TermsPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}
