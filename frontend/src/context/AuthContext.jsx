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
import { getMe } from '../services/api';

export const AuthContext = createContext(null);

// The Firebase SDK keeps the session (in IndexedDB) and refreshes ID tokens itself.
// Nothing auth-related is stored in localStorage, and API calls fetch a fresh token
// per request (see services/api.ts), so no stale or tamperable copy exists.
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true); // true until Firebase restores the session

  useEffect(() => {
    // Clean up values written by earlier versions of the app.
    try {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    } catch { /* storage unavailable */ }

    return onAuthStateChanged(auth, (firebaseUser) => {
      setUser(firebaseUser ? {
        uid: firebaseUser.uid,
        email: firebaseUser.email,
        displayName: firebaseUser.displayName,
        photoURL: firebaseUser.photoURL,
      } : null);
      setLoading(false);
    });
  }, []);

  const loginWithEmail = useCallback(
    async (email, password) => (await signInWithEmailAndPassword(auth, email, password)).user, []);
  const signupWithEmail = useCallback(
    async (email, password) => (await createUserWithEmailAndPassword(auth, email, password)).user, []);
  const loginWithGoogle = useCallback(async () => (await signInWithPopup(auth, googleProvider)).user, []);
  const loginWithGithub = useCallback(async () => (await signInWithPopup(auth, githubProvider)).user, []);
  const logout = useCallback(() => signOut(auth), []);

  return (
    <AuthContext.Provider value={{
      user, loading, isAuthLoading: loading,
      logout, loginWithEmail, signupWithEmail, loginWithGoogle, loginWithGithub,
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() { return useContext(AuthContext); }

export function PrivateRoute({ children }) {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) return null; // wait for Firebase to restore the session
  if (!user) return <Navigate to="/login" state={{ from: location }} replace />;
  return children;
}

/** Admin access is decided by the backend (/api/v1/me), never by client-side state. */
export function AdminRoute({ children }) {
  const { user, loading } = useAuth();
  const location = useLocation();
  const [admin, setAdmin] = useState(null); // null = checking

  useEffect(() => {
    if (!user) return;
    let cancelled = false;
    getMe().then(me => !cancelled && setAdmin(me.admin)).catch(() => !cancelled && setAdmin(false));
    return () => { cancelled = true; };
  }, [user]);

  if (loading) return null;
  if (!user) return <Navigate to="/login" state={{ from: location }} replace />;
  if (admin === null) return null;
  if (!admin) return <Navigate to="/dashboard" replace />;
  return children;
}
