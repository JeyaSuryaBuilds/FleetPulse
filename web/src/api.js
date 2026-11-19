import axios from 'axios';

const KEY = 'fp.session';
const ADMIN_KEY = 'fp.admin';

export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || '/api/v1' });

export const session = {
  get: () => { try { return JSON.parse(localStorage.getItem(KEY)); } catch { return null; } },
  set: (s) => localStorage.setItem(KEY, JSON.stringify(s)),
  clear: () => localStorage.removeItem(KEY),
};
export const adminSession = {
  get: () => { try { return JSON.parse(sessionStorage.getItem(ADMIN_KEY)); } catch { return null; } },
  set: (s) => sessionStorage.setItem(ADMIN_KEY, JSON.stringify(s)),
  clear: () => sessionStorage.removeItem(ADMIN_KEY),
};

api.interceptors.request.use((cfg) => {
  const admin = cfg.url?.startsWith('/admin');
  const s = admin ? adminSession.get() : session.get();
  if (s?.accessToken) cfg.headers.Authorization = `Bearer ${s.accessToken}`;
  return cfg;
});

let refreshing = null;
api.interceptors.response.use(
  (r) => r,
  async (err) => {
    const orig = err.config;
    const s = session.get();
    if (err.response?.status === 401 && orig.url?.startsWith('/admin') && !orig.url.startsWith('/admin/auth/login')) {
      adminSession.clear(); window.location.assign('/admin'); return Promise.reject(err);
    }
    if (err.response?.status === 401 && s?.refreshToken && !orig._retry && !orig.url?.startsWith('/admin') && !orig.url?.startsWith('/auth/')) {
      orig._retry = true;
      refreshing ||= api.post('/auth/refresh', { refreshToken: s.refreshToken }).finally(() => { refreshing = null; });
      try {
        const { data } = await refreshing;
        session.set(data);
        orig.headers.Authorization = `Bearer ${data.accessToken}`;
        return api(orig);
      } catch { session.clear(); window.location.assign('/login'); }
    }
    return Promise.reject(err);
  },
);

export const errorMessage = (e) => e?.response?.data?.message || e?.message || 'Something went wrong';
