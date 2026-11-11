import { useSyncExternalStore } from 'react';
import { API } from '../services/api';

// Render free instances sleep after inactivity and take ~30–60s to cold-start.
// This module pings /api/v1/health once (shared across the whole app) and keeps
// retrying until the backend answers, exposing the status to any component.

// Keep wake-up traffic gentle: Render answers 429 ("hibernate-rate-limited")
// when a sleeping service receives too many wake attempts, so retries back off.
const ATTEMPT_TIMEOUT_MS = 20000;   // abort a single ping after this long (one ping is usually enough to wake Render)
const RETRY_DELAYS_MS = [4000, 8000, 15000, 30000]; // successive waits; the last one repeats
const RATE_LIMITED_DELAY_MS = 60000; // wait this long after a 429 from Render
const MAX_RATE_LIMITED = 2;          // give up after this many 429s in a row
const GIVE_UP_AFTER_MS = 180000;    // stop retrying after 3 minutes
const SLOW_THRESHOLD_MS = 1500;     // faster than this = server was already awake

let state = {
  status: 'idle',      // idle | checking | waking | up | down
  reason: null,        // why it's down: 'rate-limited' | 'timeout' | null
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

/** Returns the HTTP status, or 0 on network error/timeout. */
async function pingOnce() {
  const ctrl = new AbortController();
  const t = setTimeout(() => ctrl.abort(), ATTEMPT_TIMEOUT_MS);
  try {
    const r = await fetch(`${API}/api/v1/health`, { cache: 'no-store', signal: ctrl.signal });
    return r.status;
  } catch {
    return 0;
  } finally {
    clearTimeout(t);
  }
}

const sleep = ms => new Promise(r => setTimeout(r, ms));

async function probeLoop() {
  const startedAt = Date.now();
  setState({ status: 'checking', reason: null, startedAt, readyAt: null, wasCold: false });

  // Flip to "waking" if the first ping is slow, so fast responses never flash a banner.
  const slowTimer = setTimeout(() => {
    if (state.status === 'checking') setState({ status: 'waking', wasCold: true });
  }, SLOW_THRESHOLD_MS);

  try {
    let attempt = 0;
    let rateLimited = 0;
    while (Date.now() - startedAt < GIVE_UP_AFTER_MS) {
      const status = await pingOnce();
      if (status >= 200 && status < 300) {
        setState({ status: 'up', reason: null, readyAt: Date.now() });
        return;
      }
      if (state.status === 'checking') setState({ status: 'waking', wasCold: true });

      let delay;
      if (status === 429) {
        // Render is refusing wake-ups; more pings would only extend the block.
        if (++rateLimited >= MAX_RATE_LIMITED) {
          setState({ status: 'down', reason: 'rate-limited' });
          return;
        }
        delay = RATE_LIMITED_DELAY_MS;
      } else {
        rateLimited = 0;
        delay = RETRY_DELAYS_MS[Math.min(attempt, RETRY_DELAYS_MS.length - 1)];
      }
      attempt++;
      if (Date.now() - startedAt + delay >= GIVE_UP_AFTER_MS) break;
      await sleep(delay);
    }
    setState({ status: 'down', reason: 'timeout' });
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
