/**
 * Framer Motion presets for FleetPulse. Wrap the app in <FleetPulseMotion> so
 * every motion component honours the OS "reduce motion" setting automatically.
 */
import { createElement } from 'react';
import { MotionConfig, useReducedMotion } from 'framer-motion';
import { motion as m } from './tokens.js';

export function FleetPulseMotion({ children }) {
  return createElement(
    MotionConfig,
    { reducedMotion: 'user', transition: { duration: m.base, ease: m.ease } },
    children
  );
}

const t = (d = m.base) => ({ duration: d, ease: m.ease });

export const pageTransition = {
  initial: { opacity: 0, y: 8 },
  animate: { opacity: 1, y: 0, transition: t(m.slow) },
  exit: { opacity: 0, transition: t(m.fast) },
};

export const modalTransition = {
  initial: { opacity: 0, scale: 0.97, y: 6 },
  animate: { opacity: 1, scale: 1, y: 0, transition: t() },
  exit: { opacity: 0, scale: 0.98, transition: t(m.fast) },
};

export const drawerRight = {
  initial: { x: 24, opacity: 0 },
  animate: { x: 0, opacity: 1, transition: t() },
  exit: { x: 24, opacity: 0, transition: t(m.fast) },
};

export const listContainer = { animate: { transition: { staggerChildren: 0.045 } } };
export const cardEntrance = {
  initial: { opacity: 0, y: 10 },
  animate: { opacity: 1, y: 0, transition: t(m.slow) },
};

export const sidebarWidth = { expanded: 264, collapsed: 76 };
export const sidebarTransition = { duration: m.base, ease: m.ease };

/** Draw-on route line. Apply to <motion.path pathLength={1} ...>. */
export const routeDraw = {
  initial: { pathLength: 0, opacity: 0.4 },
  animate: { pathLength: 1, opacity: 1, transition: { duration: 1.2, ease: m.ease } },
};

/** Use when you need to branch manually, e.g. skip a counting animation. */
export function usePrefersReducedMotion() {
  return !!useReducedMotion();
}
