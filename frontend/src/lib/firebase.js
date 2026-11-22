import { initializeApp } from 'firebase/app';
import { getAuth, GoogleAuthProvider, GithubAuthProvider } from 'firebase/auth';

// All values come from the build environment (see .env.example); nothing is hard-coded.
const env = import.meta.env;
const firebaseConfig = {
  apiKey: env.VITE_FIREBASE_API_KEY,
  authDomain: env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: env.VITE_FIREBASE_APP_ID,
};

const REQUIRED = {
  apiKey: 'VITE_FIREBASE_API_KEY',
  authDomain: 'VITE_FIREBASE_AUTH_DOMAIN',
  projectId: 'VITE_FIREBASE_PROJECT_ID',
  appId: 'VITE_FIREBASE_APP_ID',
};
const missing = Object.keys(REQUIRED).filter(key => !firebaseConfig[key]);

if (missing.length) {
  if (env.MODE !== 'test') {
    console.error(`Firebase is not configured. Missing: ${missing.map(k => REQUIRED[k]).join(', ')}`);
  }
  // Placeholders keep public pages working; sign-in fails with a clear Firebase error instead.
  for (const key of missing) firebaseConfig[key] = 'not-configured';
}

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const googleProvider = new GoogleAuthProvider();
export const githubProvider = new GithubAuthProvider();
