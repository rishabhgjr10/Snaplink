// Site-wide maintenance notice (database migration).
// On by default. To hide it after the move, set VITE_MAINTENANCE_MODE=false in Vercel
// (or change the default below) and redeploy.
export const MAINTENANCE_MODE = import.meta.env.VITE_MAINTENANCE_MODE !== 'false';

export const PORTFOLIO_URL = 'https://rishabhgjr.vercel.app/';
