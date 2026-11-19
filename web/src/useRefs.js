import { useEffect, useState } from 'react';
import { api } from './api.js';

const cache = new Map();
/** Loads up to 100 rows of a referenced resource for select boxes and label lookup (short-lived in-memory cache). */
export function useRefOptions(path, labelKey) {
  const [rows, setRows] = useState(() => cache.get(path)?.rows || null);
  useEffect(() => {
    if (!path) return;
    const hit = cache.get(path);
    if (hit && Date.now() - hit.at < 30000) { setRows(hit.rows); return; }
    api.get(`/${path}`, { params: { size: 100, sort: 'id,desc' } })
      .then((r) => { cache.set(path, { rows: r.data.content, at: Date.now() }); setRows(r.data.content); })
      .catch(() => setRows([]));
  }, [path]);
  const label = (id) => { const r = rows?.find((x) => x.id === id); return r ? r[labelKey] : id; };
  return { rows, label };
}
export const clearRefCache = () => cache.clear();
