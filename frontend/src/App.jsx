import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { useToast } from './hooks/useToast';
import { ToastStack } from './components/ToastStack';
import { AppRoutes } from './routes/AppRoutes';
import { GrainBg } from './components/GrainBg';
import { ErrorBoundary } from './components/ErrorBoundary';

export default function App() {
  const { toasts, show: toast } = useToast();
  return (
    <BrowserRouter>
      <GrainBg />
      <ErrorBoundary>
        <AuthProvider>
          <AppRoutes toast={toast} />
          <ToastStack toasts={toasts} />
        </AuthProvider>
      </ErrorBoundary>
    </BrowserRouter>
  );
}
