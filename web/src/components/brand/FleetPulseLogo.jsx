import horizontalDark from '../../assets/brand/fleetpulse-logo-horizontal-dark.svg';
import horizontalLight from '../../assets/brand/fleetpulse-logo-horizontal-light.svg';
import monoWhite from '../../assets/brand/fleetpulse-logo-mono-white.svg';
import monoBlack from '../../assets/brand/fleetpulse-logo-mono-black.svg';
import icon from '../../assets/brand/fleetpulse-icon.svg';
import mark from '../../assets/brand/fleetpulse-mark.svg';

const SOURCES = {
  horizontal: { dark: horizontalDark, light: horizontalLight, 'mono-white': monoWhite, 'mono-black': monoBlack },
  icon: { dark: icon, light: icon, 'mono-white': mark, 'mono-black': mark },
  mark: { dark: mark, light: mark, 'mono-white': mark, 'mono-black': mark },
};

/**
 * <FleetPulseLogo variant="horizontal" theme="dark" height={32} />
 *
 * variant: 'horizontal' (symbol + wordmark) | 'icon' (symbol on graphite tile) | 'mark' (symbol only, transparent)
 * theme:   'dark'  -> for dark surfaces  (white wordmark)
 *          'light' -> for light surfaces (graphite wordmark)
 *          'mono-white' | 'mono-black' -> single-colour versions
 * When the logo is a link, put the accessible name on the link and pass decorative.
 */
export default function FleetPulseLogo({
  variant = 'horizontal',
  theme = 'dark',
  height = 32,
  decorative = false,
  className = '',
  ...rest
}) {
  const src = SOURCES[variant]?.[theme] ?? SOURCES.horizontal.dark;
  return (
    <img
      src={src}
      height={height}
      style={{ height, width: 'auto', display: 'block' }}
      alt={decorative ? '' : 'FleetPulse'}
      aria-hidden={decorative ? true : undefined}
      draggable={false}
      className={className}
      {...rest}
    />
  );
}
