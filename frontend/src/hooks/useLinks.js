import { useCallback, useEffect, useRef, useState } from 'react';
import { getSummary, listLinks } from '../services/api';

const PAGE_SIZE = 25;

/**
 * The signed-in user's links, loaded a page at a time and searched on the server.
 * Keeps a separate error state, so a failed load is never shown as "no links".
 */
export function useLinks() {
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [summary, setSummary] = useState(null);
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('all');   // all | active | scheduled | expired
  const [sort, setSort] = useState('newest');    // newest | expiring
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState('');
  const requestId = useRef(0);

  const loadFirstPage = useCallback(async (q, statusFilter, sortOrder) => {
    const id = ++requestId.current; // ignore responses to superseded searches
    setLoading(true);
    setError('');
    try {
      const [result, totals] = await Promise.all([listLinks({ page: 0, size: PAGE_SIZE, q, status: statusFilter, sort: sortOrder }), getSummary()]);
      if (id !== requestId.current) return;
      setItems(result.items);
      setPage(0);
      setTotalItems(result.totalItems);
      setTotalPages(result.totalPages);
      setSummary(totals);
    } catch (err) {
      if (id === requestId.current) setError(err.message || 'Could not load your links.');
    } finally {
      if (id === requestId.current) setLoading(false);
    }
  }, []);

  // Debounced server-side search; filter and sort changes apply immediately.
  useEffect(() => {
    const t = setTimeout(() => loadFirstPage(query.trim(), status, sort), query ? 300 : 0);
    return () => clearTimeout(t);
  }, [query, status, sort, loadFirstPage]);

  const reload = useCallback(() => loadFirstPage(query.trim(), status, sort), [loadFirstPage, query, status, sort]);

  const loadMore = useCallback(async () => {
    if (loadingMore || page + 1 >= totalPages) return;
    setLoadingMore(true);
    try {
      const result = await listLinks({ page: page + 1, size: PAGE_SIZE, q: query.trim(), status, sort });
      setItems(prev => [...prev, ...result.items.filter(n => !prev.some(p => p.code === n.code))]);
      setPage(result.page);
      setTotalPages(result.totalPages);
      setTotalItems(result.totalItems);
    } catch (err) {
      setError(err.message || 'Could not load more links.');
    } finally {
      setLoadingMore(false);
    }
  }, [loadingMore, page, totalPages, query, status, sort]);

  return {
    items, totalItems, summary, query, setQuery, status, setStatus, sort, setSort, loading, loadingMore, error,
    hasMore: page + 1 < totalPages, reload, loadMore,
  };
}
