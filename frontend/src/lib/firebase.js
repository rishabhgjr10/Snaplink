import { initializeApp } from 'firebase/app';
import { getAuth, GoogleAuthProvider, GithubAuthProvider } from 'firebase/auth';

const firebaseConfig = {
  apiKey:            import.meta.env.VITE_FIREBASE_API_KEY            || 'AIzaSyC1OWFGWaF20qHJtCsvQzoWgrkY7GY3cj8',
  authDomain:        import.meta.env.VITE_FIREBASE_AUTH_DOMAIN        || 'snapliink.firebaseapp.com',
  projectId:         import.meta.env.VITE_FIREBASE_PROJECT_ID         || 'snapliink',
  storageBucket:     import.meta.env.VITE_FIREBASE_STORAGE_BUCKET     || 'snapliink.firebasestorage.app',
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || '1025901795798',
  appId:             import.meta.env.VITE_FIREBASE_APP_ID             || '1:1025901795798:web:7580c19477188505b9ac77',
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const googleProvider = new GoogleAuthProvider();
export const githubProvider = new GithubAuthProvider();
