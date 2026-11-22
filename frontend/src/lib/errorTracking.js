// Error tracking is opt-in: nothing is loaded unless VITE_SENTRY_DSN is set at build time.
// The Sentry SDK is imported lazily so it never adds to the main bundle when unused.
let sentry = null;

export function initErrorTracking() {
  const dsn = import.meta.env.VITE_SENTRY_DSN;
  if (!dsn) return;
  import('@sentry/react')
    .then(Sentry => {
      Sentry.init({
        dsn,
        environment: import.meta.env.MODE,
        tracesSampleRate: 0,
        sendDefaultPii: false,
      });
      sentry = Sentry;
    })
    .catch(() => { /* tracking unavailable; the app keeps working */ });
}

export function reportError(error, context) {
  if (sentry) sentry.captureException(error, context ? { extra: context } : undefined);
}
