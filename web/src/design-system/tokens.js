/**
 * FleetPulse design tokens — single source of truth for JavaScript consumers.
 * src/styles/tokens.css mirrors these values for CSS. If you change one, change the other.
 */
export const colors = {
  bg: '#111111',
  sidebar: '#171717',
  card: '#1D1D1D',
  cardRaised: '#232323',
  inset: '#0D0D0D',
  border: '#303030',
  borderStrong: '#3D3D3D',
  orange: '#FF8733',      // primary
  orangeDeep: '#F97316',  // secondary
  orangePressed: '#E5670F',
  text: '#FFFFFF',
  textMuted: '#A3A3A3',
  onOrange: '#111111',    // text on orange fills: white on #FF8733 is only ~2.4:1, #111 is ~8:1
  success: '#22C55E',
  warning: '#FBBF24',
  error: '#EF4444',
};

export const lightColors = {
  bg: '#F4F4F5',
  sidebar: '#FFFFFF',
  card: '#FFFFFF',
  border: '#E4E4E7',
  text: '#171717',
  textMuted: '#52525B',
  orange: '#C2570C', // darker orange keeps text/link contrast on white (~4.9:1)
};

export const radii = { sm: '6px', md: '10px', lg: '14px', xl: '20px' };

export const shadows = {
  card: '0 1px 0 rgba(255,255,255,0.04) inset, 0 10px 28px -14px rgba(0,0,0,0.65)',
  raised: '0 1px 0 rgba(255,255,255,0.06) inset, 0 18px 40px -16px rgba(0,0,0,0.75)',
  overlay: '0 24px 64px -20px rgba(0,0,0,0.85)',
};

export const motion = {
  ease: [0.2, 0.7, 0.2, 1],
  easeCss: 'cubic-bezier(0.2, 0.7, 0.2, 1)',
  fast: 0.14,
  base: 0.22,
  slow: 0.36,
};

export const fontFamily = {
  sans: ['Inter Variable', 'Inter', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
  mono: ['ui-monospace', 'SFMono-Regular', 'Menlo', 'Consolas', 'monospace'],
};
