import * as THREE from 'three';

/**
 * Procedural, resolution-independent canvas textures (generated at runtime → no blurry assets,
 * sized up to 2048² where it matters). Cached per key.
 */
const cache = new Map();
let maxAniso = 8;
export const setMaxAnisotropy = (v) => { maxAniso = v; };

function canvasTexture(key, size, draw, { repeat = [1, 1], srgb = true, wrap = true } = {}) {
  const id = `${key}@${size}`;
  if (cache.has(id)) return cache.get(id);
  const c = document.createElement('canvas');
  c.width = c.height = size;
  const g = c.getContext('2d');
  draw(g, size);
  const tex = new THREE.CanvasTexture(c);
  if (srgb) tex.colorSpace = THREE.SRGBColorSpace;
  if (wrap) tex.wrapS = tex.wrapT = THREE.RepeatWrapping;
  tex.repeat.set(...repeat);
  tex.anisotropy = maxAniso;
  tex.generateMipmaps = true;
  tex.minFilter = THREE.LinearMipmapLinearFilter;
  cache.set(id, tex);
  return tex;
}

const rng = (seed) => () => ((seed = (seed * 16807) % 2147483647) / 2147483647);

/** Diagonal candy stripes (u along length, v around). */
export function candyStripes({ base = '#fbf7f0', stripe = '#c8102e', thin = '#2f8f46', count = 6, size = 1024 } = {}) {
  return canvasTexture(`candy-${base}-${stripe}-${thin}-${count}`, size, (g, s) => {
    g.fillStyle = base; g.fillRect(0, 0, s, s);
    const w = s / count;
    g.save();
    for (let i = -count; i < count * 2; i++) {
      const x = i * w;
      g.fillStyle = stripe;
      g.beginPath();
      g.moveTo(x, 0); g.lineTo(x + w * 0.45, 0); g.lineTo(x + w * 0.45 + s, s); g.lineTo(x + s, s); g.fill();
      if (thin) {
        g.fillStyle = thin;
        const o = w * 0.62;
        g.beginPath();
        g.moveTo(x + o, 0); g.lineTo(x + o + w * 0.08, 0); g.lineTo(x + o + w * 0.08 + s, s); g.lineTo(x + o + s, s); g.fill();
      }
    }
    g.restore();
    // subtle sugar sparkle
    const r = rng(7);
    for (let i = 0; i < 2600; i++) {
      g.fillStyle = `rgba(255,255,255,${0.05 + r() * 0.12})`;
      g.fillRect(r() * s, r() * s, 1.5, 1.5);
    }
  });
}

const PAPER = {
  red:    ['#8e0e1c', '#b3162a', '#f3d27a'],
  green:  ['#0d4a2c', '#14663c', '#f3d27a'],
  gold:   ['#b8862b', '#e3b553', '#7a1020'],
  white:  ['#f2efe9', '#ffffff', '#b3162a'],
  night:  ['#0c1a33', '#16294d', '#cfe8ff'],
  ice:    ['#7fb6dd', '#b8def5', '#ffffff'],
  royal:  ['#4a0716', '#7a0c22', '#ffd36b'],
};

/** Gift-wrap paper: base + motif. */
export function giftPaper(palette = 'red', motif = 'stars', size = 1024) {
  const [a, b, ink] = PAPER[palette] ?? PAPER.red;
  return canvasTexture(`paper-${palette}-${motif}`, size, (g, s) => {
    const grd = g.createLinearGradient(0, 0, s, s);
    grd.addColorStop(0, a); grd.addColorStop(1, b);
    g.fillStyle = grd; g.fillRect(0, 0, s, s);
    g.fillStyle = ink; g.strokeStyle = ink;
    const n = 6, cell = s / n;
    for (let y = 0; y < n; y++) for (let x = 0; x < n; x++) {
      const cx = (x + 0.5 + (y % 2) * 0.5) * cell, cy = (y + 0.5) * cell;
      if (motif === 'stars') drawStar(g, cx, cy, cell * 0.22, cell * 0.09, 5);
      else if (motif === 'dots') { g.beginPath(); g.arc(cx, cy, cell * 0.12, 0, Math.PI * 2); g.fill(); }
      else if (motif === 'snowflakes') drawFlake(g, cx, cy, cell * 0.28, Math.max(2, s / 400));
      else if (motif === 'stripes') { if (x === 0) { g.globalAlpha = 0.85; g.fillRect(0, y * cell, s, cell * 0.25); g.globalAlpha = 1; } }
    }
    if (motif === 'plaid') {
      g.globalAlpha = 0.35;
      for (let i = 0; i < n; i++) { g.fillRect(i * cell, 0, cell * 0.3, s); g.fillRect(0, i * cell, s, cell * 0.3); }
      g.globalAlpha = 0.8; g.lineWidth = s / 300;
      for (let i = 0; i < n; i++) { g.beginPath(); g.moveTo(i * cell + cell * 0.65, 0); g.lineTo(i * cell + cell * 0.65, s); g.stroke(); g.beginPath(); g.moveTo(0, i * cell + cell * 0.65); g.lineTo(s, i * cell + cell * 0.65); g.stroke(); }
      g.globalAlpha = 1;
    }
    // paper fibre noise
    const r = rng(palette.length * 31 + motif.length);
    for (let i = 0; i < 4000; i++) {
      g.fillStyle = `rgba(0,0,0,${r() * 0.05})`;
      g.fillRect(r() * s, r() * s, 2, 2);
    }
  });
}

function drawStar(g, cx, cy, ro, ri, pts) {
  g.beginPath();
  for (let i = 0; i < pts * 2; i++) {
    const r = i % 2 ? ri : ro, a = (i / (pts * 2)) * Math.PI * 2 - Math.PI / 2;
    g.lineTo(cx + Math.cos(a) * r, cy + Math.sin(a) * r);
  }
  g.closePath(); g.fill();
}

function drawFlake(g, cx, cy, r, lw) {
  g.lineWidth = lw; g.lineCap = 'round';
  for (let i = 0; i < 6; i++) {
    const a = (i / 6) * Math.PI * 2;
    const dx = Math.cos(a), dy = Math.sin(a);
    g.beginPath(); g.moveTo(cx, cy); g.lineTo(cx + dx * r, cy + dy * r); g.stroke();
    for (const t of [0.45, 0.72]) {
      const bx = cx + dx * r * t, by = cy + dy * r * t, bl = r * 0.28 * (1.1 - t);
      for (const s of [-1, 1]) {
        const ba = a + s * 0.75;
        g.beginPath(); g.moveTo(bx, by); g.lineTo(bx + Math.cos(ba) * bl, by + Math.sin(ba) * bl); g.stroke();
      }
    }
  }
}

/** Soft radial glow sprite. */
export function glowSprite() {
  return canvasTexture('glow', 256, (g, s) => {
    const grd = g.createRadialGradient(s / 2, s / 2, 0, s / 2, s / 2, s / 2);
    grd.addColorStop(0, 'rgba(255,255,255,1)');
    grd.addColorStop(0.18, 'rgba(255,255,255,0.75)');
    grd.addColorStop(0.45, 'rgba(255,255,255,0.18)');
    grd.addColorStop(1, 'rgba(255,255,255,0)');
    g.fillStyle = grd; g.fillRect(0, 0, s, s);
  }, { srgb: false, wrap: false });
}

/** Crisp snowflake sprite. */
export function flakeSprite() {
  return canvasTexture('flake', 256, (g, s) => {
    g.shadowColor = 'white'; g.shadowBlur = s * 0.05;
    g.strokeStyle = 'white';
    drawFlake(g, s / 2, s / 2, s * 0.42, s * 0.035);
  }, { srgb: false, wrap: false });
}

/** Four-point lens sparkle. */
export function sparkleSprite() {
  return canvasTexture('sparkle', 256, (g, s) => {
    const c = s / 2;
    const grd = g.createRadialGradient(c, c, 0, c, c, c * 0.35);
    grd.addColorStop(0, 'rgba(255,255,255,1)'); grd.addColorStop(1, 'rgba(255,255,255,0)');
    g.fillStyle = grd; g.fillRect(0, 0, s, s);
    g.fillStyle = 'white';
    for (const [w, h] of [[s * 0.03, s * 0.48], [s * 0.48, s * 0.03]]) {
      const lg = g.createRadialGradient(c, c, 0, c, c, Math.max(w, h));
      lg.addColorStop(0, 'rgba(255,255,255,1)'); lg.addColorStop(1, 'rgba(255,255,255,0)');
      g.fillStyle = lg;
      g.beginPath(); g.ellipse(c, c, w, h, 0, 0, Math.PI * 2); g.fill();
    }
  }, { srgb: false, wrap: false });
}

/** Dragon/reptile scale pattern (tileable). */
export function scaleTexture(base = '#1d6b3a', edge = '#0b3a1e', highlight = '#3ea862') {
  return canvasTexture(`scales-${base}`, 1024, (g, s) => {
    g.fillStyle = edge; g.fillRect(0, 0, s, s);
    const n = 12, w = s / n;
    for (let row = -1; row <= n * 1.4; row++) for (let col = -1; col <= n; col++) {
      const x = (col + (row % 2) * 0.5) * w, y = row * w * 0.7;
      const grd = g.createRadialGradient(x, y + w * 0.1, w * 0.05, x, y + w * 0.25, w * 0.6);
      grd.addColorStop(0, highlight); grd.addColorStop(0.6, base); grd.addColorStop(1, edge);
      g.fillStyle = grd;
      g.beginPath(); g.ellipse(x, y + w * 0.3, w * 0.48, w * 0.52, 0, 0, Math.PI); g.fill();
    }
  });
}

/** Grey-scale noise usable as bump / roughness. */
export function noiseTexture(seed = 1, size = 512, scale = 1) {
  return canvasTexture(`noise-${seed}-${scale}`, size, (g, s) => {
    const img = g.createImageData(s, s);
    const r = rng(seed * 9973 + 1);
    // value noise with a few octaves on a coarse grid (tileable)
    const octave = (cells) => {
      const grid = Array.from({ length: cells * cells }, () => r());
      return (x, y) => {
        const gx = (x / s) * cells, gy = (y / s) * cells;
        const x0 = Math.floor(gx), y0 = Math.floor(gy), fx = gx - x0, fy = gy - y0;
        const at = (i, j) => grid[((j % cells + cells) % cells) * cells + ((i % cells + cells) % cells)];
        const sx = fx * fx * (3 - 2 * fx), sy = fy * fy * (3 - 2 * fy);
        return (at(x0, y0) * (1 - sx) + at(x0 + 1, y0) * sx) * (1 - sy) + (at(x0, y0 + 1) * (1 - sx) + at(x0 + 1, y0 + 1) * sx) * sy;
      };
    };
    const o = [octave(4 * scale), octave(8 * scale), octave(32 * scale), octave(96 * scale)];
    for (let y = 0; y < s; y++) for (let x = 0; x < s; x++) {
      const v = o[0](x, y) * 0.4 + o[1](x, y) * 0.3 + o[2](x, y) * 0.2 + o[3](x, y) * 0.1;
      const i = (y * s + x) * 4;
      img.data[i] = img.data[i + 1] = img.data[i + 2] = v * 255; img.data[i + 3] = 255;
    }
    g.putImageData(img, 0, 0);
  }, { srgb: false });
}

/** Radial ground rune ring for auras. */
export function runeRing(color = '#ffd36b', variant = 'star') {
  return canvasTexture(`rune-${color}-${variant}`, 2048, (g, s) => {
    const c = s / 2;
    g.strokeStyle = color; g.fillStyle = color;
    g.shadowColor = color; g.shadowBlur = s * 0.01;
    g.lineWidth = s * 0.004;
    for (const r of [0.47, 0.44, 0.31, 0.29]) { g.beginPath(); g.arc(c, c, s * r, 0, Math.PI * 2); g.stroke(); }
    for (let i = 0; i < 24; i++) {
      const a = (i / 24) * Math.PI * 2;
      const x = c + Math.cos(a) * s * 0.375, y = c + Math.sin(a) * s * 0.375;
      if (variant === 'star') drawStar(g, x, y, s * 0.02, s * 0.008, 5);
      else drawFlake(g, x, y, s * 0.025, s * 0.003);
    }
    g.lineWidth = s * 0.0025;
    for (let i = 0; i < 12; i++) {
      const a = (i / 12) * Math.PI * 2;
      g.beginPath(); g.moveTo(c + Math.cos(a) * s * 0.31, c + Math.sin(a) * s * 0.31); g.lineTo(c + Math.cos(a + 0.26) * s * 0.44, c + Math.sin(a + 0.26) * s * 0.44); g.stroke();
    }
    drawFlake(g, c, c, s * 0.2, s * 0.006);
  }, { wrap: false });
}

/** Vertical light-ray gradient (for god rays / beams). */
export function beamTexture() {
  return canvasTexture('beam', 512, (g, s) => {
    const grd = g.createLinearGradient(0, 0, s, 0);
    grd.addColorStop(0, 'rgba(255,255,255,0)'); grd.addColorStop(0.5, 'rgba(255,255,255,1)'); grd.addColorStop(1, 'rgba(255,255,255,0)');
    g.fillStyle = grd; g.fillRect(0, 0, s, s);
    const v = g.createLinearGradient(0, 0, 0, s);
    v.addColorStop(0, 'rgba(0,0,0,1)'); v.addColorStop(0.25, 'rgba(0,0,0,0)'); v.addColorStop(0.75, 'rgba(0,0,0,0)'); v.addColorStop(1, 'rgba(0,0,0,1)');
    g.globalCompositeOperation = 'destination-out'; g.fillStyle = v; g.fillRect(0, 0, s, s);
  }, { srgb: false, wrap: false });
}

/** Peppermint swirl disc. */
export function peppermintTexture(stripe = '#c8102e') {
  return canvasTexture(`pepper-${stripe}`, 1024, (g, s) => {
    const c = s / 2;
    g.fillStyle = '#fbf7f0'; g.beginPath(); g.arc(c, c, c, 0, Math.PI * 2); g.fill();
    g.fillStyle = stripe;
    for (let i = 0; i < 8; i++) {
      g.beginPath(); g.moveTo(c, c);
      for (let k = 0; k <= 40; k++) {
        const t = k / 40, a = (i / 8) * Math.PI * 2 + t * 1.2, r = t * c * 0.98;
        g.lineTo(c + Math.cos(a) * r, c + Math.sin(a) * r);
      }
      for (let k = 40; k >= 0; k--) {
        const t = k / 40, a = (i / 8) * Math.PI * 2 + 0.32 + t * 1.2, r = t * c * 0.98;
        g.lineTo(c + Math.cos(a) * r, c + Math.sin(a) * r);
      }
      g.fill();
    }
  }, { wrap: false });
}
