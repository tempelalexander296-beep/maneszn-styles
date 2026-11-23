import * as THREE from 'three';
import { M } from '../materials.js';
import { starGeometry, crystalGeometry, sweepGeometry, giftBox, lerp, TAU } from '../geometry.js';
import { ParticleSystem, RibbonTrail } from '../particles.js';
import { sparkleSprite, flakeSprite } from '../textures.js';
import { cosmetic, trailPath } from './base.js';

/** Drives an anchor along the showcase path; calls fn(pos, tangent, t, dt) every frame. */
function driveAnchor(c, fn, prewarm = 2.5) {
  const p = new THREE.Vector3(), q = new THREE.Vector3(), tan = new THREE.Vector3();
  const step = (t, dt) => { trailPath(t, p); trailPath(t + 0.02, q); tan.subVectors(q, p).normalize(); fn(p, tan, t, dt); };
  for (let t = -prewarm; t < 0; t += 1 / 60) step(t, 1 / 60);
  c.on(step);
}

/* ------------------------------------------------------------- Day 2: Christmas Trail */
export function christmasTrail() {
  const c = cosmetic('trail');
  const red = new RibbonTrail({ length: 90, width: 0.07, colorA: '#ff2b47', colorB: '#ff8a5c', intensity: 2.4 });
  const green = new RibbonTrail({ length: 90, width: 0.07, colorA: '#24d86a', colorB: '#c9ff9e', intensity: 2.2 });
  const core = new THREE.Mesh(starGeometry(5, 0.07, 0.03, 0.025), M.glow('#ffd36b', 5));
  const sparks = c.track(new ParticleSystem({ max: 900, map: sparkleSprite(), rate: 0, life: [0.6, 1.4], size: [0.03, 0.08], color: ['#ff3355', '#2be07a', '#ffd36b', '#ffffff'], gravity: [0, -0.4, 0], drag: 1.2, spin: [-3, 3], intensity: 2.2 }));
  c.root.add(red, green, core, sparks);
  const h = new THREE.Vector3(), side = new THREE.Vector3(), up = new THREE.Vector3(0, 1, 0);
  driveAnchor(c, (p, tan, t, dt) => {
    side.crossVectors(tan, up).normalize();
    const a = t * 9;
    h.copy(p).addScaledVector(side, Math.cos(a) * 0.09).addScaledVector(up, Math.sin(a) * 0.09); red.push(h, t);
    h.copy(p).addScaledVector(side, -Math.cos(a) * 0.09).addScaledVector(up, -Math.sin(a) * 0.09); green.push(h, t);
    core.position.copy(p); core.rotation.y = t * 4; core.rotation.z = t * 2;
    const n = Math.round(dt * 220);
    for (let i = 0; i < n; i++) sparks.spawnOne({ spawn: (P, V) => { P.copy(p).add(new THREE.Vector3((Math.random() - 0.5) * 0.1, (Math.random() - 0.5) * 0.1, (Math.random() - 0.5) * 0.1)); V.set((Math.random() - 0.5) * 0.4, Math.random() * 0.3, (Math.random() - 0.5) * 0.4); } });
    sparks.tick(dt);
  });
  return c;
}

/* ------------------------------------------------------------- Day 12: Ice Crystal Trail */
export function iceCrystalTrail() {
  const c = cosmetic('trail');
  const ribbon = new RibbonTrail({ length: 70, width: 0.05, colorA: '#7fd4ff', colorB: '#e8f8ff', intensity: 2, twist: 0.6 });
  const N = 70;
  const crystals = new THREE.InstancedMesh(crystalGeometry(6, 0.22, 0.04, 0.07), M.ice({ emissiveIntensity: 0.5, transmission: 0.7 }), N);
  crystals.castShadow = true;
  const slots = Array.from({ length: N }, () => ({ born: -99, pos: new THREE.Vector3(), rot: new THREE.Euler(), s: 1 }));
  let cursor = 0, acc = 0;
  const gem = new THREE.Mesh(new THREE.OctahedronGeometry(0.07, 0), M.gem('#8fdcff'));
  const frost = c.track(new ParticleSystem({ max: 700, map: flakeSprite(), rate: 0, life: [0.8, 1.8], size: [0.03, 0.07], color: ['#ffffff', '#bfe9ff', '#7fd4ff'], gravity: [0, -0.2, 0], drag: 1.5, spin: [-2, 2], intensity: 2 }));
  c.root.add(ribbon, crystals, gem, frost);
  const m = new THREE.Matrix4(), q = new THREE.Quaternion(), sc = new THREE.Vector3();
  driveAnchor(c, (p, tan, t, dt) => {
    ribbon.push(p, t);
    gem.position.copy(p); gem.rotation.y = t * 3; gem.rotation.x = t * 1.3;
    acc += dt * 28;
    while (acc >= 1) {
      acc -= 1;
      const s = slots[cursor]; cursor = (cursor + 1) % N;
      s.born = t; s.pos.copy(p).add(new THREE.Vector3((Math.random() - 0.5) * 0.14, -0.05 - Math.random() * 0.05, (Math.random() - 0.5) * 0.14));
      s.rot.set((Math.random() - 0.5) * 1.4, Math.random() * TAU, (Math.random() - 0.5) * 1.4); s.s = 0.6 + Math.random() * 0.8;
    }
    for (let i = 0; i < N; i++) {
      const s = slots[i]; const age = t - s.born;
      const k = age < 0.15 ? age / 0.15 : age < 1.6 ? 1 : Math.max(0, 1 - (age - 1.6) / 0.35);
      q.setFromEuler(s.rot); sc.setScalar(Math.max(0.0001, k * s.s));
      m.compose(s.pos, q, sc); crystals.setMatrixAt(i, m);
      if (age > 1.6 && age < 1.62) for (let j = 0; j < 6; j++) frost.spawnOne({ spawn: (P, V) => { P.copy(s.pos); V.set((Math.random() - 0.5) * 0.8, Math.random() * 0.6, (Math.random() - 0.5) * 0.8); } });
    }
    crystals.instanceMatrix.needsUpdate = true;
    for (let j = 0; j < Math.round(dt * 90); j++) frost.spawnOne({ spawn: (P, V) => { P.copy(p); V.set((Math.random() - 0.5) * 0.3, (Math.random() - 0.5) * 0.3, (Math.random() - 0.5) * 0.3); } });
    frost.tick(dt);
  });
  return c;
}

/* ------------------------------------------------------------- Day 21: Santa's Sleigh Trail */
function sleighModel() {
  const g = new THREE.Group();
  const red = M.plastic('#a30f1f', { clearcoat: 1, clearcoatRoughness: 0.05, metalness: 0.2 });
  const gold = M.gold();
  // body: extruded side profile
  const s = new THREE.Shape();
  s.moveTo(-0.22, 0.04); s.lineTo(0.16, 0.04); s.bezierCurveTo(0.26, 0.04, 0.3, 0.12, 0.27, 0.2);
  s.bezierCurveTo(0.24, 0.26, 0.2, 0.22, 0.18, 0.16); s.lineTo(0.02, 0.13); s.lineTo(-0.14, 0.2);
  s.bezierCurveTo(-0.2, 0.26, -0.27, 0.24, -0.26, 0.15); s.lineTo(-0.22, 0.04);
  const body = new THREE.Mesh(new THREE.ExtrudeGeometry(s, { depth: 0.18, bevelEnabled: true, bevelThickness: 0.015, bevelSize: 0.012, bevelSegments: 4, curveSegments: 24 }), red);
  body.position.z = -0.09; g.add(body);
  const trim = new THREE.Mesh(new THREE.TorusGeometry(0.115, 0.007, 8, 48, Math.PI), gold);
  trim.position.set(-0.02, 0.13, 0.1); trim.scale.set(1.6, 0.5, 1); g.add(trim);
  const trim2 = trim.clone(); trim2.position.z = -0.1; g.add(trim2);
  // runners with curl
  for (const z of [-0.08, 0.08]) {
    const curve = new THREE.CatmullRomCurve3([new THREE.Vector3(-0.24, 0.0, z), new THREE.Vector3(0.18, 0.0, z), new THREE.Vector3(0.28, 0.03, z), new THREE.Vector3(0.31, 0.1, z), new THREE.Vector3(0.26, 0.14, z), new THREE.Vector3(0.23, 0.1, z)]);
    g.add(new THREE.Mesh(new THREE.TubeGeometry(curve, 60, 0.008, 8), gold));
    for (const x of [-0.14, 0.08]) { const post = new THREE.Mesh(new THREE.CylinderGeometry(0.006, 0.006, 0.05, 6), gold); post.position.set(x, 0.025, z); g.add(post); }
  }
  // sack with presents
  const sack = new THREE.Mesh(new THREE.SphereGeometry(0.09, 24, 16), M.velvet('#6b4a2b', { sheenColor: new THREE.Color('#c79a63') }));
  sack.scale.set(1, 1.1, 0.9); sack.position.set(-0.13, 0.2, 0); g.add(sack);
  const gb = giftBox({ w: 0.07, h: 0.07, d: 0.07, palette: 'green', motif: 'dots' }); gb.position.set(-0.04, 0.13, 0.03); gb.rotation.y = 0.5; g.add(gb);
  const gb2 = giftBox({ w: 0.06, h: 0.06, d: 0.06, palette: 'white', motif: 'stars', ribbon: '#c8102e' }); gb2.position.set(-0.08, 0.27, -0.02); gb2.rotation.z = 0.3; g.add(gb2);
  const lamp = new THREE.Mesh(new THREE.SphereGeometry(0.02, 12, 10), M.glow('#ffcf6b', 6)); lamp.position.set(0.27, 0.22, 0); g.add(lamp);
  g.traverse((o) => { if (o.isMesh) o.castShadow = true; });
  return g;
}

export function santasSleighTrail() {
  const c = cosmetic('trail');
  const sleigh = sleighModel(); sleigh.scale.setScalar(1.15);
  const ribbonA = new RibbonTrail({ length: 110, width: 0.06, colorA: '#ffd36b', colorB: '#ff7a3d', intensity: 2.6 });
  const ribbonB = new RibbonTrail({ length: 110, width: 0.06, colorA: '#ffe9a8', colorB: '#ffd36b', intensity: 2.2 });
  const dust = c.track(new ParticleSystem({ max: 1400, map: sparkleSprite(), rate: 0, life: [0.8, 2.2], size: [0.025, 0.08], color: ['#ffd36b', '#fff4cf', '#ffb52e', '#ff6b6b'], gravity: [0, -0.18, 0], drag: 1, spin: [-3, 3], intensity: 2.5 }));
  const stars = c.track(new ParticleSystem({ max: 200, map: sparkleSprite(), rate: 0, life: [0.4, 1], size: [0.1, 0.22], color: ['#ffffff', '#ffe6a3'], intensity: 3, fadeIn: 0.3 }));
  // glowing reins leading to a guiding star
  const guide = new THREE.Mesh(starGeometry(5, 0.06, 0.025, 0.02), M.glow('#fff1c2', 6));
  const reinGeo = new THREE.BufferGeometry().setFromPoints(Array.from({ length: 20 }, () => new THREE.Vector3()));
  const reins = new THREE.Line(reinGeo, new THREE.LineBasicMaterial({ color: '#ffd36b', transparent: true, opacity: 0.8 }));
  reins.frustumCulled = false;
  c.root.add(sleigh, ribbonA, ribbonB, dust, stars, guide, reins);
  const tmp = new THREE.Vector3(), look = new THREE.Vector3(), g = new THREE.Vector3(), up = new THREE.Vector3(0, 1, 0), side = new THREE.Vector3();
  driveAnchor(c, (p, tan, t, dt) => {
    sleigh.position.copy(p);
    look.copy(p).add(tan);
    sleigh.lookAt(look); sleigh.rotateY(-Math.PI / 2);
    sleigh.rotation.x += Math.sin(t * 3) * 0.05;
    side.crossVectors(tan, up).normalize();
    tmp.copy(p).addScaledVector(tan, -0.25).addScaledVector(side, 0.07); ribbonA.push(tmp, t);
    tmp.copy(p).addScaledVector(tan, -0.25).addScaledVector(side, -0.07); ribbonB.push(tmp, t);
    trailPath(t + 0.45, g); g.y += 0.12; guide.position.copy(g); guide.rotation.y = t * 3;
    const arr = reinGeo.attributes.position.array;
    for (let i = 0; i < 20; i++) {
      const k = i / 19; tmp.copy(p).addScaledVector(tan, 0.3).lerp(g, k); tmp.y -= Math.sin(k * Math.PI) * 0.06;
      arr[i * 3] = tmp.x; arr[i * 3 + 1] = tmp.y; arr[i * 3 + 2] = tmp.z;
    }
    reinGeo.attributes.position.needsUpdate = true;
    for (let j = 0; j < Math.round(dt * 380); j++) dust.spawnOne({ spawn: (P, V) => { P.copy(p).addScaledVector(tan, -0.25).add(new THREE.Vector3((Math.random() - 0.5) * 0.2, (Math.random() - 0.5) * 0.1, (Math.random() - 0.5) * 0.2)); V.set((Math.random() - 0.5) * 0.2, Math.random() * 0.15, (Math.random() - 0.5) * 0.2); } });
    if (Math.random() < dt * 14) stars.spawnOne({ spawn: (P) => { P.copy(p).addScaledVector(tan, -0.3 - Math.random() * 1.2).add(new THREE.Vector3((Math.random() - 0.5) * 0.3, (Math.random() - 0.5) * 0.3, (Math.random() - 0.5) * 0.3)); } });
    dust.tick(dt); stars.tick(dt);
  });
  return c;
}
