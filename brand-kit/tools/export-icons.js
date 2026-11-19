// Rasterises the SVG sources into PNG icon sizes. Requires: npm i -D sharp
const sharp = require('sharp');
const path = require('path');
const B = path.join(__dirname, '..', 'src', 'assets', 'brand');
const P = path.join(__dirname, '..', 'public');
const A = path.join(__dirname, '..', 'app-icons');
const svg = (n, size, out, density = 1200) =>
  sharp(path.join(B, n), { density }).resize(size, size).png().toFile(out);

(async () => {
  // Web
  for (const s of [16, 32, 48]) await svg('favicon.svg', s, path.join(P, 'icons', `favicon-${s}.png`));
  await svg('app-icon-source.svg', 180, path.join(P, 'icons', 'apple-touch-icon.png'));
  await svg('fleetpulse-icon.svg', 192, path.join(P, 'icons', 'icon-192.png'));
  await svg('fleetpulse-icon.svg', 512, path.join(P, 'icons', 'icon-512.png'));

  // Maskable (PWA / Android): full-bleed tile, mark kept inside the 80% safe zone
  const base = await sharp(path.join(B, 'app-icon-source.svg'), { density: 1200 }).resize(512, 512).png().toBuffer();
  const mark = await sharp(path.join(B, 'fleetpulse-mark.svg'), { density: 1200 }).resize(360, 360).png().toBuffer();
  await sharp(base).composite([{ input: mark, left: 76, top: 76 }]).png().toFile(path.join(P, 'icons', 'icon-maskable-512.png'));

  // Native app sources
  await sharp(path.join(B, 'app-icon-source.svg'), { density: 1500 }).resize(1024, 1024).flatten({ background: '#171717' }).png().toFile(path.join(A, 'ios-app-icon-1024.png')); // no alpha, as App Store requires
  await sharp(path.join(B, 'app-icon-source.svg'), { density: 1500 }).resize(1024, 1024).png().toFile(path.join(A, 'tauri-icon-source-1024.png'));
  await sharp(path.join(B, 'android-adaptive-foreground.svg'), { density: 1500 }).resize(1024, 1024).png().toFile(path.join(A, 'android-adaptive-foreground-1024.png'));
  await sharp({ create: { width: 1024, height: 1024, channels: 3, background: '#171717' } }).png().toFile(path.join(A, 'android-adaptive-background-1024.png'));
  await sharp(path.join(B, 'fleetpulse-mark.svg'), { density: 1500 }).resize(1024, 1024).png().toFile(path.join(A, 'splash-mark-1024.png'));
  console.log('icons exported');
})();
