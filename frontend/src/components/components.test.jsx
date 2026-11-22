import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';

vi.mock('../lib/firebase', () => ({ auth: { currentUser: null } }));
vi.mock('../services/api', async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, updateLink: vi.fn(async () => ({})), deleteLink: vi.fn(async () => {}) };
});

import { updateLink, deleteLink } from '../services/api';
import { ErrorBoundary } from './ErrorBoundary';
import { EditModal } from './EditModal';
import { LinkRow } from './LinkRow';

const entry = { code: 'abc1234', shortUrl: 'https://bauna.xyz/abc1234', originalUrl: 'https://example.com', createdAt: '', clicks: 5, disabled: false };

afterEach(() => { cleanup(); vi.clearAllMocks(); });

describe('ErrorBoundary', () => {
  it('shows a recovery screen instead of a blank page', () => {
    const Boom = () => { throw new Error('boom'); };
    vi.spyOn(console, 'error').mockImplementation(() => {});
    render(<ErrorBoundary><Boom /></ErrorBoundary>);
    expect(screen.getByRole('alert').textContent).toContain('Something went wrong');
  });
});

describe('EditModal', () => {
  it('renders and saves only changed fields', async () => {
    const toast = vi.fn();
    const onSave = vi.fn();
    const onClose = vi.fn();
    render(<EditModal entry={entry} toast={toast} onSave={onSave} onClose={onClose} />);

    expect(screen.getByRole('dialog', { name: 'Edit /abc1234' })).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Destination URL'), { target: { value: 'https://new.example' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() => expect(updateLink).toHaveBeenCalledWith('abc1234', { originalUrl: 'https://new.example' }));
    expect(onSave).toHaveBeenCalled();
    expect(onClose).toHaveBeenCalled();
  });

  it('closes on Escape', () => {
    const onClose = vi.fn();
    render(<EditModal entry={entry} toast={vi.fn()} onSave={vi.fn()} onClose={onClose} />);
    fireEvent.keyDown(document, { key: 'Escape' });
    expect(onClose).toHaveBeenCalled();
  });
});

describe('LinkRow', () => {
  it('asks for confirmation in an accessible dialog before deleting', async () => {
    const onRefresh = vi.fn();
    render(<LinkRow entry={entry} onRefresh={onRefresh} toast={vi.fn()} />);

    fireEvent.click(screen.getByRole('button', { name: 'Delete /abc1234' }));
    expect(deleteLink).not.toHaveBeenCalled();
    expect(screen.getByRole('dialog', { name: 'Delete /abc1234?' })).toBeTruthy();

    fireEvent.click(screen.getByRole('button', { name: 'Delete link' }));
    await waitFor(() => expect(deleteLink).toHaveBeenCalledWith('abc1234'));
    expect(onRefresh).toHaveBeenCalled();
  });

  it('labels icon buttons and never links to localhost', () => {
    render(<LinkRow entry={entry} onRefresh={vi.fn()} toast={vi.fn()} />);
    for (const name of ['Copy /abc1234', 'Edit /abc1234', 'Show analytics for /abc1234']) {
      expect(screen.getByRole('button', { name })).toBeTruthy();
    }
    expect(screen.getByRole('link').getAttribute('href')).toBe('https://bauna.xyz/abc1234');
  });
});
