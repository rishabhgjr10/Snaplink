import { Link } from 'react-router-dom';
import { C } from '../constants/theme';
import { Logo } from '../components/Logo';
import { Footer } from '../layouts/MainLayout';

const SECTIONS = [
  { title: '1. Who we are', body: 'SnapLink is a URL shortening and link management service available at bauna.xyz. By using the service you agree to this policy.' },
  { title: '2. Information we collect', body: 'Account data: your email address and a user identifier, managed by Google Firebase Authentication. We never see or store your password. Link data: the destination URL you submit, the short code, the creation time and which account created it. Click analytics: for each click on a short link we store the time, the device type (desktop, mobile or tablet, derived from the browser user agent), the referring site (domain only, never the full page address) and a shortened IP address with the last part removed (for example 203.0.113.0). We do not record clicks from known bots and link-preview services. Abuse reports: the reported link, the reason, any details you write and, if you are signed in, your account identifier.' },
  { title: '3. Signing in with Google or GitHub', body: 'If you sign in with Google or GitHub, Firebase Authentication receives your email address and an account identifier from that provider. We do not receive your provider password, contacts or other data.' },
  { title: '4. How we use your data', body: 'Your email identifies your account. Link analytics are shown only to the owner of the link. Shortened IP addresses and abuse reports are used to prevent abuse. We do not sell, share or transfer your data to third parties except the service providers listed below, or as required by law.' },
  { title: '5. Where data is stored', body: 'Accounts are managed by Google Firebase. Links, analytics and reports are stored in a managed MySQL database; short-link lookups may be cached in Redis. The backend runs on Render and the website on Vercel. All connections use HTTPS. API requests are authenticated with short-lived Firebase ID tokens.' },
  { title: '6. Data retention', body: 'Click analytics are deleted automatically after 365 days. Links are kept until you delete them; deleting a link also deletes its analytics immediately. You may ask for your account and all links you created to be deleted at any time by emailing dhakad.kumawat18@gmail.com; we will complete the deletion within 30 days.' },
  { title: '7. Cookies and local storage', body: 'We do not use tracking cookies. Firebase keeps your sign-in session in your browser (IndexedDB) so you stay signed in. We do not track visitors who click short links across sites.' },
  { title: '8. Children', body: 'The service is not directed at children under 13. We do not knowingly collect data from children.' },
  { title: '9. Changes to this policy', body: 'We may update this policy. The date at the top of this page shows the latest revision. Continued use of the service after a change constitutes acceptance.' },
  { title: '10. Contact', body: 'Questions or requests regarding your data: dhakad.kumawat18@gmail.com' },
];

export function PrivacyPage() {
  return (
    <div style={{ minHeight: '100vh', color: C.text, display: 'flex', flexDirection: 'column' }}>
      <nav style={{ padding: '0 32px', height: 64, display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderBottom: `1px solid ${C.border}` }}>
        <Link to="/" style={{ textDecoration: 'none' }}><Logo size="sm" /></Link>
        <Link to="/" style={{ fontFamily: C.mono, fontSize: 13, color: C.muted, textDecoration: 'none' }}>← back</Link>
      </nav>
      <main style={{ flex: 1, maxWidth: 760, margin: '0 auto', padding: '64px 24px 80px', width: '100%' }}>
        <h1 style={{ fontFamily: C.display, fontStyle: 'italic', fontSize: 48, color: C.text, marginBottom: 8 }}>Privacy Policy</h1>
        <p style={{ fontFamily: C.mono, fontSize: 12, color: C.muted, marginBottom: 48 }}>
          Last updated: 22 November 2026
        </p>
        {SECTIONS.map(({ title, body }) => (
          <div key={title} style={{ marginBottom: 40 }}>
            <h2 style={{ fontFamily: C.mono, fontSize: 14, color: C.accent, marginBottom: 10, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' }}>{title}</h2>
            <p style={{ fontFamily: C.space, fontSize: 15, color: C.text, lineHeight: 1.8 }}>{body}</p>
          </div>
        ))}
      </main>
      <Footer />
    </div>
  );
}
