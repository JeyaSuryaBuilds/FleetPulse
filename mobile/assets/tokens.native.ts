/**
 * FleetPulse tokens for React Native / Expo (Android + iOS).
 * Same values as src/design-system/tokens.js.
 */
export const colors = {
  bg: '#111111',
  sidebar: '#171717',
  card: '#1D1D1D',
  cardRaised: '#232323',
  inset: '#0D0D0D',
  border: '#303030',
  borderStrong: '#3D3D3D',
  orange: '#FF8733',
  orangeDeep: '#F97316',
  orangePressed: '#E5670F',
  onOrange: '#111111',
  text: '#FFFFFF',
  textMuted: '#A3A3A3',
  success: '#22C55E',
  warning: '#FBBF24',
  error: '#EF4444',
} as const;

export const radii = { sm: 6, md: 10, lg: 14, xl: 20 } as const;

export const spacing = { xs: 4, sm: 8, md: 12, lg: 16, xl: 24, xxl: 32 } as const;

/** Use with @expo-google-fonts/inter (Inter_400Regular, Inter_500Medium, Inter_600SemiBold, Inter_700Bold). */
export const fonts = {
  regular: 'Inter_400Regular',
  medium: 'Inter_500Medium',
  semibold: 'Inter_600SemiBold',
  bold: 'Inter_700Bold',
} as const;

/** React Native has no CSS box-shadow; use these style objects. */
export const elevation = {
  card: { shadowColor: '#000', shadowOpacity: 0.35, shadowRadius: 14, shadowOffset: { width: 0, height: 8 }, elevation: 4 },
  raised: { shadowColor: '#000', shadowOpacity: 0.5, shadowRadius: 22, shadowOffset: { width: 0, height: 14 }, elevation: 8 },
} as const;

export const motion = { fast: 140, base: 220, slow: 360 } as const;
