import FleetPulseLogo from './FleetPulseLogo.jsx';

/**
 * Full-screen loading state. Use as the Suspense fallback / initial auth check.
 * <Suspense fallback={<BrandLoader />}> ... </Suspense>
 */
export default function BrandLoader({ label = 'Loading FleetPulse' }) {
  return (
    <div className="fp-loader" role="status" aria-live="polite">
      <div className="fp-loader__inner">
        <FleetPulseLogo variant="icon" height={64} decorative />
        <svg width="120" height="16" viewBox="0 0 120 16" aria-hidden="true">
          <path className="fp-loader__trace" pathLength="1" d="M2 8H38L46 2L54 14L62 8H118" />
        </svg>
        <span className="fp-sr-only">{label}</span>
      </div>
    </div>
  );
}
