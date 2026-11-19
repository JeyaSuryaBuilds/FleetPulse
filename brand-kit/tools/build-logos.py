#!/usr/bin/env python3
"""
FleetPulse logo generator.
Regenerates every SVG in src/assets/brand/ with the wordmark converted to
outlined paths (no font dependency). Requires: pip install fonttools
Set FLEETPULSE_FONT to a local Inter Display SemiBold .otf/.ttf file if the default path differs.
"""
import os
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen

FONT = os.environ.get("FLEETPULSE_FONT", "/usr/share/fonts/opentype/inter/InterDisplay-SemiBold.otf")
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "assets", "brand")
os.makedirs(OUT, exist_ok=True)

ORANGE_1, ORANGE_2 = "#FF8733", "#F97316"
GRAPHITE_TOP, GRAPHITE_BOT = "#2C2C2C", "#141414"

def mark_shapes(paint, bold=False):
    """Forward-leaning F: solid top arm, a pulse trace as the middle arm, ending in a GPS waypoint. 64x64 box."""
    sw = 6 if bold else 4.5
    node = (f'<circle cx="49" cy="41" r="4.4" fill="{paint}"/>' if bold else
            f'<circle cx="49" cy="41" r="3" fill="{paint}"/>'
            f'<circle cx="49" cy="41" r="5.6" fill="none" stroke="{paint}" stroke-width="1.6" opacity=".7"/>')
    return (
        f'<path d="M18 50H24.5L30.9 20H24.4Z" fill="{paint}"/>'
        f'<path d="M24.4 20H52L50.7 26H29.6Z" fill="{paint}"/>'
        f'<path d="M25 41H33L36 34.5L39.5 46L42 41H43.8" fill="none" stroke="{paint}" stroke-width="{sw}" stroke-linejoin="round"/>'
        + node
    )

def mark_group(paint="url(#fp-or)", depth=True, bold=False):
    under = ""
    if depth:
        under = f'<g transform="translate(0 1.3)" opacity=".5">{mark_shapes("#6E2D07", bold)}</g>'
    return f'<g transform="translate(-4.5 -3)">{under}{mark_shapes(paint, bold)}</g>'

def defs(tile=True):
    d = (f'<linearGradient id="fp-or" gradientUnits="userSpaceOnUse" x1="0" y1="20" x2="0" y2="50">'
         f'<stop offset="0" stop-color="{ORANGE_1}"/><stop offset="1" stop-color="{ORANGE_2}"/></linearGradient>')
    if tile:
        d += (f'<linearGradient id="fp-tile" x1="0" y1="0" x2="0" y2="1">'
              f'<stop offset="0" stop-color="{GRAPHITE_TOP}"/><stop offset="1" stop-color="{GRAPHITE_BOT}"/></linearGradient>'
              f'<linearGradient id="fp-rim" x1="0" y1="0" x2="0" y2="1">'
              f'<stop offset="0" stop-color="#fff" stop-opacity=".16"/><stop offset=".45" stop-color="#fff" stop-opacity="0"/>'
              f'<stop offset="1" stop-color="#000" stop-opacity=".35"/></linearGradient>')
    return f"<defs>{d}</defs>"

def tile(radius=15, full_bleed=False):
    if full_bleed:
        return ('<rect width="64" height="64" fill="url(#fp-tile)"/>'
                '<rect width="64" height="64" fill="url(#fp-rim)"/>')
    return (f'<rect x="1" y="1" width="62" height="62" rx="{radius}" fill="url(#fp-tile)"/>'
            f'<rect x="1.5" y="1.5" width="61" height="61" rx="{radius-.5}" fill="none" stroke="url(#fp-rim)" stroke-width="1"/>'
            f'<rect x=".5" y=".5" width="63" height="63" rx="{radius+.5}" fill="none" stroke="#303030" stroke-width="1"/>')

def svg(w, h, body, title):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="{w}" height="{h}" role="img" aria-label="{title}">'
            f'<title>{title}</title>{body}</svg>\n')

def wordmark_path(text="FleetPulse", size=38, tracking=-0.012, x0=0, baseline=0):
    font = TTFont(FONT)
    gs = font.getGlyphSet()
    cmap = font.getBestCmap()
    sc = size / font["head"].unitsPerEm
    x, parts = x0, []
    for ch in text:
        name = cmap[ord(ch)]
        pen = SVGPathPen(gs, ntos=lambda v: f"{v:.2f}".rstrip("0").rstrip("."))
        gs[name].draw(TransformPen(pen, (sc, 0, 0, -sc, x, baseline)))
        parts.append(pen.getCommands())
        x += gs[name].width * sc + tracking * size
    return " ".join(parts), x - tracking * size - x0

def horizontal(text_color=None, mono=None):
    d, tw = wordmark_path(x0=81, baseline=46)
    W = round(81 + tw + 2)
    if mono:
        body = mark_group(mono, depth=False) + f'<path d="{d}" fill="{mono}"/>'
    else:
        body = defs() + tile() + mark_group() + f'<path d="{d}" fill="{text_color}"/>'
    return svg(W, 64, body, "FleetPulse")

def write(name, content):
    with open(os.path.join(OUT, name), "w", encoding="utf-8") as f:
        f.write(content)
    print("wrote", name)

write("fleetpulse-logo-horizontal-dark.svg",  horizontal("#FFFFFF"))   # for dark surfaces
write("fleetpulse-logo-horizontal-light.svg", horizontal("#171717"))   # for light surfaces
write("fleetpulse-logo-mono-white.svg",       horizontal(mono="#FFFFFF"))
write("fleetpulse-logo-mono-black.svg",       horizontal(mono="#111111"))
write("fleetpulse-icon.svg", svg(64, 64, defs() + tile() + mark_group(), "FleetPulse"))
write("fleetpulse-mark.svg", svg(64, 64, defs(tile=False) + mark_group(), "FleetPulse"))

fav = defs() + '<rect width="64" height="64" rx="14" fill="url(#fp-tile)"/>' + '<g transform="translate(32 32) scale(1.12) translate(-32 -32)">' + mark_group(depth=False, bold=True) + '</g>'
write("favicon.svg", svg(64, 64, fav, "FleetPulse"))

write("app-icon-source.svg", svg(64, 64, defs() + tile(full_bleed=True) + mark_group(), "FleetPulse"))
write("android-adaptive-foreground.svg",
      svg(108, 108, defs(tile=False) + f'<g transform="translate(24 24) scale(.9)">{mark_group()}</g>', "FleetPulse"))
