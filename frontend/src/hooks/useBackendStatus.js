import { useSyncExternalStore } from 'react';
import { API } from '../services/api';

// Render free instances sleep after inactivity and take ~30–60s to cold-start.
// This module pings /api/v1/health once (shared across the whole app) and keeps
// retrying until the backend answers, exposing the status to any component.

const ATTEMPT_TIMEOUT_MS = 10000;   // abort a single ping after this long
const RETRY_DELAY_MS = 2500;        // wait between failed pings
const GIVE_UP_AFTER_MS = 180000;    // stop retrying after 3 minutes
const SLOW_THRESHOLD_MS = 1500;     // faster than this = server was already awake

let state = {
  status: 'idle',      // idle | checking | waking | up | down
  startedAt: null,
  readyAt: null,
  wasCold: false,      // true if the user had to wait for a cold start
};
const listeners = new Set();
let running = null;

function setState(patch) {
  state = { ...state, ...patch };
  listeners.forEach(l => l());
}

async function pingOnce() {
  const ctrl = new AbortController();
  const t = setTimeout(() => ctrl.abort(), ATTEMPT_TIMEOUT_MS);
  try {
    const r = await fetch(`${API}/api/v1/health`, { cache: 'no-store', signal: ctrl.signal });
    return r.ok;
  } catch {
    return false;
  } finally {
    clearTimeout(t);
  }
}

async function probeLoop() {
  const startedAt = Date.now();
  setState({ status: 'checking', startedAt, readyAt: null, wasCold: false });

  // Flip to "waking" if the first ping is slow, so fast responses never flash a banner.
  const slowTimer = setTimeout(() => {
    if (state.status === 'checking') setState({ status: 'waking', wasCold: true });
  }, SLOW_THRESHOLD_MS);

  try {
    while (Date.now() - startedAt < GIVE_UP_AFTER_MS) {
      if (await pingOnce()) {
        setState({ status: 'up', readyAt: Date.now() });
        return;
      }
      if (state.status === 'checking') setState({ status: 'waking', wasCold: true });
      await new Promise(r => setTimeout(r, RETRY_DELAY_MS));
    }
    setState({ status: 'down' });
  } finally {
    clearTimeout(slowTimer);
    running = null;
  }
}

/** Start waking the backend (idempotent). Safe to call from any page. */
export function wakeBackend() {
  if (state.status === 'up' || running) return running;
  running = probeLoop();
  return running;
}

/** Resolves true once the backend is up, false if it gave up. */
export async function waitForBackend() {
  await wakeBackend();
  return state.status === 'up';
}

function subscribe(l) { listeners.add(l); return () => listeners.delete(l); }
function getSnapshot() { return state; }

export function useBackendStatus() {
  return useSyncExternalStore(subscribe, getSnapshot);
}
