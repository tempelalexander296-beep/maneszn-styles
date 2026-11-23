import * as THREE from 'three';
import { M } from '../materials.js';
import { snowflakeGeometry, crystalGeometry, TAU } from '../geometry.js';
import { ParticleSystem, RibbonTrail } from '../particles.js';
import { flakeSprite, sparkleSprite, runeRing, glowSprite } from '../textures.js';
import { cosmetic } from './base.js';

export function groundRing(color, variant, size = 2.6, intensity = 2) {
  const mat = new THREE.MeshBasicMaterial({ map: runeRing(color, variant), transparent: true, blending: THREE.AdditiveBlending, depthWrite: false, color: new THREE.Color(1, 1, 1).multiplyScalar(intensity) });
  const m = new THREE.Mesh(new THREE.PlaneGeometry(size, size), mat);
  m.rotation.x = -Math.PI / 2; m.position.y = 0.012;
  return m;
}

/* ------------------------------------------------------------- Day 4: Snowfall Aura */
export function snowfallAura() {
  const c = cosmetic('aura');
  const N = 34;
  const mat = new THREE.MeshPhysicalMaterial({ color: '#f2fbff', roughness: 0.15, metalness: 0.2, clearcoat: 1, emissive: '#8fd3ff', emissiveIntensity: 0.9, iridescence: 0.5 });
  const meshes = [0, 1, 2].map((v) => { const m = new THREE.InstancedMesh(snowflakeGeometry(v, 0.06), mat, Math.ceil(N / 3)); m.castShadow = true; return m; });
  const data = Array.from({ length: N }, (_, i) => ({ r: 0.55 + Math.random() * 0.5, h: 0.15 + Math.random() * 1.9, a: Math.random() * TAU, sp: 0.25 + Math.random() * 0.35, s: 0.04 + Math.random() * 0.05, ph: Math.random() * TAU, mesh: i % 3, idx: Math.floor(i / 3) }));
  const ring = groundRing('#bfe9ff', 'flake', 2.4, 1.4);
  const snow = c.track(new ParticleSystem({
    max: 900, map: flakeSprite(), rate: 140, life: [3, 5], size: [0.025, 0.06], color: ['#ffffff', '#e2f4ff'], gravity: [0, -0.05, 0], spin: [-1, 1], intensity: 1.6, fadeIn: 0.15,
    spawn: (P, V) => { const a = Math.random() * TAU, r = 0.3 + Math.random() * 1.0; P.set(Math.cos(a) * r, 2.3, Math.sin(a) * r); V.set(0, -0.35, 0); },
    force: (k, P, V, dt, t) => { V[k] += Math.sin(t * 0.8 + P[k + 1] * 3) * 0.08 * dt; V[k + 2] += Math.cos(t * 0.7 + P[k] * 3) * 0.08 * dt; },
    prewarm: 5,
  }));
  c.root.add(...meshes, ring, snow);
  const m = new THREE.Matrix4(), q = new THREE.Quaternion(), e = new THREE.Euler(), p = new THREE.Vector3(), s = new THREE.Vector3();
  c.on((t, dt) => {
    for (const d of data) {
      const a = d.a + t * d.sp;
      p.set(Math.cos(a) * d.r, d.h + Math.sin(t * 1.2 + d.ph) * 0.08, Math.sin(a) * d.r);
      e.set(Math.sin(t + d.ph) * 0.6, -a, t * 0.8 + d.ph); q.setFromEuler(e); s.setScalar(d.s);
      m.compose(p, q, s); meshes[d.mesh].setMatrixAt(d.idx, m);
    }
    meshes.forEach((x) => { x.instanceMatrix.needsUpdate = true; });
    ring.rotation.z = t * 0.15;
    ring.material.opacity = 0.75 + Math.sin(t * 1.5) * 0.2;
    snow.tick(dt);
  });
  return c;
}

/* ------------------------------------------------------------- Day 20: Winter Storm Aura */
export function winterStormAura() {
  const c = cosmetic('aura');
  const vortex = c.track(new ParticleSystem({
    max: 3200, map: flakeSprite(), rate: 900, life: [2, 3.5], size: [0.02, 0.07], color: ['#ffffff', '#d4efff', '#9fd8ff'], spin: [-4, 4], intensity: 1.7, fadeIn: 0.1,
    spawn: (P, V) => { const a = Math.random() * TAU, r = 0.6 + Math.random() * 0.8; P.set(Math.cos(a) * r, Math.random() * 0.3, Math.sin(a) * r); V.set(0, 0.6 + Math.random() * 0.4, 0); },
    force: (k, P, V, dt, t, life) => {
      const x = P[k], z = P[k + 2]; const r = Math.hypot(x, z) + 1e-4;
      const swirl = 3.2 / (0.4 + r), pull = (r - (0.7 + P[k + 1] * 0.25)) * 1.6;
      V[k] = (-z / r) * swirl - (x / r) * pull; V[k + 2] = (x / r) * swirl - (z / r) * pull;
      V[k + 1] += Math.sin(t * 3 + x * 4) * 0.4 * dt;
    },
    prewarm: 3,
  }));
  const mist = c.track(new ParticleSystem({
    max: 500, rate: 120, life: [2, 3], size: [0.25, 0.6], color: ['#a6d6ff', '#e6f6ff'], sizeEnd: 1.8, intensity: 0.35, fadeIn: 0.3, fadeOut: 0.6,
    spawn: (P, V) => { const a = Math.random() * TAU, r = 0.7 + Math.random() * 0.6; P.set(Math.cos(a) * r, Math.random() * 2.2, Math.sin(a) * r); V.set(-Math.sin(a) * 1.4, 0.2, Math.cos(a) * 1.4); },
    prewarm: 3,
  }));
  const winds = [0, 1, 2].map((i) => new RibbonTrail({ length: 60, width: 0.035, colorA: '#e8f8ff', colorB: '#6bb8ff', intensity: 1.8 }));
  const shardMat = M.ice({ emissiveIntensity: 0.7 });
  const shards = Array.from({ length: 14 }, (_, i) => { const m = new THREE.Mesh(crystalGeometry(5, 0.16, 0.03, 0.06), shardMat); m.userData = { r: 0.75 + (i % 4) * 0.12, h: 0.3 + (i / 14) * 1.8, a: (i / 14) * TAU, sp: 2 + (i % 3) * 0.6 }; return m; });
  const flash = new THREE.PointLight('#bfe4ff', 0, 6, 2); flash.position.set(0, 2.4, 0);
  const ring = groundRing('#9fd8ff', 'flake', 3, 1.8);
  c.root.add(vortex, mist, ...winds, ...shards, flash, ring);
  const p = new THREE.Vector3();
  for (let t = -2; t < 0; t += 1 / 60) winds.forEach((w, i) => { const a = t * 3.2 + i * 2.1; p.set(Math.cos(a) * (0.8 + i * 0.15), 0.3 + ((t * 0.6 + i * 0.6) % 2.2 + 2.2) % 2.2, Math.sin(a) * (0.8 + i * 0.15)); w.push(p, t); });
  let nextFlash = 2;
  c.on((t, dt) => {
    vortex.tick(dt); mist.tick(dt);
    winds.forEach((w, i) => { const a = t * 3.2 + i * 2.1; p.set(Math.cos(a) * (0.8 + i * 0.15), 0.3 + ((t * 0.6 + i * 0.6) % 2.2 + 2.2) % 2.2, Math.sin(a) * (0.8 + i * 0.15)); w.push(p, t); });
    shards.forEach((s) => { const d = s.userData; const a = d.a + t * d.sp; s.position.set(Math.cos(a) * d.r, d.h + Math.sin(t * 2 + d.a) * 0.1, Math.sin(a) * d.r); s.rotation.set(t * 3 + d.a, a, Math.PI / 2); });
    if (t > nextFlash) { flash.intensity = 25; nextFlash = t + 2 + Math.random() * 3; }
    flash.intensity *= Math.pow(0.002, dt);
    ring.rotation.z = -t * 0.4;
  });
  return c;
}
