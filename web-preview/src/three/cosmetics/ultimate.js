import * as THREE from 'three';
import { M } from '../materials.js';
import { christmasTree, giftBox, ornamentBall, candyCane, bell, starGeometry, snowflakeGeometry, lightString, hollySprig, TAU, rand } from '../geometry.js';
import { ParticleSystem } from '../particles.js';
import { flakeSprite, sparkleSprite, glowSprite, beamTexture } from '../textures.js';
import { cosmetic } from './base.js';
import { groundRing } from './auras.js';

/**
 * Day 24 – ULTIMATE CHRISTMAS AURA (Mythic)
 * Layered composition around the player:
 *   L0 ground:   double rune ring + glow disc + warm uplight
 *   L1 orbit:    6 decorated trees (large/small) slowly circling the player
 *   L2 float:    8 gifts at different heights, bobbing & tumbling (counter-rotating)
 *   L3 float:    ornaments, rotating candy canes, swinging bells, snowflakes
 *   L4 garland:  blinking light-string helix spiralling around the player
 *   L5 crown:    giant golden Christmas star above the head with rotating god-rays
 *   FX:          continuous snowfall, rising golden embers, twinkling star glints
 */
export function ultimateChristmasAura() {
  const c = cosmetic('aura');
  const R = rand(2412);
  const all = new THREE.Group(); c.root.add(all);

  /* ---------- L0 ground */
  const ring = groundRing('#ffd36b', 'star', 3.6, 2.2);
  const ring2 = groundRing('#ff5a6e', 'flake', 2.4, 1.2); ring2.position.y = 0.014;
  const disc = new THREE.Mesh(new THREE.CircleGeometry(1.9, 96), new THREE.MeshBasicMaterial({ map: glowSprite(), color: new THREE.Color('#ffb347').multiplyScalar(0.7), transparent: true, blending: THREE.AdditiveBlending, depthWrite: false }));
  disc.rotation.x = -Math.PI / 2; disc.position.y = 0.01;
  const uplight = new THREE.PointLight('#ffcf6b', 3, 5, 1.6); uplight.position.set(0, 0.3, 0);
  all.add(ring, ring2, disc, uplight);

  /* ---------- L1 orbiting trees */
  const treeOrbit = new THREE.Group(); all.add(treeOrbit);
  const trees = [];
  for (let i = 0; i < 6; i++) {
    const big = i % 2 === 0;
    const tr = christmasTree({ height: big ? 0.95 : 0.55, seed: 30 + i });
    const a = (i / 6) * TAU;
    const r = big ? 1.55 : 1.35;
    tr.position.set(Math.cos(a) * r, 0, Math.sin(a) * r);
    const base = new THREE.Mesh(new THREE.CylinderGeometry(big ? 0.2 : 0.13, big ? 0.24 : 0.16, 0.05, 32), M.snow()); base.position.y = 0.02; tr.add(base);
    treeOrbit.add(tr); trees.push(tr);
  }

  /* ---------- L2 floating gifts */
  const giftOrbit = new THREE.Group(); all.add(giftOrbit);
  const looks = [['red', 'stars', '#ffd36b'], ['green', 'dots', '#ffd36b'], ['white', 'snowflakes', '#c8102e'], ['gold', 'stripes', '#7a1020'], ['night', 'stars', '#cfe8ff'], ['royal', 'plaid', '#ffd36b'], ['ice', 'snowflakes', '#ffffff'], ['red', 'plaid', '#0f5a2a']];
  const gifts = looks.map(([pal, motif, rib], i) => {
    const s = 0.16 + R() * 0.12;
    const gb = giftBox({ w: s, h: s * (0.8 + R() * 0.5), d: s, palette: pal, motif, ribbon: rib, ribbonMetal: rib === '#ffd36b' });
    const holder = new THREE.Group(); holder.add(gb); gb.position.y = -s * 0.5;
    const a = (i / looks.length) * TAU + 0.3;
    holder.userData = { a, r: 0.95 + R() * 0.3, h: 0.45 + (i % 4) * 0.42 + R() * 0.1, ph: R() * TAU, spin: (R() - 0.5) * 1.2 };
    giftOrbit.add(holder);
    return holder;
  });

  /* ---------- L3 ornaments, candy canes, bells, snowflakes */
  const midOrbit = new THREE.Group(); all.add(midOrbit);
  const ornColors = ['#c8102e', '#ffd36b', '#cfe8ff', '#0f7a3c', '#e8e8e8'];
  const ornaments = Array.from({ length: 10 }, (_, i) => {
    const o = ornamentBall(ornColors[i % ornColors.length], 0.05 + R() * 0.03);
    o.userData = { a: (i / 10) * TAU, r: 0.75 + R() * 0.25, h: 0.6 + R() * 1.6, ph: R() * TAU };
    midOrbit.add(o); return o;
  });
  const canes = Array.from({ length: 6 }, (_, i) => {
    const cn = candyCane(0.32, 0.018, i % 2 ? '#c8102e' : '#1f8a45');
    const holder = new THREE.Group(); cn.position.y = -0.16; holder.add(cn);
    holder.userData = { a: (i / 6) * TAU + 0.5, r: 1.15, h: 0.9 + (i % 3) * 0.5, ph: R() * TAU };
    midOrbit.add(holder); return holder;
  });
  const bells = Array.from({ length: 4 }, (_, i) => {
    const holder = new THREE.Group();
    const b = bell(M.gold({ roughness: 0.12 })); b.scale.setScalar(0.07); b.position.y = -0.12; holder.add(b);
    const h = hollySprig(0.07); h.position.y = -0.03; holder.add(h);
    holder.userData = { a: (i / 4) * TAU + 1.1, r: 0.68, h: 1.75 + (i % 2) * 0.2, ph: i * 1.7, bell: b };
    midOrbit.add(holder); return holder;
  });
  const flakeMat = new THREE.MeshPhysicalMaterial({ color: '#f2fbff', roughness: 0.12, metalness: 0.3, clearcoat: 1, emissive: '#8fd3ff', emissiveIntensity: 1.4, iridescence: 0.6 });
  const flakes = Array.from({ length: 16 }, (_, i) => {
    const f = new THREE.Mesh(snowflakeGeometry(i % 3, 0.06), flakeMat); f.scale.setScalar(0.05 + R() * 0.05);
    f.userData = { a: R() * TAU, r: 0.6 + R() * 1.0, h: 0.3 + R() * 2.2, ph: R() * TAU, sp: 0.3 + R() * 0.4 };
    midOrbit.add(f); return f;
  });

  /* ---------- L4 light-string helix */
  const helixPts = [];
  for (let i = 0; i <= 240; i++) { const t = i / 240, a = t * TAU * 3.2; const r = 0.88 - t * 0.25; helixPts.push(new THREE.Vector3(Math.cos(a) * r, 0.15 + t * 2.1, Math.sin(a) * r)); }
  const helix = lightString(new THREE.CatmullRomCurve3(helixPts), 90, ['#ffcf6b', '#ff4d4d', '#ffffff', '#7fd4ff', '#8dff9a'], 0.018);
  all.add(helix);

  /* ---------- L5 giant golden star */
  const starRig = new THREE.Group(); starRig.position.y = 2.55; all.add(starRig);
  const bigStar = new THREE.Mesh(starGeometry(5, 0.3, 0.13, 0.09), M.gold({ roughness: 0.1, emissive: new THREE.Color('#ff9f1a'), emissiveIntensity: 0.35 })); bigStar.castShadow = true;
  const innerStar = new THREE.Mesh(starGeometry(5, 0.2, 0.085, 0.1), M.glow('#fff1c2', 7)); innerStar.position.z = 0.0;
  const backStar = new THREE.Mesh(starGeometry(5, 0.22, 0.1, 0.06), M.gold({ roughness: 0.2 })); backStar.rotation.z = Math.PI / 5; backStar.scale.setScalar(0.92);
  starRig.add(bigStar, innerStar, backStar);
  const starGlow = new THREE.Sprite(new THREE.SpriteMaterial({ map: glowSprite(), color: '#ffcf6b', blending: THREE.AdditiveBlending, depthWrite: false, opacity: 0.95 })); starGlow.scale.setScalar(1.7);
  const flare = new THREE.Sprite(new THREE.SpriteMaterial({ map: sparkleSprite(), color: '#ffffff', blending: THREE.AdditiveBlending, depthWrite: false })); flare.scale.setScalar(1.5);
  starRig.add(starGlow, flare);
  const starLight = new THREE.PointLight('#ffd58a', 6, 6, 1.5); starRig.add(starLight);
  const rays = new THREE.Group(); starRig.add(rays);
  const rayMat = new THREE.MeshBasicMaterial({ map: beamTexture(), color: new THREE.Color('#ffd36b').multiplyScalar(1.3), transparent: true, blending: THREE.AdditiveBlending, depthWrite: false, side: THREE.DoubleSide });
  for (let i = 0; i < 10; i++) { const ray = new THREE.Mesh(new THREE.PlaneGeometry(0.1, 1.9), rayMat); ray.rotation.z = (i / 10) * Math.PI; ray.position.z = -0.05; rays.add(ray); }
  // mini star satellites
  const sats = Array.from({ length: 12 }, (_, i) => { const s = new THREE.Mesh(starGeometry(5, 0.045, 0.02, 0.015), M.glow('#ffe39a', 5)); s.userData = { a: (i / 12) * TAU, r: 1.45 + (i % 2) * 0.25, h: 0.5 + (i % 4) * 0.55 }; all.add(s); return s; });

  /* ---------- FX */
  const snow = c.track(new ParticleSystem({
    max: 1500, map: flakeSprite(), rate: 260, life: [3.5, 6], size: [0.025, 0.07], color: ['#ffffff', '#e8f6ff'], spin: [-1.2, 1.2], intensity: 1.7, fadeIn: 0.12,
    spawn: (P, V) => { const a = Math.random() * TAU, r = Math.sqrt(Math.random()) * 2.1; P.set(Math.cos(a) * r, 3.1, Math.sin(a) * r); V.set(0, -0.45, 0); },
    force: (k, P, V, dt, t) => { V[k] += Math.sin(t * 0.7 + P[k + 1] * 2.1) * 0.1 * dt; V[k + 2] += Math.cos(t * 0.6 + P[k] * 2.3) * 0.1 * dt; },
    prewarm: 6,
  }));
  const embers = c.track(new ParticleSystem({
    max: 1400, map: sparkleSprite(), rate: 300, life: [2, 3.6], size: [0.02, 0.06], color: ['#ffd36b', '#ffb52e', '#fff1c2', '#ff8a3d'], spin: [-3, 3], intensity: 2.6, fadeIn: 0.2,
    spawn: (P, V) => { const a = Math.random() * TAU, r = 0.45 + Math.random() * 1.2; P.set(Math.cos(a) * r, Math.random() * 0.2, Math.sin(a) * r); V.set(0, 0.5 + Math.random() * 0.5, 0); },
    force: (k, P, V) => { const x = P[k], z = P[k + 2], r = Math.hypot(x, z) + 1e-4; V[k] = (-z / r) * 0.45; V[k + 2] = (x / r) * 0.45; },
    prewarm: 3.5,
  }));
  const glints = c.track(new ParticleSystem({
    max: 300, map: sparkleSprite(), rate: 60, life: [0.3, 0.9], size: [0.06, 0.16], color: ['#ffffff', '#fff1c2', '#cfe8ff'], intensity: 3.2, fadeIn: 0.4, fadeOut: 0.5,
    spawn: (P) => { const a = Math.random() * TAU, r = 0.5 + Math.random() * 1.3; P.set(Math.cos(a) * r, Math.random() * 2.8, Math.sin(a) * r); },
    prewarm: 1,
  }));
  all.add(snow, embers, glints);

  /* ---------- intro (used by the finale reveal): scale everything in */
  c.intro = 1; // 0 → hidden, 1 → fully shown; animated by the reveal sequence
  c.on((t, dt) => {
    const k = c.intro;
    all.scale.setScalar(0.001 + k * 0.999);
    treeOrbit.rotation.y = t * 0.12;
    trees.forEach((tr, i) => { tr.rotation.y = -t * 0.3; tr.userData.lights?.userData.update(t + i); tr.userData.star.rotation.y = t * 2; });
    giftOrbit.rotation.y = -t * 0.22;
    gifts.forEach((g) => { const d = g.userData; g.position.set(Math.cos(d.a) * d.r, d.h + Math.sin(t * 1.1 + d.ph) * 0.09, Math.sin(d.a) * d.r); g.rotation.set(Math.sin(t * 0.8 + d.ph) * 0.25, t * d.spin, Math.cos(t * 0.6 + d.ph) * 0.2); });
    midOrbit.rotation.y = t * 0.28;
    ornaments.forEach((o) => { const d = o.userData; o.position.set(Math.cos(d.a) * d.r, d.h + Math.sin(t * 1.5 + d.ph) * 0.06, Math.sin(d.a) * d.r); o.rotation.y = t; });
    canes.forEach((h) => { const d = h.userData; h.position.set(Math.cos(d.a) * d.r, d.h + Math.sin(t + d.ph) * 0.07, Math.sin(d.a) * d.r); h.rotation.set(0.3, t * 1.6 + d.ph, Math.sin(t * 1.3 + d.ph) * 0.4); });
    bells.forEach((h) => { const d = h.userData; h.position.set(Math.cos(d.a) * d.r, d.h + Math.sin(t * 1.2 + d.ph) * 0.04, Math.sin(d.a) * d.r); h.rotation.z = Math.sin(t * 2.6 + d.ph) * 0.35; });
    flakes.forEach((f) => { const d = f.userData; const a = d.a + t * d.sp; f.position.set(Math.cos(a) * d.r, d.h + Math.sin(t + d.ph) * 0.1, Math.sin(a) * d.r); f.rotation.set(t * 0.6 + d.ph, a, t * 0.9); });
    helix.userData.update(t);
    sats.forEach((s, i) => { const d = s.userData; const a = d.a - t * 0.35; s.position.set(Math.cos(a) * d.r, d.h + Math.sin(t * 1.4 + i) * 0.08, Math.sin(a) * d.r); s.rotation.y = t * 3; s.scale.setScalar(0.7 + Math.pow(0.5 + 0.5 * Math.sin(t * 4.5 + i * 1.9), 3) * 0.9); });
    starRig.position.y = 2.55 + Math.sin(t * 1.2) * 0.05;
    bigStar.rotation.y = Math.sin(t * 0.8) * 0.5; innerStar.rotation.y = bigStar.rotation.y; backStar.rotation.y = bigStar.rotation.y;
    rays.rotation.z = t * 0.25; rayMat.opacity = 0.45 + Math.sin(t * 2.2) * 0.15;
    const tw = 0.85 + Math.sin(t * 5) * 0.1 + Math.sin(t * 11.3) * 0.05;
    starGlow.scale.setScalar(1.7 * tw); flare.material.rotation = t * 0.5; flare.scale.setScalar(1.4 + Math.sin(t * 3) * 0.2);
    innerStar.material.emissiveIntensity = 6 + Math.sin(t * 3) * 2;
    starLight.intensity = 6 * tw * k;
    ring.rotation.z = t * 0.1; ring2.rotation.z = -t * 0.18;
    ring.material.opacity = 0.8 + Math.sin(t * 1.7) * 0.2;
    uplight.intensity = 3 * k;
    snow.tick(dt); embers.tick(dt); glints.tick(dt);
  });
  return c;
}
