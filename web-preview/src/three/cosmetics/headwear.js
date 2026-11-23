import * as THREE from 'three';
import { M } from '../materials.js';
import { crystalGeometry, sweepGeometry, antler, lightString, hollySprig, starGeometry, bell, snowflakeGeometry, lerp, TAU, rand } from '../geometry.js';
import { ParticleSystem } from '../particles.js';
import { sparkleSprite, flakeSprite, glowSprite, beamTexture, noiseTexture } from '../textures.js';
import { cosmetic } from './base.js';

/* ------------------------------------------------------------- Day 6: Frozen Crown */
export function frozenCrown() {
  const c = cosmetic('head');
  const g = new THREE.Group(); g.position.y = 0.075; c.root.add(g);
  const ice = M.ice({ emissiveIntensity: 0.35 });
  const silver = M.silver();
  const band = new THREE.Mesh(new THREE.CylinderGeometry(0.128, 0.122, 0.05, 64, 1, true), ice); g.add(band);
  for (const y of [-0.026, 0.026]) { const rim = new THREE.Mesh(new THREE.TorusGeometry(0.127, 0.007, 12, 96), silver); rim.rotation.x = Math.PI / 2; rim.position.y = y; g.add(rim); }
  const gems = [];
  const n = 11;
  for (let i = 0; i < n; i++) {
    const a = (i / n) * TAU + Math.PI / 2;
    const front = Math.cos(a - Math.PI / 2);
    const h = 0.06 + Math.pow(Math.max(0, Math.sin(a)), 3) * 0.09 + (i % 2) * 0.025;
    const cr = new THREE.Mesh(crystalGeometry(6, h, 0.017, 0.035), ice);
    cr.position.set(Math.cos(a) * 0.125, 0.02, Math.sin(a) * 0.125);
    cr.rotation.set(Math.sin(a) * 0.18, 0, -Math.cos(a) * 0.18);
    cr.castShadow = true;
    g.add(cr);
    const gem = new THREE.Mesh(new THREE.OctahedronGeometry(0.011, 0), M.glow('#8fe0ff', 4));
    gem.position.set(Math.cos(a) * 0.131, 0, Math.sin(a) * 0.131); g.add(gem); gems.push(gem);
    void front;
  }
  const centre = new THREE.Mesh(new THREE.OctahedronGeometry(0.026, 0), M.gem('#57c7ff', { emissiveIntensity: 2 }));
  centre.position.set(0, 0.005, 0.135); centre.scale.y = 1.4; g.add(centre);
  const frost = c.track(new ParticleSystem({ max: 200, map: flakeSprite(), rate: 30, life: [1, 2], size: [0.015, 0.035], color: ['#ffffff', '#bfe9ff'], gravity: [0, 0.05, 0], spin: [-2, 2], intensity: 2,
    spawn: (P, V) => { const a = Math.random() * TAU; P.set(Math.cos(a) * 0.13, 0.08 + Math.random() * 0.1, Math.sin(a) * 0.13); V.set(Math.cos(a) * 0.03, 0.05, Math.sin(a) * 0.03); }, prewarm: 2 }));
  c.root.add(frost);
  c.on((t, dt) => { gems.forEach((m, i) => { m.material.emissiveIntensity = 2.5 + Math.sin(t * 3 + i) * 1.8; }); frost.tick(dt); });
  return c;
}

/* ------------------------------------------------------------- Day 7: Reindeer Horns */
export function reindeerHorns() {
  const c = cosmetic('head');
  const velvet = M.velvet('#6b4428', { sheenColor: new THREE.Color('#c89a6a'), roughness: 0.7, bumpScale: 0.3 });
  const bandCurve = new THREE.CatmullRomCurve3(Array.from({ length: 13 }, (_, i) => { const a = Math.PI * (i / 12); return new THREE.Vector3(Math.cos(a) * 0.132, 0.02 + Math.sin(a) * 0.12, -0.01); }));
  c.root.add(new THREE.Mesh(sweepGeometry(bandCurve, () => ({ x: 0.012, y: 0.006 }), 48, 10), M.satin('#7a0c22')));
  const strings = [];
  for (const side of [1, -1]) {
    const a = antler({ scale: 0.26, seed: side > 0 ? 7 : 11, material: velvet, mirror: side });
    a.position.set(side * 0.1, 0.1, -0.01);
    a.rotation.z = -side * 0.15;
    c.root.add(a);
    // fairy lights spiralling around the main beam
    const pts = [];
    for (let i = 0; i <= 40; i++) { const t = i / 40, ang = t * TAU * 3; pts.push(new THREE.Vector3(side * (t * 0.11) + Math.cos(ang) * 0.02, t * 0.24 + Math.sin(ang) * 0.02, Math.sin(ang) * 0.02)); }
    const ls = lightString(new THREE.CatmullRomCurve3(pts), 14, ['#ffcf6b', '#ff4d4d', '#7fd4ff', '#8dff9a'], 0.008);
    ls.position.copy(a.position); ls.rotation.z = a.rotation.z; c.root.add(ls); strings.push(ls);
    // jingle bell + holly at the base
    const b = bell(M.gold()); b.scale.setScalar(0.035); b.position.set(side * 0.13, 0.05, 0.03); b.rotation.x = Math.PI; c.root.add(b);
    const h = hollySprig(0.08); h.position.set(side * 0.12, 0.1, 0.03); h.rotation.set(0.3, side * 0.5, side * -0.6); c.root.add(h);
  }
  c.on((t) => strings.forEach((s) => s.userData.update(t)));
  return c;
}

/* ------------------------------------------------------------- Day 10: Christmas Halo */
export function christmasHalo() {
  const c = cosmetic('head');
  const g = new THREE.Group(); g.position.y = 0.25; c.root.add(g);
  const gold = M.gold({ roughness: 0.15 });
  const ring = new THREE.Mesh(new THREE.TorusGeometry(0.19, 0.016, 24, 160), gold); ring.rotation.x = Math.PI / 2; ring.castShadow = true;
  const inner = new THREE.Mesh(new THREE.TorusGeometry(0.19, 0.006, 12, 160), M.glow('#ffd36b', 6)); inner.rotation.x = Math.PI / 2; inner.position.y = -0.012;
  // twisted filigree around the ring
  const fil = new THREE.Mesh(new THREE.TorusKnotGeometry(0.19, 0.004, 400, 8, 40, 1), gold); fil.rotation.x = Math.PI / 2; fil.scale.z = 0.6;
  g.add(ring, inner, fil);
  for (let i = 0; i < 4; i++) {
    const a = (i / 4) * TAU;
    const h = hollySprig(0.11); h.position.set(Math.cos(a) * 0.19, 0.01, Math.sin(a) * 0.19); h.rotation.set(-Math.PI / 2 + 0.3, 0, -a + Math.PI / 2); g.add(h);
  }
  const stars = [];
  for (let i = 0; i < 8; i++) {
    const s = new THREE.Mesh(starGeometry(5, 0.022, 0.01, 0.008), M.glow('#fff1c2', 5));
    stars.push(s); g.add(s);
  }
  const glow = new THREE.Sprite(new THREE.SpriteMaterial({ map: glowSprite(), color: '#ffcf6b', blending: THREE.AdditiveBlending, depthWrite: false, opacity: 0.55 }));
  glow.scale.set(0.7, 0.25, 1); g.add(glow);
  c.on((t) => {
    g.position.y = 0.25 + Math.sin(t * 1.6) * 0.012;
    g.rotation.y = t * 0.4;
    stars.forEach((s, i) => { const a = (i / 8) * TAU + t * 0.9; s.position.set(Math.cos(a) * 0.24, Math.sin(t * 2 + i) * 0.02, Math.sin(a) * 0.24); s.rotation.y = t * 3; s.scale.setScalar(0.8 + Math.sin(t * 5 + i * 2) * 0.35); });
  });
  return c;
}

/* ------------------------------------------------------------- Day 11: Santa Hat */
export function santaHat() {
  const c = cosmetic('head');
  const g = new THREE.Group(); g.position.y = 0.04; g.rotation.x = -0.12; c.root.add(g);
  const curve = new THREE.CatmullRomCurve3([
    new THREE.Vector3(0, 0, 0), new THREE.Vector3(0, 0.1, -0.01), new THREE.Vector3(0.015, 0.19, -0.03), new THREE.Vector3(0.06, 0.25, -0.07),
    new THREE.Vector3(0.13, 0.24, -0.12), new THREE.Vector3(0.18, 0.17, -0.15), new THREE.Vector3(0.2, 0.1, -0.16),
  ]);
  const body = new THREE.Mesh(sweepGeometry(curve, (t) => lerp(0.138, 0.012, Math.pow(t, 0.85)) * (1 + Math.sin(t * 40) * 0.015 * (1 - t)), 120, 40, true), M.velvet('#b0101f', { sheenColor: new THREE.Color('#ff6a6a') }));
  body.castShadow = true; g.add(body);
  // fur brim with lumpy displacement
  const brimGeo = new THREE.TorusGeometry(0.14, 0.036, 32, 128);
  const p = brimGeo.attributes.position, n = brimGeo.attributes.normal, r = rand(4);
  for (let i = 0; i < p.count; i++) { const k = 1 + (Math.sin(i * 0.37) * 0.5 + r()) * 0.012 / 0.036 * 0.6; p.setXYZ(i, p.getX(i) + n.getX(i) * 0.036 * (k - 1), p.getY(i) + n.getY(i) * 0.036 * (k - 1), p.getZ(i) + n.getZ(i) * 0.036 * (k - 1)); }
  brimGeo.computeVertexNormals();
  const fur = M.fur();
  const brim = new THREE.Mesh(brimGeo, fur); brim.rotation.x = Math.PI / 2; brim.scale.z = 0.8; brim.castShadow = true; g.add(brim);
  const pomGeo = new THREE.IcosahedronGeometry(0.045, 5);
  const pp = pomGeo.attributes.position;
  for (let i = 0; i < pp.count; i++) { const v = new THREE.Vector3().fromBufferAttribute(pp, i); v.multiplyScalar(1 + (r() - 0.5) * 0.18); pp.setXYZ(i, v.x, v.y, v.z); }
  pomGeo.computeVertexNormals();
  const pom = new THREE.Mesh(pomGeo, fur); pom.position.copy(curve.getPointAt(1)); pom.castShadow = true; g.add(pom);
  // embroidered golden snowflake emblem
  const emblem = new THREE.Mesh(snowflakeGeometry(1, 0.02), M.gold({ emissive: new THREE.Color('#ffb52e'), emissiveIntensity: 0.4 }));
  emblem.scale.setScalar(0.035); emblem.position.set(0, 0.075, 0.122); emblem.rotation.x = -0.15; g.add(emblem);
  const twinkle = c.track(new ParticleSystem({ max: 60, map: sparkleSprite(), rate: 6, life: [0.5, 1], size: [0.03, 0.05], color: ['#fff1c2'], intensity: 2.5,
    spawn: (P) => { P.copy(emblem.position).add(new THREE.Vector3((Math.random() - 0.5) * 0.06, 0.04 + (Math.random() - 0.5) * 0.06, 0.01)); }, prewarm: 1 }));
  g.add(twinkle);
  c.on((t, dt) => { pom.position.copy(curve.getPointAt(1)).add(new THREE.Vector3(Math.sin(t * 2) * 0.006, Math.sin(t * 2.6) * 0.004, 0)); twinkle.tick(dt); });
  return c;
}

/* ------------------------------------------------------------- Day 23: Legendary Ice Crown */
export function legendaryIceCrown() {
  const c = cosmetic('head');
  const g = new THREE.Group(); g.position.y = 0.075; c.root.add(g);
  const gold = M.gold({ roughness: 0.14 });
  const ice = M.ice({ emissiveIntensity: 0.55, iridescence: 0.8 });
  const band = new THREE.Mesh(new THREE.CylinderGeometry(0.132, 0.126, 0.06, 96, 1, true), gold); band.castShadow = true; g.add(band);
  for (const y of [-0.03, 0.03]) { const r = new THREE.Mesh(new THREE.TorusGeometry(0.131, 0.008, 12, 128), gold); r.rotation.x = Math.PI / 2; r.position.y = y; g.add(r); }
  // filigree scrolls
  const fil = new THREE.Mesh(new THREE.TorusKnotGeometry(0.133, 0.0035, 600, 6, 24, 2), gold); fil.rotation.x = Math.PI / 2; fil.scale.z = 0.18; g.add(fil);
  // inset gems on band
  const gemMats = [M.gem('#57c7ff', { emissiveIntensity: 1.6 }), M.gem('#ff3355', { emissiveIntensity: 1.2 })];
  for (let i = 0; i < 16; i++) { const a = (i / 16) * TAU; const gm = new THREE.Mesh(new THREE.OctahedronGeometry(0.011, 0), gemMats[i % 2]); gm.position.set(Math.cos(a) * 0.137, 0, Math.sin(a) * 0.137); g.add(gm); }
  // alternating gold prongs and ice spires
  const n = 10;
  for (let i = 0; i < n; i++) {
    const a = (i / n) * TAU + Math.PI / 2;
    const frontBoost = Math.pow(Math.max(0, Math.sin(a)), 2);
    const base = new THREE.Vector3(Math.cos(a) * 0.132, 0.03, Math.sin(a) * 0.132);
    if (i % 2 === 0) {
      const h = 0.13 + frontBoost * 0.08;
      const spire = new THREE.Mesh(crystalGeometry(6, h, 0.02, 0.05), ice);
      spire.position.copy(base); spire.rotation.set(Math.sin(a) * 0.2, 0, -Math.cos(a) * 0.2); spire.castShadow = true; g.add(spire);
      const cradle = new THREE.Mesh(new THREE.CylinderGeometry(0.026, 0.018, 0.03, 6), gold); cradle.position.copy(base).setY(0.04); g.add(cradle);
    } else {
      const top = base.clone().multiplyScalar(1.12).setY(0.11 + frontBoost * 0.04);
      const curve = new THREE.CatmullRomCurve3([base, base.clone().multiplyScalar(0.92).setY(0.07), top]);
      g.add(new THREE.Mesh(sweepGeometry(curve, (t) => lerp(0.009, 0.004, t), 20, 8), gold));
      const orb = new THREE.Mesh(new THREE.SphereGeometry(0.011, 16, 12), gold); orb.position.copy(top); g.add(orb);
    }
  }
  // central star jewel
  const star = new THREE.Mesh(starGeometry(8, 0.045, 0.02, 0.014), gold); star.position.set(0, 0.1, 0.15); g.add(star);
  const jewel = new THREE.Mesh(new THREE.OctahedronGeometry(0.024, 1), M.gem('#9fe6ff', { emissiveIntensity: 3 })); jewel.position.copy(star.position).add(new THREE.Vector3(0, 0, 0.012)); g.add(jewel);
  // orbiting ice shards
  const orbit = new THREE.Group(); g.add(orbit);
  for (let i = 0; i < 6; i++) { const s = new THREE.Mesh(crystalGeometry(4, 0.05, 0.012, 0.025), ice); const a = (i / 6) * TAU; s.position.set(Math.cos(a) * 0.22, 0.05 + (i % 2) * 0.05, Math.sin(a) * 0.22); orbit.add(s); }
  // light beams
  const beams = new THREE.Group(); beams.position.y = 0.05; g.add(beams);
  const beamMat = new THREE.MeshBasicMaterial({ map: beamTexture(), color: new THREE.Color('#bfe9ff').multiplyScalar(1.6), transparent: true, blending: THREE.AdditiveBlending, depthWrite: false, side: THREE.DoubleSide });
  for (let i = 0; i < 3; i++) { const b = new THREE.Mesh(new THREE.PlaneGeometry(0.18, 0.7), beamMat); b.position.y = 0.3; b.rotation.y = (i / 3) * Math.PI; beams.add(b); }
  const dust = c.track(new ParticleSystem({ max: 400, map: sparkleSprite(), rate: 70, life: [1, 2], size: [0.015, 0.04], color: ['#ffd36b', '#bfe9ff', '#ffffff'], gravity: [0, 0.12, 0], spin: [-3, 3], intensity: 2.6,
    spawn: (P, V) => { const a = Math.random() * TAU, r = 0.1 + Math.random() * 0.15; P.set(Math.cos(a) * r, 0.1 + Math.random() * 0.1, Math.sin(a) * r); V.set(0, 0.05, 0); }, prewarm: 2 }));
  g.add(dust);
  c.on((t, dt) => {
    orbit.rotation.y = t * 0.8; orbit.children.forEach((s, i) => { s.rotation.y = t * 2 + i; });
    jewel.rotation.y = t * 1.5; jewel.material.emissiveIntensity = 2.5 + Math.sin(t * 3) * 1.2;
    beams.rotation.y = t * 0.3; beamMat.opacity = 0.35 + Math.sin(t * 2) * 0.15;
    dust.tick(dt);
  });
  return c;
}

