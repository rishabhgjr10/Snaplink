import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import {
  onAuthStateChanged,
  signInWithPopup,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut,
} from 'firebase/auth';
import { auth, googleProvider, githubProvider } from '../lib/firebase';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [loading, setLoading] = useState(true); // true while Firebase resolves initial state

  // Listen for Firebase auth state changes (persisted across refreshes)
  useEffect(() => {
    const unsub = onAuthStateChanged(auth, async (firebaseUser) => {
      if (firebaseUser) {
        const idToken = await firebaseUser.getIdToken();
        setToken(idToken);
        localStorage.setItem('token', idToken);
        setUser({
          email: firebaseUser.email,
          displayName: firebaseUser.displayName,
          photoURL: firebaseUser.photoURL,
          uid: firebaseUser.uid,
          role: 'user',
        });
      } else {
        setToken(null);
        setUser(null);
        localStorage.removeItem('token');
      }
      setLoading(false);
    });
    return unsub;
  }, []);

  // ── Email / Password ───────────────────────────────────────────────────────
  const loginWithEmail = useCallback(async (email, password) => {
    const cred = await signInWithEmailAndPassword(auth, email, password);
    return cred.user;
  }, []);

  const signupWithEmail = useCallback(async (email, password) => {
    const cred = await createUserWithEmailAndPassword(auth, email, password);
    return cred.user;
  }, []);

  // ── OAuth providers ────────────────────────────────────────────────────────
  const loginWithGoogle = useCallback(async () => {
    const cred = await signInWithPopup(auth, googleProvider);
    return cred.user;
  }, []);

  const loginWithGithub = useCallback(async () => {
    const cred = await signInWithPopup(auth, githubProvider);
    return cred.user;
  }, []);

  // ── Logout ─────────────────────────────────────────────────────────────────
  const logout = useCallback(async () => {
    await signOut(auth);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  }, []);

  // Legacy compat — some components call login(token, userObj) directly.
  const login = useCallback((t, u) => {
    localStorage.setItem('token', t);
    setToken(t);
    setUser(u);
  }, []);

  return (
    <AuthContext.Provider value={{
      token, user, loading,
      login, logout,
      loginWithEmail, signupWithEmail,
      loginWithGoogle, loginWithGithub,
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() { return useContext(AuthContext); }

export function PrivateRoute({ children }) {
  const { token, loading } = useAuth();
  const location = useLocation();
  if (loading) return null; // wait for Firebase to resolve
  if (!token) return <Navigate to="/login" state={{ from: location }} replace />;
  return children;
}

export function AdminRoute({ children }) {
  const { token, user, loading } = useAuth();
  const location = useLocation();
  if (loading) return null;
  if (!token) return <Navigate to="/login" state={{ from: location }} replace />;
  if (user?.role !== 'admin') return <Navigate to="/dashboard" replace />;
  return children;
}
