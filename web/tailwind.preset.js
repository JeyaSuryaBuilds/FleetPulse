/**
 * FleetPulse Tailwind preset (Tailwind CSS v3).
 * Usage in tailwind.config.js:
 *   import fleetpulse from './tailwind.preset.js';
 *   export default { presets: [fleetpulse], content: ['./index.html', './src/**\/*.{js,jsx,ts,tsx}'] };
 * (Tailwind v4 users: see src/styles/tailwind-v4-theme.css.)
 */
import { colors, radii, shadows, fontFamily, motion } from './src/design-system/tokens.js';

export default {
  darkMode: ['selector', '[data-theme="dark"]'],
  theme: {
    extend: {
      colors: {
        fp: {
          bg: colors.bg,
          sidebar: colors.sidebar,
          card: colors.card,
          raised: colors.cardRaised,
          inset: colors.inset,
          border: colors.border,
          'border-strong': colors.borderStrong,
          text: colors.text,
          muted: colors.textMuted,
          'on-orange': colors.onOrange,
        },
        orange: {
          DEFAULT: colors.orange,
          deep: colors.orangeDeep,
          pressed: colors.orangePressed,
        },
        success: colors.success,
        warning: colors.warning,
        danger: colors.error,
      },
      fontFamily: { sans: fontFamily.sans, mono: fontFamily.mono },
      borderRadius: { 'fp-sm': radii.sm, 'fp-md': radii.md, 'fp-lg': radii.lg, 'fp-xl': radii.xl },
      boxShadow: { 'fp-card': shadows.card, 'fp-raised': shadows.raised, 'fp-overlay': shadows.overlay },
      backgroundImage: {
        'fp-orange': 'linear-gradient(180deg, #FF8733 0%, #F97316 100%)',
        'fp-surface': 'linear-gradient(180deg, rgba(255,255,255,0.025), rgba(255,255,255,0) 45%)',
      },
      transitionTimingFunction: { fp: motion.easeCss },
      transitionDuration: { fast: '140ms', base: '220ms', slow: '360ms' },
      keyframes: {
        'fp-rise': { from: { opacity: '0', transform: 'translateY(8px)' }, to: { opacity: '1', transform: 'none' } },
        'fp-fade-in': { from: { opacity: '0' }, to: { opacity: '1' } },
        'fp-scale-in': { from: { opacity: '0', transform: 'scale(0.97)' }, to: { opacity: '1', transform: 'none' } },
        'fp-slide-in-right': { from: { opacity: '0', transform: 'translateX(16px)' }, to: { opacity: '1', transform: 'none' } },
      },
      animation: {
        'fp-rise': `fp-rise 360ms ${motion.easeCss} both`,
        'fp-fade-in': `fp-fade-in 220ms ${motion.easeCss} both`,
        'fp-scale-in': `fp-scale-in 220ms ${motion.easeCss} both`,
        'fp-slide-in-right': `fp-slide-in-right 220ms ${motion.easeCss} both`,
      },
      width: { sidebar: '264px', 'sidebar-collapsed': '76px' },
      height: { header: '64px' },
    },
  },
};
