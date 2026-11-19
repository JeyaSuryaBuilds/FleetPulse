import { useCallback, useEffect, useState } from 'react';
import { api, errorMessage } from '../../api.js';

export function useGet(path, params) {
  const [s, setS] = useState({ data: null, error: '', loading: true });
  const key = JSON.stringify(params || {});
  const reload = useCallback(() => {
    setS((x) => ({ ...x, loading: true }));
    api.get(path, { params }).then((r) => setS({ data: r.data, error: '', loading: false })).catch((e) => setS({ data: null, error: errorMessage(e), loading: false }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [path, key]);
  useEffect(() => { reload(); }, [reload]);
  return { ...s, reload };
}
