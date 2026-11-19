# FleetPulse Logo Kit and Design System: integration

Nothing in this kit has been run inside your project. Test it on your laptop.

## 1. Copy into the website (React + Vite + Tailwind v3)

| Kit path | Copy to |
|---|---|
| `src/assets/brand/` | `src/assets/brand/` |
| `src/styles/` | `src/styles/` |
| `src/design-system/` | `src/design-system/` |
| `src/components/brand/` | `src/components/brand/` |
| `public/*` | your `public/` (merge, replace the Vite default favicon) |
| `tailwind.preset.js` | project root |

```bash
npm i @fontsource-variable/inter framer-motion lucide-react
```

**tailwind.config.js**
```js
import fleetpulse from './tailwind.preset.js';
export default {
  presets: [fleetpulse],
  content: ['./index.html', './src/**/*.{js,jsx,ts,tsx}'],
};
```

**main.jsx**
```jsx
import './styles/index.css';
import { FleetPulseMotion } from './design-system/motion.js';

createRoot(document.getElementById('root')).render(
  <FleetPulseMotion><App /></FleetPulseMotion>
);
```

**index.html**: add `data-theme="dark"` to `<html>` and paste `public/head-snippet.html` into `<head>`.

If your postcss setup does not inline `@import`, add `postcss-import` as the first PostCSS plugin.

## 2. Where the logo goes

| Place | Usage |
|---|---|
| Public site header and footer | `<FleetPulseLogo variant="horizontal" height={32} />` (use `theme="light"` on light sections) |
| Login, registration, /admin | `<FleetPulseLogo variant="horizontal" height={40} />` centred above the form |
| Company Admin and Super Admin sidebar | expanded: `horizontal`, collapsed: `<FleetPulseLogo variant="icon" height={36} />` |
| Dashboard header (mobile) | `variant="icon"` |
| Customer tracking portal | `horizontal`, with `theme="light"` if the page is light |
| Loading screen | `<BrandLoader />` as the Suspense fallback and auth-check screen |
| Emails, invoices, PDFs | `fleetpulse-logo-horizontal-light.svg` or `fleetpulse-logo-mono-black.svg` |

Wrap the logo in a link and name the link, not the image: `<a href="/" aria-label="FleetPulse home"><FleetPulseLogo decorative /></a>`.

Sidebar collapse example:
```jsx
<aside className="fp-sidebar" data-collapsed={collapsed}>
  <FleetPulseLogo variant={collapsed ? 'icon' : 'horizontal'} height={collapsed ? 36 : 32} />
</aside>
```

## 3. Windows desktop (Tauri)

```bash
npm i -D @tauri-apps/cli
npx tauri icon app-icons/tauri-icon-source-1024.png
```
This generates `src-tauri/icons/` (.ico, .icns, PNGs) for the installer. In `tauri.conf.json` set `productName` to `FleetPulse` and `identifier` to your reverse-domain ID (for example `com.fayzotechnologies.fleetpulse`). The window background should be `#111111` to avoid a white flash on launch.

## 4. Android and iOS (Expo)

```bash
npx expo install expo-font @expo-google-fonts/inter expo-splash-screen
```

**app.json**
```json
{
  "expo": {
    "name": "FleetPulse",
    "icon": "./assets/ios-app-icon-1024.png",
    "backgroundColor": "#111111",
    "userInterfaceStyle": "dark",
    "splash": { "image": "./assets/splash-mark-1024.png", "resizeMode": "contain", "backgroundColor": "#111111" },
    "ios": { "icon": "./assets/ios-app-icon-1024.png" },
    "android": {
      "adaptiveIcon": {
        "foregroundImage": "./assets/android-adaptive-foreground-1024.png",
        "backgroundImage": "./assets/android-adaptive-background-1024.png",
        "backgroundColor": "#171717"
      }
    }
  }
}
```
Copy the files from `app-icons/` into the app's `assets/`. Import colours from `src/design-system/tokens.native.ts`. For the in-app logo, convert the SVGs with `react-native-svg` (or use `splash-mark-1024.png` as an `Image`).

`ios-app-icon-1024.png` has no transparency, as the App Store requires. Do not add rounded corners to it; iOS applies its own mask.

## 5. Regenerating assets

```bash
pip install fonttools && python tools/build-logos.py    # rewrites src/assets/brand/*.svg
npm i -D sharp && node tools/export-icons.js            # rewrites PNGs in public/icons and app-icons
```
`build-logos.py` converts the wordmark to outlined paths using Inter Display SemiBold. Set `FLEETPULSE_FONT` to your local font file if the default path does not exist.

## 6. Brand rules

- Symbol: a forward-leaning F whose middle arm is a pulse trace ending in a GPS waypoint. Orange on graphite.
- Clear space: at least the height of the F's top arm on every side. Minimum size: 24px for the icon, 100px wide for the horizontal logo.
- Use `theme="dark"` on dark surfaces and `theme="light"` on light ones. Never recolour the symbol; use the mono files for single-colour jobs.
- Text on orange fills is `#111111`, not white (white on `#FF8733` fails contrast).
- Orange is for primary actions, active navigation, route lines and key highlights. Keep large surfaces graphite.
- Do not add glows, outlines, stretched or rotated versions, or place the symbol on busy photos.

## 7. Design system quick reference

- Tokens: `src/styles/tokens.css` (CSS variables) and `src/design-system/tokens.js` (JS). Keep them in sync.
- Tailwind classes: `bg-fp-bg`, `bg-fp-sidebar`, `bg-fp-card`, `border-fp-border`, `text-fp-muted`, `bg-fp-orange`, `text-orange`, `shadow-fp-card`, `rounded-fp-lg`, `animate-fp-rise`.
- Component classes: `.fp-card`, `.fp-btn--primary|secondary|ghost|danger`, `.fp-input`, `.fp-badge--success|warning|error|brand`, `.fp-sidebar`, `.fp-nav-link`, `.fp-table`, `.fp-modal`, `.fp-toast`, `.fp-skeleton`, `.fp-spinner`.
- Animations: `.fp-rise`, `.fp-fade-in`, `.fp-scale-in`, `.fp-stagger` (set `style={{'--fp-i': index}}` on children), `.fp-route-draw`, `.fp-route-flow`, `.fp-status-dot[data-live="true"]`.
- Reduced motion: handled globally in `animations.css` (CSS) and by `<FleetPulseMotion>` (Framer Motion).
- Light surfaces: put `data-theme="light"` on any container.
