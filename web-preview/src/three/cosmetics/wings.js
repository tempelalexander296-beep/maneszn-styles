import * as THREE from 'three';
import { M } from '../materials.js';
import { snowflakeGeometry, sweepGeometry, candyCane, giftBox, bowMesh, lerp, rand } from '../geometry.js';
import { ParticleSystem } from '../particles.js';
import { sparkleSprite, flakeSprite, peppermintTexture } from '../textures.js';
import { cosmetic } from './base.js';

/* Wing arm (humerus → hand) in local wing space: X outward, Y up. */
const armCurve = (span = 1) => new THREE.CatmullRomCurve3([
  new THREE.Vector3(0, 0, 0), new THREE.Vector3(0.35 * span, 0.32 * span, -0.04), new THREE.Vector3(0.8 * span, 0.55 * span, -0.1), new THREE.Vector3(1.25 * span, 0.42 * span, -0.14),
]);

/** Mirrored pair of wings with a shared flap animation. */
function wingPair(c, buildWing, { flap = 0.18, speed = 1.6, sweep = 0.38, spread = 0.07 } = {}) {
  const wings = [];
  for (const side of [1, -1]) {
    const pivot = new THREE.Group();
    pivot.position.x = side * spread;
    const w = buildWing(side);
    pivot.add(w);
    pivot.scale.x = side;
    c.root.add(pivot);
    wings.push(pivot);
  }
  c.on((t) => {
    const f = Math.sin(t * speed) * flap;
    wings.forEach((p) => { p.rotation.y = (sweep + f) * Math.sign(p.scale.x); p.rotation.z = Math.cos(t * speed) * 0.04 * Math.sign(p.scale.x); });
  });
  return wings;
}

/* ------------------------------------------------------------- Day 1: Snowflake Wings */
export function snowflakeWings() {
  const c = cosmetic('back');
  const flakeMat = new THREE.MeshPhysicalMaterial({ color: '#eaf7ff', metalness: 0.25, roughness: 0.12, clearcoat: 1, emissive: '#5fb8ff', emissiveIntensity: 1.1, iridescence: 0.6, transmission: 0.25, thickness: 0.1 });
  const coreMat = M.glow('#bfe6ff', 5);
  const boneMat = M.ice({ emissiveIntensity: 0.6 });
  wingPair(c, () => {
    const g = new THREE.Group();
    const arm = armCurve(1);
    g.add(new THREE.Mesh(sweepGeometry(arm, (t) => lerp(0.04, 0.012, t), 48, 10), boneMat));
    // three finger bones fanning downward, snowflakes along them like feathers
    const fingers = [[1.0, -1.05, 1.0], [0.72, -1.25, 0.85], [0.45, -1.15, 0.7]];
    let seed = 3;
    for (const [at, ang, len] of fingers) {
      const root = arm.getPointAt(at);
      const dir = new THREE.Vector3(Math.cos(ang), Math.sin(ang), 0);
      const end = root.clone().addScaledVector(dir, len);
      const mid = root.clone().lerp(end, 0.5).add(new THREE.Vector3(0.08, 0.05, -0.05));
      const finger = new THREE.QuadraticBezierCurve3(root, mid, end);
      g.add(new THREE.Mesh(sweepGeometry(finger, (t) => lerp(0.018, 0.006, t), 24, 8), boneMat));
      const n = 5;
      for (let i = 0; i < n; i++) {
        const t = 0.2 + (i / (n - 1)) * 0.8;
        const f = new THREE.Mesh(snowflakeGeometry((seed++) % 3, 0.05), flakeMat);
        f.position.copy(finger.getPointAt(t));
        f.scale.setScalar(lerp(0.17, 0.08, t) * (at > 0.9 ? 1.2 : 1));
        f.rotation.set(0.1, 0.2, t * 2 + at);
        f.castShadow = true;
        g.add(f);
        const core = new THREE.Mesh(new THREE.IcosahedronGeometry(0.018, 1), coreMat);
        core.position.copy(f.position); g.add(core);
        f.userData.spin = 0.2 + (i % 3) * 0.15;
      }
    }
    // large snowflakes along the arm (coverts)
    for (let i = 0; i < 4; i++) {
      const f = new THREE.Mesh(snowflakeGeometry(i % 3, 0.05), flakeMat);
      f.position.copy(arm.getPointAt(0.25 + i * 0.22)).add(new THREE.Vector3(0, -0.12, 0.02));
      f.scale.setScalar(0.13 + i * 0.01); f.rotation.z = i;
      f.userData.spin = 0.3; g.add(f);
    }
    c.on((t, dt) => g.children.forEach((m) => { if (m.userData.spin) m.rotation.z += m.userData.spin * dt; }));
    return g;
  }, { flap: 0.14, speed: 1.3 });
  const p = c.track(new ParticleSystem({
    max: 500, map: flakeSprite(), rate: 70, life: [1.5, 3], size: [0.03, 0.07], color: ['#cfeeff', '#ffffff', '#8fd3ff'],
    gravity: [0, -0.15, 0], spin: [-1.5, 1.5], fadeOut: 0.6, intensity: 2,
    spawn: (P, V) => { const s = Math.random() < 0.5 ? 1 : -1; P.set(s * (0.2 + Math.random() * 1.1), -0.6 + Math.random() * 1.2, -0.3 - Math.random() * 0.3); V.set(s * 0.05, -0.05, -0.05); },
    prewarm: 2,
  }));
  c.root.add(p);
  c.on((t, dt) => p.tick(dt));
  return c;
}

/* ------------------------------------------------------------- Day 8: Candy Cane Wings */
export function candyCaneWings() {
  const c = cosmetic('back');
  const pepperMat = new THREE.MeshPhysicalMaterial({ map: peppermintTexture(), roughness: 0.15, clearcoat: 1 });
  wingPair(c, () => {
    const g = new THREE.Group();
    const arm = armCurve(1.05);
    g.add(new THREE.Mesh(sweepGeometry(arm, (t) => lerp(0.045, 0.02, t), 48, 12), M.candy('#c8102e', null, 18)));
    // primary + secondary canes hanging from the arm with hooks curling outward
    const rows = [{ n: 9, len: [1.05, 0.55], r: 0.03, z: 0 }, { n: 7, len: [0.55, 0.32], r: 0.026, z: 0.05 }];
    rows.forEach((row, ri) => {
      for (let i = 0; i < row.n; i++) {
        const t = i / (row.n - 1);
        const cane = candyCane(lerp(row.len[1], row.len[0], t), row.r, ri === 0 ? '#c8102e' : '#e0263f');
        cane.position.copy(arm.getPointAt(lerp(0.15, 1, t))).add(new THREE.Vector3(0, 0, row.z));
        cane.rotation.z = lerp(-2.95, -1.85, t); // hook sits at the far tip
        cane.rotation.y = ri * 0.15;
        g.add(cane);
      }
    });
    for (let i = 0; i < 3; i++) {
      const disc = new THREE.Mesh(new THREE.CylinderGeometry(0.1 - i * 0.015, 0.1 - i * 0.015, 0.04, 48), [M.plastic('#fbf7f0', { clearcoat: 1 }), pepperMat, pepperMat]);
      disc.rotation.x = Math.PI / 2;
      disc.position.copy(arm.getPointAt(0.12 + i * 0.4)).add(new THREE.Vector3(0, 0, 0.06));
      g.add(disc);
      c.on((tt) => { disc.rotation.y = tt * (0.8 + i * 0.3); });
    }
    return g;
  }, { flap: 0.2, speed: 1.8 });
  const p = c.track(new ParticleSystem({
    max: 300, map: sparkleSprite(), rate: 45, life: [0.8, 1.6], size: [0.04, 0.09], color: ['#ff4d6a', '#ffffff', '#ffd36b'],
    gravity: [0, -0.3, 0], spin: [-2, 2], intensity: 2.2,
    spawn: (P, V) => { const s = Math.random() < 0.5 ? 1 : -1; P.set(s * (0.3 + Math.random() * 1.1), -0.8 + Math.random() * 1.3, -0.35); V.set(0, -0.1, -0.1); },
    prewarm: 1.5,
  }));
  c.root.add(p); c.on((t, dt) => p.tick(dt));
  return c;
}

/* ------------------------------------------------------------- Day 16: Frozen Wings */
function shardShape(len, w) {
  const s = new THREE.Shape();
  s.moveTo(0, 0);
  s.bezierCurveTo(w * 0.9, len * 0.15, w, len * 0.55, w * 0.2, len * 0.95);
  s.lineTo(0, len); s.lineTo(-w * 0.15, len * 0.9);
  s.bezierCurveTo(-w * 0.6, len * 0.5, -w * 0.5, len * 0.15, 0, 0);
  return s;
}

export function frozenWings() {
  const c = cosmetic('back');
  const ice = M.ice({ iridescence: 1, iridescenceThicknessRange: [200, 800], transmission: 0.96, thickness: 0.15, emissiveIntensity: 0.25, side: THREE.DoubleSide });
  const vein = M.glow('#7fd4ff', 4);
  const veinPulse = [];
  wingPair(c, () => {
    const g = new THREE.Group();
    const shards = [
      { len: 1.55, w: 0.32, a: -0.75, at: [0.05, 0.05] }, { len: 1.35, w: 0.28, a: -1.15, at: [0.04, 0] },
      { len: 1.1, w: 0.26, a: -1.6, at: [0.03, -0.05] }, { len: 0.95, w: 0.22, a: -2.05, at: [0.02, -0.1] },
      { len: 0.8, w: 0.2, a: -2.45, at: [0.02, -0.15] }, { len: 0.75, w: 0.16, a: -0.35, at: [0.06, 0.08] },
    ];
    shards.forEach((s, i) => {
      const geo = new THREE.ExtrudeGeometry(shardShape(s.len, s.w), { depth: 0.012, bevelEnabled: true, bevelThickness: 0.012, bevelSize: 0.012, bevelSegments: 3, curveSegments: 24 });
      const m = new THREE.Mesh(geo, ice);
      m.position.set(s.at[0], s.at[1], -i * 0.012);
      m.rotation.z = s.a; m.rotation.y = 0.12 * i;
      m.castShadow = true;
      g.add(m);
      // glowing central vein with branches
      const veinCurve = new THREE.CatmullRomCurve3([new THREE.Vector3(0, 0.05, 0.02), new THREE.Vector3(s.w * 0.15, s.len * 0.45, 0.02), new THREE.Vector3(0.02, s.len * 0.9, 0.02)]);
      const v = new THREE.Mesh(sweepGeometry(veinCurve, (t) => lerp(0.008, 0.002, t), 30, 6), vein);
      m.add(v);
      for (let k = 1; k <= 3; k++) {
        const p0 = veinCurve.getPointAt(k * 0.25);
        const br = new THREE.CatmullRomCurve3([p0, p0.clone().add(new THREE.Vector3(s.w * 0.45, s.len * 0.12, 0))]);
        m.add(new THREE.Mesh(sweepGeometry(br, (t) => lerp(0.004, 0.001, t), 8, 5), vein));
      }
      veinPulse.push(m);
    });
    // crystalline shoulder clusters
    for (let i = 0; i < 5; i++) {
      const cr = new THREE.Mesh(new THREE.OctahedronGeometry(0.06 + i * 0.012, 0), ice);
      cr.scale.y = 2.4; cr.position.set(0.05 + i * 0.05, 0.08 + i * 0.03, 0.04); cr.rotation.z = -0.4 - i * 0.2;
      g.add(cr);
    }
    return g;
  }, { flap: 0.1, speed: 1.0, sweep: 0.3 });
  c.on((t) => { vein.emissiveIntensity = 3 + Math.sin(t * 2.2) * 1.5; });
  const mist = c.track(new ParticleSystem({
    max: 600, rate: 110, life: [1.5, 3], size: [0.08, 0.22], color: ['#9fdcff', '#d9f3ff', '#6bb8ff'], sizeEnd: 2.2,
    gravity: [0, -0.06, 0], drag: 0.4, fadeIn: 0.2, fadeOut: 0.6, intensity: 0.7,
    spawn: (P, V) => { const s = Math.random() < 0.5 ? 1 : -1; const r = Math.random(); P.set(s * (0.15 + r * 1.2), 0.4 - r * 1.3 + Math.random() * 0.3, -0.2 - r * 0.45); V.set(s * 0.04, -0.04, -0.06); },
    prewarm: 2.5,
  }));
  const glints = c.track(new ParticleSystem({
    max: 150, map: sparkleSprite(), rate: 25, life: [0.4, 1], size: [0.05, 0.12], color: ['#ffffff', '#bfe9ff'], intensity: 3,
    spawn: (P) => { const s = Math.random() < 0.5 ? 1 : -1; const r = Math.random(); P.set(s * (0.15 + r * 1.2), 0.5 - r * 1.2, -0.15 - r * 0.45); }, prewarm: 1,
  }));
  c.root.add(mist, glints);
  c.on((t, dt) => { mist.tick(dt); glints.tick(dt); });
  return c;
}

/* ------------------------------------------------------------- Day 19: Golden Gift Wings */
function featherGeometry(len = 1, w = 0.12) {
  const s = new THREE.Shape();
  s.moveTo(-w * 0.15, 0);
  s.bezierCurveTo(-w * 0.9, len * 0.25, -w * 0.8, len * 0.8, 0, len);
  s.bezierCurveTo(w * 0.55, len * 0.78, w * 0.7, len * 0.25, w * 0.15, 0);
  s.lineTo(-w * 0.15, 0);
  const g = new THREE.ExtrudeGeometry(s, { depth: 0.008, bevelEnabled: true, bevelThickness: 0.01, bevelSize: 0.008, bevelSegments: 2, curveSegments: 16 });
  // gentle curvature + barb ripples
  const p = g.attributes.position;
  for (let i = 0; i < p.count; i++) {
    const x = p.getX(i), y = p.getY(i);
    p.setZ(i, p.getZ(i) - Math.pow(y / len, 2) * 0.08 + Math.sin(y * 70) * 0.002 * Math.abs(x / w));
  }
  g.computeVertexNormals();
  return g;
}

export function goldenGiftWings() {
  const c = cosmetic('back');
  const gold = M.gold({ roughness: 0.18 });
  const rose = M.roseGold({ roughness: 0.25, clearcoat: 0.6 });
  const pale = M.gold({ color: new THREE.Color('#ffe8a6'), roughness: 0.3 });
  const edgeGlow = M.glow('#ffd36b', 2.2);
  const r = rand(19);
  const gifts = [];
  wingPair(c, () => {
    const g = new THREE.Group();
    const arm = armCurve(1.1);
    g.add(new THREE.Mesh(sweepGeometry(arm, (t) => lerp(0.05, 0.022, t), 48, 12), gold));
    const rows = [
      { n: 12, len: [1.25, 0.62], w: 0.13, mat: gold, z: 0, from: 0.12 },
      { n: 10, len: [0.62, 0.36], w: 0.11, mat: rose, z: 0.035, from: 0.1 },
      { n: 8, len: [0.32, 0.2], w: 0.09, mat: pale, z: 0.07, from: 0.08 },
    ];
    rows.forEach((row) => {
      for (let i = 0; i < row.n; i++) {
        const t = i / (row.n - 1);
        const len = lerp(row.len[1], row.len[0], t);
        const f = new THREE.Mesh(featherGeometry(len, row.w), row.mat);
        f.position.copy(arm.getPointAt(lerp(row.from, 1, t))).add(new THREE.Vector3(0, 0, row.z));
        f.rotation.z = lerp(-2.9, -1.75, t);
        f.rotation.y = (r() - 0.5) * 0.08;
        f.castShadow = true;
        g.add(f);
        if (row.mat === gold) {
          const rach = new THREE.Mesh(new THREE.CylinderGeometry(0.004, 0.008, len * 0.92, 6), edgeGlow);
          rach.position.y = len * 0.46; rach.position.z = 0.02; f.add(rach);
        }
      }
    });
    // gifts dangling from primaries on ribbons
    const palettes = [['red', 'stars', '#ffd36b'], ['green', 'dots', '#ffd36b'], ['white', 'snowflakes', '#c8102e']];
    [0.55, 0.78, 1].forEach((t, i) => {
      const anchor = arm.getPointAt(t);
      const hang = new THREE.Group(); hang.position.copy(anchor).add(new THREE.Vector3(0, 0, 0.12));
      const len = 0.35 + i * 0.12;
      const ribbon = new THREE.Mesh(new THREE.CylinderGeometry(0.004, 0.004, len, 6), M.satin('#c8102e'));
      ribbon.position.y = -len / 2; hang.add(ribbon);
      const [pal, motif, rib] = palettes[i];
      const box = giftBox({ w: 0.13, h: 0.13, d: 0.13, palette: pal, motif, ribbon: rib, ribbonMetal: true });
      box.position.y = -len - 0.12; hang.add(box);
      g.add(hang); gifts.push({ hang, phase: i * 1.3 });
    });
    const bow = bowMesh(0.2, M.satin('#c8102e')); bow.position.set(0.05, 0.05, 0.12); g.add(bow);
    return g;
  }, { flap: 0.16, speed: 1.4, sweep: 0.35 });
  c.on((t) => gifts.forEach(({ hang, phase }) => { hang.rotation.z = Math.sin(t * 2 + phase) * 0.18; hang.rotation.x = Math.cos(t * 1.7 + phase) * 0.1; }));
  const dust = c.track(new ParticleSystem({
    max: 700, map: sparkleSprite(), rate: 120, life: [1, 2.2], size: [0.03, 0.09], color: ['#ffd36b', '#fff1c2', '#ffb52e'], intensity: 2.4,
    gravity: [0, -0.25, 0], spin: [-2, 2], fadeOut: 0.5,
    spawn: (P, V) => { const s = Math.random() < 0.5 ? 1 : -1; const k = Math.random(); P.set(s * (0.2 + k * 1.3), 0.5 - k * 1.5 + Math.random() * 0.2, -0.25 - k * 0.5); V.set(s * 0.03, -0.1, -0.08); },
    prewarm: 2,
  }));
  c.root.add(dust); c.on((t, dt) => dust.tick(dt));
  return c;
}
