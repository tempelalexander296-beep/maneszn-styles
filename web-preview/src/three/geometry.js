import * as THREE from 'three';
import { RoundedBoxGeometry } from 'three/addons/geometries/RoundedBoxGeometry.js';
import { mergeGeometries } from 'three/addons/utils/BufferGeometryUtils.js';
import { M } from './materials.js';

/** Shared procedural geometry builders. Heavy geometries are cached. */
const cache = new Map();
const cached = (key, fn) => { if (!cache.has(key)) cache.set(key, fn()); return cache.get(key); };

export const rand = (seed = 1) => () => ((seed = (seed * 16807) % 2147483647) / 2147483647);
export const lerp = (a, b, t) => a + (b - a) * t;
export const TAU = Math.PI * 2;

/* ------------------------------------------------------------------ snowflake */
export function snowflakeGeometry(variant = 0, depth = 0.06) {
  return cached(`flake-${variant}-${depth}`, () => {
    const parts = [];
    const armW = 0.055, armL = 1;
    const branchSets = [[0.38, 0.32], [0.62, 0.24], [0.82, 0.14]];
    const ext = { depth, bevelEnabled: true, bevelThickness: depth * 0.45, bevelSize: 0.018, bevelSegments: 3, curveSegments: 4 };
    for (let i = 0; i < 6; i++) {
      const a = (i / 6) * TAU;
      const arm = new THREE.Shape();
      arm.moveTo(-armW, 0); arm.lineTo(-armW * 0.55, armL); arm.lineTo(0, armL + 0.09); arm.lineTo(armW * 0.55, armL); arm.lineTo(armW, 0);
      const g = new THREE.ExtrudeGeometry(arm, ext); g.rotateZ(a - Math.PI / 2); parts.push(g);
      branchSets.forEach(([t, len], bi) => {
        const L = len * (variant % 2 ? 1.15 : 1) * (bi === 1 && variant === 2 ? 1.3 : 1);
        for (const s of [-1, 1]) {
          const b = new THREE.Shape();
          b.moveTo(-0.032, 0); b.lineTo(-0.02, L); b.lineTo(0, L + 0.05); b.lineTo(0.02, L); b.lineTo(0.032, 0);
          const bg = new THREE.ExtrudeGeometry(b, ext);
          bg.rotateZ(s * (variant === 1 ? 0.9 : 0.78));
          bg.translate(0, t * armL, 0);
          bg.rotateZ(a - Math.PI / 2);
          parts.push(bg);
        }
      });
    }
    const hex = new THREE.Shape();
    for (let i = 0; i < 6; i++) { const a = (i / 6) * TAU; hex[i ? 'lineTo' : 'moveTo'](Math.cos(a) * 0.17, Math.sin(a) * 0.17); }
    parts.push(new THREE.ExtrudeGeometry(hex, ext));
    const g = mergeGeometries(parts.map((p) => (p.index ? p.toNonIndexed() : p)));
    g.translate(0, 0, -depth / 2); g.computeVertexNormals();
    return g;
  });
}

/* ------------------------------------------------------------------ star */
export function starGeometry(points = 5, outer = 1, inner = 0.45, depth = 0.25) {
  return cached(`star-${points}-${outer}-${inner}-${depth}`, () => {
    // faceted 3D star: double pyramid per point
    const pos = [];
    const front = new THREE.Vector3(0, 0, depth), back = new THREE.Vector3(0, 0, -depth);
    const ring = [];
    for (let i = 0; i < points * 2; i++) {
      const r = i % 2 ? inner : outer, a = (i / (points * 2)) * TAU + Math.PI / 2;
      ring.push(new THREE.Vector3(Math.cos(a) * r, Math.sin(a) * r, 0));
    }
    for (let i = 0; i < ring.length; i++) {
      const a = ring[i], b = ring[(i + 1) % ring.length];
      pos.push(...front.toArray(), ...a.toArray(), ...b.toArray());
      pos.push(...back.toArray(), ...b.toArray(), ...a.toArray());
    }
    const g = new THREE.BufferGeometry();
    g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
    g.computeVertexNormals();
    return g;
  });
}

/* ------------------------------------------------------------------ variable radius sweep */
/**
 * Sweep a circle with radius(t) along a curve. Like TubeGeometry, but tapered.
 * radius(t) → number | {x,y} for elliptical cross sections.
 */
export function sweepGeometry(curve, radius, tubular = 64, radial = 16, closedEnds = true) {
  const frames = curve.computeFrenetFrames(tubular, false);
  const pos = [], nor = [], uv = [], idx = [];
  const P = new THREE.Vector3(), N = new THREE.Vector3();
  for (let i = 0; i <= tubular; i++) {
    const t = i / tubular;
    curve.getPointAt(t, P);
    const r = radius(t); const rx = typeof r === 'number' ? r : r.x, ry = typeof r === 'number' ? r : r.y;
    const n = frames.normals[i], b = frames.binormals[i];
    for (let j = 0; j <= radial; j++) {
      const v = (j / radial) * TAU, s = Math.sin(v), c = -Math.cos(v);
      N.set(c * n.x * ry + s * b.x * rx, c * n.y * ry + s * b.y * rx, c * n.z * ry + s * b.z * rx);
      pos.push(P.x + N.x, P.y + N.y, P.z + N.z);
      N.normalize(); nor.push(N.x, N.y, N.z);
      uv.push(t, j / radial);
    }
  }
  for (let i = 0; i < tubular; i++) for (let j = 0; j < radial; j++) {
    const a = i * (radial + 1) + j, b = (i + 1) * (radial + 1) + j;
    idx.push(a, b, a + 1, b, b + 1, a + 1);
  }
  if (closedEnds) {
    for (const [i, flip] of [[0, true], [tubular, false]]) {
      const t = i / tubular; curve.getPointAt(t, P);
      const center = pos.length / 3;
      const tan = curve.getTangentAt(t).multiplyScalar(flip ? -1 : 1);
      pos.push(P.x, P.y, P.z); nor.push(tan.x, tan.y, tan.z); uv.push(t, 0.5);
      for (let j = 0; j < radial; j++) {
        const a = i * (radial + 1) + j;
        flip ? idx.push(center, a + 1, a) : idx.push(center, a, a + 1);
      }
    }
  }
  const g = new THREE.BufferGeometry();
  g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3));
  g.setAttribute('normal', new THREE.Float32BufferAttribute(nor, 3));
  g.setAttribute('uv', new THREE.Float32BufferAttribute(uv, 2));
  g.setIndex(idx);
  return g;
}

/* ------------------------------------------------------------------ candy cane */
export function candyCaneCurve(length = 1, hook = 0.22) {
  const pts = [];
  for (let i = 0; i <= 10; i++) pts.push(new THREE.Vector3(0, (i / 10) * length, 0));
  for (let i = 1; i <= 14; i++) {
    const a = (i / 14) * Math.PI * 1.08;
    pts.push(new THREE.Vector3(hook - Math.cos(a) * hook, length + Math.sin(a) * hook, 0));
  }
  return new THREE.CatmullRomCurve3(pts, false, 'centripetal');
}

export function candyCane(length = 1, radius = 0.05, stripe = '#c8102e') {
  const curve = candyCaneCurve(length, length * 0.22);
  const g = sweepGeometry(curve, () => radius, 120, 20);
  const mesh = new THREE.Mesh(g, M.candy(stripe, '#2f8f46', length * 9));
  mesh.castShadow = true;
  return mesh;
}

/* ------------------------------------------------------------------ gift box */
export function giftBox({ w = 1, h = 1, d = 1, palette = 'red', motif = 'stars', ribbon = '#ffd36b', bow = true, ribbonMetal = false } = {}) {
  const grp = new THREE.Group();
  const paper = M.paper(palette, motif, Math.max(w, h, d) * 1.2);
  const r = Math.min(w, h, d) * 0.06;
  const body = new THREE.Mesh(new RoundedBoxGeometry(w, h * 0.82, d, 3, r), paper);
  body.position.y = h * 0.41;
  const lid = new THREE.Mesh(new RoundedBoxGeometry(w * 1.06, h * 0.2, d * 1.06, 3, r), paper);
  lid.position.y = h * 0.86;
  const rib = ribbonMetal ? M.gold({ color: new THREE.Color(ribbon) }) : M.satin(ribbon);
  const bw = Math.min(w, d) * 0.16;
  const r1 = new THREE.Mesh(new THREE.BoxGeometry(bw, h * 1.0, d * 1.08), rib); r1.position.y = h * 0.48;
  const r2 = new THREE.Mesh(new THREE.BoxGeometry(w * 1.08, h * 1.0, bw), rib); r2.position.y = h * 0.48;
  grp.add(body, lid, r1, r2);
  if (bow) {
    const b = bowMesh(Math.min(w, d) * 0.32, rib);
    b.position.y = h * 0.97; grp.add(b);
  }
  grp.traverse((o) => { if (o.isMesh) { o.castShadow = true; o.receiveShadow = true; } });
  grp.userData.lid = lid;
  return grp;
}

export function bowMesh(size = 0.3, material = M.satin('#c8102e')) {
  const grp = new THREE.Group();
  const loopGeo = cached('bowloop', () => {
    const g = new THREE.TorusGeometry(0.5, 0.16, 16, 48);
    g.scale(1, 0.55, 0.35); return g;
  });
  for (const s of [-1, 1]) {
    const l = new THREE.Mesh(loopGeo, material);
    l.position.set(s * 0.45, 0.22, 0); l.rotation.z = s * 0.45; l.rotation.y = s * 0.25;
    grp.add(l);
    const tail = new THREE.Mesh(new THREE.BoxGeometry(0.22, 0.75, 0.05), material);
    tail.position.set(s * 0.28, -0.1, 0.15); tail.rotation.set(0.6, 0, s * 0.5);
    grp.add(tail);
  }
  const knot = new THREE.Mesh(new THREE.SphereGeometry(0.2, 24, 16), material); knot.scale.set(1, 0.85, 0.8);
  knot.position.y = 0.2;
  grp.add(knot);
  grp.scale.setScalar(size);
  return grp;
}

/* ------------------------------------------------------------------ christmas tree */
export function christmasTree({ height = 1, seed = 3, ornaments = true, snow = true, lights = true } = {}) {
  const grp = new THREE.Group();
  const r = rand(seed);
  const needle = new THREE.MeshPhysicalMaterial({ color: '#0d4d2a', roughness: 0.55, sheen: 0.6, sheenColor: new THREE.Color('#3aa05f'), flatShading: true });
  const snowMat = M.snow();
  const tiers = 5;
  for (let i = 0; i < tiers; i++) {
    const t = i / tiers;
    const radius = height * (0.42 - t * 0.065) * (1 - t * 0.35);
    const h = height * 0.34;
    const cone = new THREE.ConeGeometry(radius, h, 22, 3, true);
    // jagged branch tips
    const p = cone.attributes.position;
    for (let k = 0; k < p.count; k++) {
      const y = p.getY(k);
      if (y < -h * 0.45) {
        const ang = Math.atan2(p.getZ(k), p.getX(k));
        const j = 1 + 0.18 * Math.sin(ang * 11 + i) + (r() - 0.5) * 0.12;
        p.setX(k, p.getX(k) * j); p.setZ(k, p.getZ(k) * j); p.setY(k, y - (r() * 0.06 * height));
      }
    }
    cone.computeVertexNormals();
    const m = new THREE.Mesh(cone, needle);
    m.position.y = height * (0.22 + t * 0.15) + h * 0.5;
    m.castShadow = true; m.receiveShadow = true;
    grp.add(m);
    if (snow) {
      const cap = new THREE.Mesh(new THREE.ConeGeometry(radius * 0.72, h * 0.35, 22, 1, true), snowMat);
      cap.position.y = m.position.y + h * 0.33;
      grp.add(cap);
    }
  }
  const trunk = new THREE.Mesh(new THREE.CylinderGeometry(height * 0.05, height * 0.065, height * 0.25, 12), M.matte('#4a2c19', { roughness: 0.9 }));
  trunk.position.y = height * 0.12; grp.add(trunk);
  const star = new THREE.Mesh(starGeometry(5, height * 0.085, height * 0.038, height * 0.025), M.glow('#ffd36b', 4));
  star.position.y = height * 1.06; grp.add(star);
  grp.userData.star = star;
  if (ornaments) {
    const colors = ['#c8102e', '#ffd36b', '#cfe8ff', '#e0e0e0'];
    const geo = new THREE.SphereGeometry(height * 0.03, 16, 12);
    for (let i = 0; i < 18; i++) {
      const t = 0.12 + r() * 0.75, a = r() * TAU;
      const rad = height * 0.4 * (1 - t) * 0.95;
      const o = new THREE.Mesh(geo, M.ornament(colors[i % colors.length]));
      o.position.set(Math.cos(a) * rad, height * (0.28 + t * 0.7), Math.sin(a) * rad);
      grp.add(o);
    }
  }
  if (lights) {
    const pts = [];
    for (let i = 0; i <= 120; i++) {
      const t = i / 120, a = t * TAU * 5;
      const rad = height * 0.41 * (1 - t) + 0.01;
      pts.push(new THREE.Vector3(Math.cos(a) * rad, height * (0.27 + t * 0.72), Math.sin(a) * rad));
    }
    const curve = new THREE.CatmullRomCurve3(pts);
    const bulbs = lightString(curve, 40, ['#ffcf6b', '#ff6b6b', '#8fd3ff', '#9effa8'], height * 0.012);
    grp.add(bulbs);
    grp.userData.lights = bulbs;
  }
  return grp;
}

/* ------------------------------------------------------------------ light string (instanced, blinking) */
export function lightString(curve, count = 40, palette = ['#ffd36b'], size = 0.02, wire = true) {
  const grp = new THREE.Group();
  const geo = new THREE.SphereGeometry(size, 10, 8);
  const mat = new THREE.MeshStandardMaterial({ color: '#000', emissive: '#ffffff', emissiveIntensity: 3.5, roughness: 0.4 });
  const inst = new THREE.InstancedMesh(geo, mat, count);
  const m = new THREE.Matrix4(), c = new THREE.Color();
  const base = [];
  for (let i = 0; i < count; i++) {
    const p = curve.getPointAt(i / count);
    m.makeTranslation(p.x, p.y, p.z);
    inst.setMatrixAt(i, m);
    c.set(palette[i % palette.length]); base.push(c.clone());
    inst.setColorAt(i, c);
  }
  grp.add(inst);
  if (wire) {
    const w = new THREE.Mesh(new THREE.TubeGeometry(curve, count * 4, size * 0.18, 4, false), M.matte('#0b2416'));
    grp.add(w);
  }
  grp.userData.update = (t) => {
    for (let i = 0; i < count; i++) {
      const k = 0.35 + 0.65 * (0.5 + 0.5 * Math.sin(t * 2.4 + i * 1.7));
      c.copy(base[i]).multiplyScalar(k);
      inst.setColorAt(i, c);
    }
    inst.instanceColor.needsUpdate = true;
  };
  return grp;
}

/* ------------------------------------------------------------------ bell */
export function bellGeometry() {
  return cached('bell', () => {
    const pts = [];
    const prof = [[0, 1.0], [0.12, 0.99], [0.22, 0.94], [0.28, 0.82], [0.31, 0.62], [0.36, 0.4], [0.46, 0.2], [0.58, 0.06], [0.62, 0.0], [0.6, -0.03], [0.54, 0.0], [0.42, 0.12], [0.32, 0.36], [0.27, 0.6], [0.24, 0.8], [0.17, 0.9], [0, 0.92]];
    for (const [x, y] of prof) pts.push(new THREE.Vector2(x, y));
    const g = new THREE.LatheGeometry(pts, 64);
    g.computeVertexNormals();
    return g;
  });
}

export function bell(material = M.gold()) {
  const grp = new THREE.Group();
  const b = new THREE.Mesh(bellGeometry(), material); b.castShadow = true;
  const loop = new THREE.Mesh(new THREE.TorusGeometry(0.09, 0.03, 12, 32), material); loop.position.y = 1.04;
  const clapper = new THREE.Mesh(new THREE.SphereGeometry(0.1, 20, 14), material); clapper.position.y = 0.06;
  const rim = new THREE.Mesh(new THREE.TorusGeometry(0.6, 0.025, 12, 64), material); rim.rotation.x = Math.PI / 2; rim.position.y = -0.005;
  grp.add(b, loop, clapper, rim);
  grp.userData.clapper = clapper;
  return grp;
}

/* ------------------------------------------------------------------ ornament */
export function ornamentBall(color = '#c8102e', r = 0.1) {
  const grp = new THREE.Group();
  const ball = new THREE.Mesh(new THREE.SphereGeometry(r, 32, 24), M.ornament(color));
  const cap = new THREE.Mesh(new THREE.CylinderGeometry(r * 0.25, r * 0.28, r * 0.25, 16), M.gold());
  cap.position.y = r * 1.0;
  const loop = new THREE.Mesh(new THREE.TorusGeometry(r * 0.12, r * 0.03, 8, 20), M.gold()); loop.position.y = r * 1.2;
  grp.add(ball, cap, loop);
  return grp;
}

/* ------------------------------------------------------------------ holly */
export function hollyGeometry() {
  return cached('holly', () => {
    const s = new THREE.Shape();
    s.moveTo(0, 0);
    const pts = [[0.12, 0.1], [0.2, 0.08], [0.18, 0.22], [0.26, 0.28], [0.17, 0.38], [0.2, 0.52], [0.08, 0.5], [0, 0.66]];
    for (const [x, y] of pts) s.lineTo(x, y);
    for (let i = pts.length - 2; i >= 0; i--) s.lineTo(-pts[i][0], pts[i][1]);
    s.lineTo(0, 0);
    const g = new THREE.ExtrudeGeometry(s, { depth: 0.01, bevelEnabled: true, bevelThickness: 0.008, bevelSize: 0.008, bevelSegments: 2 });
    // curl the leaf
    const p = g.attributes.position;
    for (let i = 0; i < p.count; i++) { const x = p.getX(i); p.setZ(i, p.getZ(i) + x * x * 1.6); }
    g.computeVertexNormals();
    return g;
  });
}

export function hollySprig(scale = 1) {
  const grp = new THREE.Group();
  const leaf = M.leaf();
  for (const a of [-0.7, 0.7, 0]) {
    const l = new THREE.Mesh(hollyGeometry(), leaf);
    l.rotation.z = a; l.rotation.x = -0.3;
    if (a === 0) l.rotation.z = Math.PI;
    grp.add(l);
  }
  const bg = new THREE.SphereGeometry(0.045, 16, 12);
  [[0, 0.02, 0.05], [0.05, -0.02, 0.04], [-0.05, -0.02, 0.04]].forEach(([x, y, z]) => {
    const b = new THREE.Mesh(bg, M.berry()); b.position.set(x, y, z); grp.add(b);
  });
  grp.scale.setScalar(scale);
  return grp;
}

/* ------------------------------------------------------------------ crystal */
export function crystalGeometry(sides = 6, height = 1, radius = 0.15, tip = 0.3) {
  return cached(`crystal-${sides}-${height}-${radius}-${tip}`, () => {
    const pts = [new THREE.Vector2(0, -tip * 0.4), new THREE.Vector2(radius * 0.8, 0), new THREE.Vector2(radius, height * 0.1), new THREE.Vector2(radius * 0.9, height), new THREE.Vector2(0, height + tip)];
    const g = new THREE.LatheGeometry(pts, sides);
    g.computeVertexNormals();
    return g.toNonIndexed();
  });
}

/* ------------------------------------------------------------------ antler */
/** Recursive branching antler made of tapered sweeps. Returns { group, tips } */
export function antler({ scale = 1, seed = 5, material = M.matte('#6b4428', { roughness: 0.6 }), mirror = 1 } = {}) {
  const grp = new THREE.Group();
  const r = rand(seed);
  const tips = [];
  const branch = (start, dir, len, rad, depth) => {
    const pts = [start.clone()];
    const d = dir.clone();
    let p = start.clone();
    for (let i = 1; i <= 5; i++) {
      d.applyAxisAngle(new THREE.Vector3(0, 0, 1), -0.12 * mirror).normalize();
      d.y += 0.05;
      p = p.clone().addScaledVector(d.clone().normalize(), len / 5);
      pts.push(p);
    }
    const curve = new THREE.CatmullRomCurve3(pts);
    const geo = sweepGeometry(curve, (t) => lerp(rad, rad * 0.35, t), 24, 10);
    const m = new THREE.Mesh(geo, material); m.castShadow = true; grp.add(m);
    if (depth <= 0) { tips.push(p.clone()); return; }
    const count = depth === 2 ? 3 : 2;
    for (let i = 0; i < count; i++) {
      const t = 0.35 + (i / count) * 0.55;
      const at = curve.getPointAt(t);
      const nd = curve.getTangentAt(t).clone();
      nd.applyAxisAngle(new THREE.Vector3(0, 0, 1), (0.7 + r() * 0.3) * mirror);
      nd.z += (r() - 0.5) * 0.6;
      branch(at, nd.normalize(), len * (0.45 + r() * 0.15), rad * 0.6, depth - 1);
    }
    tips.push(p.clone());
  };
  branch(new THREE.Vector3(0, 0, 0), new THREE.Vector3(0.35 * mirror, 1, -0.1).normalize(), 0.9, 0.07, 2);
  grp.scale.setScalar(scale);
  grp.userData.tips = tips.map((t) => t.multiplyScalar(scale));
  return grp;
}

/* ------------------------------------------------------------------ helpers */
export function shadowAll(obj, cast = true, receive = false) {
  obj.traverse((o) => { if (o.isMesh) { o.castShadow = cast; o.receiveShadow = receive; } });
  return obj;
}

export function disposeObject(obj) {
  obj.traverse((o) => {
    if (o.geometry && !isCachedGeometry(o.geometry)) o.geometry.dispose();
    const mats = Array.isArray(o.material) ? o.material : o.material ? [o.material] : [];
    for (const m of mats) m.dispose();
  });
}
const isCachedGeometry = (g) => { for (const v of cache.values()) if (v === g) return true; return false; };
