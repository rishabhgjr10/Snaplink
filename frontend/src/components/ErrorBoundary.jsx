import { Component } from 'react';
import { C } from '../constants/theme';
import { reportError } from '../lib/errorTracking';

/**
 * Catches render errors anywhere below it and shows a recovery screen
 * instead of unmounting the whole app into a blank page.
 */
export class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    console.error('Unhandled UI error', error, info?.componentStack);
    reportError(error, { componentStack: info?.componentStack });
  }

  render() {
    if (!this.state.error) return this.props.children;
    return (
      <div role="alert" style={{
        minHeight: '100vh', display: 'flex', flexDirection: 'column', alignItems: 'center',
        justifyContent: 'center', gap: 16, padding: 24, textAlign: 'center', color: C.text,
      }}>
        <h1 style={{ fontFamily: C.display, fontWeight: 800, fontSize: 28 }}>Something went wrong</h1>
        <p style={{ fontFamily: C.mono, fontSize: 13, color: C.muted, maxWidth: 420, lineHeight: 1.6 }}>
          This page hit an unexpected error. Your links are safe. Try again, or go back to the home page.
        </p>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', justifyContent: 'center' }}>
          <button className="btn-p" onClick={() => this.setState({ error: null })}>Try again</button>
          <button className="btn-g" onClick={() => { window.location.href = '/'; }}>Home</button>
        </div>
      </div>
    );
  }
}
