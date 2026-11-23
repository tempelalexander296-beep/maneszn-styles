import * as THREE from 'three';
import { M } from '../materials.js';
import { antler, sweepGeometry, bell, hollySprig, lerp, TAU, rand } from '../geometry.js';
import { ParticleSystem } from '../particles.js';
import { sparkleSprite, glowSprite, flakeSprite, scaleTexture, giftPaper } from '../textures.js';
import { cosmetic } from './base.js';

const eye = (r = 0.012) => {
  const g = new THREE.Group();
  g.add(new THREE.Mesh(new THREE.SphereGeometry(r, 20, 14), new THREE.MeshPhysicalMaterial({ color: '#0b0b0f', roughness: 0.05, clearcoat: 1 })));
  const hi = new THREE.Mesh(new THREE.SphereGeometry(r * 0.3, 10, 8), M.glow('#ffffff', 3)); hi.position.set(r * 0.35, r * 0.4, r * 0.75); g.add(hi);
  return g;
};

const leg = (len, r0, r1, mat) => {
  const curve = new THREE.CatmullRomCurve3([new THREE.Vector3(0, 0, 0), new THREE.Vector3(0, -len * 0.5, 0.01), new THREE.Vector3(0, -len, 0)]);
  return new THREE.Mesh(sweepGeometry(curve, (t) => lerp(r0, r1, t), 12, 10), mat);
};

/* ------------------------------------------------------------- Day 15: Reindeer Companion */
export function reindeerCompanion() {
  const c = cosmetic('companion');
  const deer = new THREE.Group(); c.root.add(deer);
  const fur = M.velvet('#8a5a36', { sheenColor: new THREE.Color('#e0b48a'), bumpScale: 0.3, roughness: 0.75 });
  const belly = M.velvet('#e9d3b4', { sheenColor: new THREE.Color('#ffffff'), bumpScale: 0.3 });
  const body = new THREE.Mesh(new THREE.SphereGeometry(0.1, 32, 24), fur); body.scale.set(1.45, 0.95, 0.9); deer.add(body);
  const tummy = new THREE.Mesh(new THREE.SphereGeometry(0.08, 24, 16), belly); tummy.scale.set(1.5, 0.8, 0.85); tummy.position.set(0, -0.03, 0.0); deer.add(tummy);
  const neck = new THREE.Mesh(sweepGeometry(new THREE.CatmullRomCurve3([new THREE.Vector3(0.1, 0.03, 0), new THREE.Vector3(0.14, 0.1, 0), new THREE.Vector3(0.16, 0.16, 0)]), (t) => lerp(0.05, 0.04, t), 12, 16), fur); deer.add(neck);
  const head = new THREE.Group(); head.position.set(0.17, 0.19, 0); deer.add(head);
  const skull = new THREE.Mesh(new THREE.SphereGeometry(0.065, 32, 24), fur); skull.scale.set(1.1, 1, 0.95); head.add(skull);
  const snout = new THREE.Mesh(new THREE.SphereGeometry(0.045, 24, 16), belly); snout.scale.set(1.3, 0.8, 0.9); snout.position.set(0.06, -0.025, 0); head.add(snout);
  const nose = new THREE.Mesh(new THREE.SphereGeometry(0.022, 24, 16), M.glow('#ff2238', 5)); nose.position.set(0.115, -0.02, 0); head.add(nose);
  const noseLight = new THREE.PointLight('#ff2a3d', 0.6, 0.8, 2); noseLight.position.copy(nose.position).add(new THREE.Vector3(0.04, 0, 0)); head.add(noseLight);
  const noseGlow = new THREE.Sprite(new THREE.SpriteMaterial({ map: glowSprite(), color: '#ff3048', blending: THREE.AdditiveBlending, depthWrite: false })); noseGlow.scale.setScalar(0.12); noseGlow.position.copy(nose.position); head.add(noseGlow);
  for (const z of [-1, 1]) {
    const e = eye(0.012); e.position.set(0.035, 0.02, z * 0.045); e.rotation.y = z * 0.4; head.add(e);
    const ear = new THREE.Mesh(new THREE.ConeGeometry(0.02, 0.06, 16), fur); ear.scale.z = 0.4; ear.position.set(-0.02, 0.05, z * 0.055); ear.rotation.set(z * 0.9, 0, -0.4); head.add(ear);
    const a = antler({ scale: 0.12, seed: z > 0 ? 3 : 8, mirror: z, material: M.matte('#5c3a22', { roughness: 0.55 }) });
    a.rotation.y = -Math.PI / 2; a.position.set(-0.02, 0.05, z * 0.03); head.add(a);
  }
  // scarf + bell collar
  const scarf = new THREE.Mesh(new THREE.TorusGeometry(0.048, 0.016, 12, 32), new THREE.MeshPhysicalMaterial({ map: giftPaper('red', 'plaid', 512), roughness: 0.8, sheen: 1, sheenColor: new THREE.Color('#ffb0b0') }));
  scarf.position.set(0.14, 0.12, 0); scarf.rotation.set(Math.PI / 2, 0.5, 0); deer.add(scarf);
  const jingle = bell(M.gold()); jingle.scale.setScalar(0.025); jingle.rotation.z = Math.PI; jingle.position.set(0.175, 0.09, 0); deer.add(jingle);
  const tail = new THREE.Mesh(new THREE.SphereGeometry(0.025, 16, 12), belly); tail.position.set(-0.15, 0.04, 0); deer.add(tail);
  const legs = [];
  const hoofMat = M.plastic('#2b1a10', { roughness: 0.35 });
  [[0.08, 0.06], [0.08, -0.06], [-0.08, 0.06], [-0.08, -0.06]].forEach(([x, z], i) => {
    const pivot = new THREE.Group(); pivot.position.set(x, -0.04, z); deer.add(pivot);
    pivot.add(leg(0.12, 0.022, 0.012, fur));
    const hoof = new THREE.Mesh(new THREE.CylinderGeometry(0.012, 0.015, 0.02, 12), hoofMat); hoof.position.y = -0.125; pivot.add(hoof);
    legs.push({ pivot, ph: i < 2 ? 0 : Math.PI });
  });
  deer.traverse((o) => { if (o.isMesh) o.castShadow = true; });
  const sparkles = c.track(new ParticleSystem({ max: 400, map: sparkleSprite(), rate: 0, life: [0.6, 1.4], size: [0.02, 0.05], color: ['#ffd36b', '#ffffff', '#ff6b6b'], gravity: [0, -0.15, 0], intensity: 2.6, spin: [-3, 3] }));
  c.root.add(sparkles);
  const hoofPos = new THREE.Vector3();
  c.on((t, dt) => {
    const a = t * 0.7;
    deer.position.set(Math.cos(a) * 0.12, Math.sin(t * 2.4) * 0.05, Math.sin(a) * 0.12);
    deer.rotation.y = -a - Math.PI / 2 + Math.PI / 2;
    deer.rotation.z = Math.sin(t * 4.8) * 0.06;
    legs.forEach(({ pivot, ph }) => { pivot.rotation.z = Math.sin(t * 9.6 + ph) * 0.6; });
    head.rotation.z = Math.sin(t * 4.8 + 1) * 0.08;
    nose.material.emissiveIntensity = 4 + Math.sin(t * 3) * 1.5; noseLight.intensity = 0.5 + Math.sin(t * 3) * 0.2;
    if (Math.random() < dt * 40) { legs[(Math.random() * 4) | 0].pivot.children[1].getWorldPosition(hoofPos); c.root.worldToLocal(hoofPos); sparkles.spawnOne({ spawn: (P, V) => { P.copy(hoofPos); V.set((Math.random() - 0.5) * 0.1, -0.05, (Math.random() - 0.5) * 0.1); } }); }
    sparkles.tick(dt);
  });
  return c;
}

/* ------------------------------------------------------------- Day 18: Snowman Companion */
function snowBall(r, seed) {
  const geo = new THREE.IcosahedronGeometry(r, 6);
  const p = geo.attributes.position, rr = rand(seed);
  const v = new THREE.Vector3();
  for (let i = 0; i < p.count; i++) { v.fromBufferAttribute(p, i); const n = Math.sin(v.x * 60) * Math.sin(v.y * 55) * Math.sin(v.z * 50); v.multiplyScalar(1 + n * 0.015 + (rr() - 0.5) * 0.012); p.setXYZ(i, v.x, v.y, v.z); }
  geo.computeVertexNormals();
  return new THREE.Mesh(geo, M.snow({ roughness: 0.8 }));
}

export function snowmanCompanion() {
  const c = cosmetic('companion');
  const sm = new THREE.Group(); sm.position.y = -0.2; c.root.add(sm);
  const coal = M.matte('#141414', { roughness: 0.95, bumpScale: 1 });
  const b1 = snowBall(0.11, 1); b1.position.y = 0.1; sm.add(b1);
  const upper = new THREE.Group(); upper.position.y = 0.19; sm.add(upper);
  const b2 = snowBall(0.08, 2); b2.position.y = 0.06; upper.add(b2);
  const head = new THREE.Group(); head.position.y = 0.18; upper.add(head);
  const b3 = snowBall(0.06, 3); head.add(b3);
  for (const x of [-1, 1]) { const e = eye(0.009); e.position.set(x * 0.022, 0.015, 0.053); head.add(e); }
  const carrot = new THREE.Mesh(new THREE.ConeGeometry(0.012, 0.07, 16), M.matte('#ff7a1a', { roughness: 0.6 })); carrot.rotation.x = Math.PI / 2; carrot.position.set(0, 0, 0.088); head.add(carrot);
  for (let i = 0; i < 5; i++) { const a = -0.6 + i * 0.3; const s = new THREE.Mesh(new THREE.SphereGeometry(0.006, 10, 8), coal); s.position.set(Math.sin(a) * 0.04, -0.025 - Math.cos(a) * 0.008, 0.048); head.add(s); }
  for (let i = 0; i < 3; i++) { const s = new THREE.Mesh(new THREE.SphereGeometry(0.01, 12, 10), coal); s.position.set(0, 0.11 - i * 0.035 - 0.0, 0.077 - Math.abs(i - 1) * 0.005); s.position.y -= 0.03; upper.add(s); }
  // top hat
  const hat = new THREE.Group(); hat.position.y = 0.05; hat.rotation.z = 0.15; head.add(hat);
  const satin = M.satin('#121218', { sheenColor: new THREE.Color('#5a5a70') });
  const brim = new THREE.Mesh(new THREE.CylinderGeometry(0.06, 0.06, 0.008, 40), satin); hat.add(brim);
  const crown = new THREE.Mesh(new THREE.CylinderGeometry(0.038, 0.04, 0.065, 40), satin); crown.position.y = 0.035; hat.add(crown);
  const band = new THREE.Mesh(new THREE.CylinderGeometry(0.0405, 0.0405, 0.014, 40), M.satin('#b3162a')); band.position.y = 0.012; hat.add(band);
  const h = hollySprig(0.06); h.position.set(0.03, 0.02, 0.03); h.rotation.y = 0.8; hat.add(h);
  // knitted scarf
  const scarfMat = new THREE.MeshPhysicalMaterial({ map: giftPaper('green', 'stripes', 512), roughness: 0.9, sheen: 1, sheenColor: new THREE.Color('#a6ffbf') });
  const scarf = new THREE.Mesh(new THREE.TorusGeometry(0.058, 0.016, 12, 40), scarfMat); scarf.rotation.x = Math.PI / 2; scarf.position.y = 0.13; upper.add(scarf);
  const tail = new THREE.Mesh(new THREE.BoxGeometry(0.03, 0.09, 0.012), scarfMat); tail.position.set(0.035, 0.085, 0.055); tail.rotation.z = 0.2; upper.add(tail);
  // twig arms
  const twig = M.matte('#4a2e1a', { roughness: 0.9 });
  const arms = [];
  for (const side of [-1, 1]) {
    const pivot = new THREE.Group(); pivot.position.set(side * 0.07, 0.08, 0); upper.add(pivot);
    const curve = new THREE.CatmullRomCurve3([new THREE.Vector3(0, 0, 0), new THREE.Vector3(side * 0.06, 0.03, 0), new THREE.Vector3(side * 0.12, 0.05, 0.01)]);
    pivot.add(new THREE.Mesh(sweepGeometry(curve, (t) => lerp(0.006, 0.003, t), 12, 6), twig));
    for (const k of [0.6, 0.85]) { const p = curve.getPointAt(k); const f = new THREE.CatmullRomCurve3([p, p.clone().add(new THREE.Vector3(side * 0.025, 0.03, 0))]); pivot.add(new THREE.Mesh(sweepGeometry(f, () => 0.0025, 4, 5), twig)); }
    arms.push({ pivot, side });
  }
  sm.traverse((o) => { if (o.isMesh) o.castShadow = true; });
  const flurry = c.track(new ParticleSystem({ max: 300, map: flakeSprite(), rate: 0, life: [0.6, 1.2], size: [0.02, 0.04], color: ['#ffffff', '#dff2ff'], gravity: [0, -0.6, 0], intensity: 2, spin: [-3, 3] }));
  c.root.add(flurry);
  let lastHop = 0;
  c.on((t, dt) => {
    const cycle = (t * 1.4) % 1; const hop = Math.sin(cycle * Math.PI);
    sm.position.y = -0.2 + hop * 0.12;
    const squash = cycle < 0.08 || cycle > 0.92 ? 0.9 : 1.03;
    sm.scale.set(1 / Math.sqrt(squash), squash, 1 / Math.sqrt(squash));
    if (cycle < lastHop) flurry.burst(18, { spawn: (P, V) => { const a = Math.random() * TAU; P.set(Math.cos(a) * 0.08, -0.2, Math.sin(a) * 0.08); V.set(Math.cos(a) * 0.4, 0.4 + Math.random() * 0.3, Math.sin(a) * 0.4); } });
    lastHop = cycle;
    arms[1].pivot.rotation.z = 0.5 + Math.sin(t * 7) * 0.45;
    arms[0].pivot.rotation.z = -0.2 + Math.sin(t * 1.4) * 0.1;
    head.rotation.z = Math.sin(t * 1.4) * 0.12; head.rotation.y = Math.sin(t * 0.7) * 0.4;
    sm.rotation.y = Math.sin(t * 0.5) * 0.5 + 0.3;
    flurry.tick(dt);
  });
  return c;
}

/* ------------------------------------------------------------- Day 22: Christmas Dragon */
export function christmasDragon() {
  const c = cosmetic('companion');
  const dragon = new THREE.Group(); c.root.add(dragon);
  const scales = new THREE.MeshPhysicalMaterial({ map: scaleTexture('#1a7a40', '#06301a', '#5fd08a'), roughness: 0.35, metalness: 0.2, clearcoat: 0.8, iridescence: 0.3, sheen: 0.4, sheenColor: new THREE.Color('#9fffbf') });
  scales.map = scales.map.clone(); scales.map.needsUpdate = true; scales.map.repeat.set(2, 2);
  const gold = M.gold({ roughness: 0.2 });
  const bellyMat = M.gold({ color: new THREE.Color('#ffd98a'), roughness: 0.35, metalness: 0.6 });
  // serpentine body as a chain of segments (cheap to animate, looks continuous)
  const SEG = 22;
  const segGeo = new THREE.SphereGeometry(1, 24, 16);
  const segments = [];
  for (let i = 0; i < SEG; i++) {
    const t = i / (SEG - 1);
    const r = lerp(0.045, 0.008, Math.pow(t, 0.9)) * (i < 3 ? 1 - (3 - i) * 0.08 : 1);
    const m = new THREE.Mesh(segGeo, scales); m.scale.set(r * 1.3, r, r);
    const bel = new THREE.Mesh(segGeo, bellyMat); bel.scale.set(0.95, 0.6, 0.9); bel.position.y = -0.4; m.add(bel);
    if (i % 2 === 0 && i < SEG - 2) { const sp = new THREE.Mesh(new THREE.ConeGeometry(0.4, 1.1, 8), gold); sp.position.y = 1.05; sp.rotation.z = 0.35; m.add(sp); }
    m.castShadow = true; dragon.add(m); segments.push(m);
  }
  // candy-cane striped tail tip
  const tip = new THREE.Mesh(new THREE.ConeGeometry(0.018, 0.07, 16), M.candy('#c8102e', null, 1)); dragon.add(tip);
  // head
  const head = new THREE.Group(); dragon.add(head);
  const skull = new THREE.Mesh(new THREE.SphereGeometry(0.055, 32, 24), scales); skull.scale.set(1.1, 0.9, 0.95); head.add(skull);
  const muzzle = new THREE.Mesh(new THREE.SphereGeometry(0.04, 24, 16), scales); muzzle.scale.set(1.5, 0.7, 0.85); muzzle.position.set(0.055, -0.012, 0); head.add(muzzle);
  const jaw = new THREE.Mesh(new THREE.SphereGeometry(0.035, 24, 16), bellyMat); jaw.scale.set(1.4, 0.45, 0.8); jaw.position.set(0.05, -0.03, 0); head.add(jaw);
  for (const z of [-1, 1]) {
    const e = new THREE.Mesh(new THREE.SphereGeometry(0.012, 16, 12), M.glow('#7fe0ff', 4)); e.position.set(0.03, 0.02, z * 0.038); head.add(e);
    const horn = new THREE.Mesh(sweepGeometry(new THREE.CatmullRomCurve3([new THREE.Vector3(0, 0, 0), new THREE.Vector3(-0.03, 0.03, z * 0.01), new THREE.Vector3(-0.08, 0.04, z * 0.02)]), (t) => lerp(0.011, 0.002, t), 16, 8), gold);
    horn.position.set(-0.01, 0.04, z * 0.025); head.add(horn);
    const whisker = new THREE.Mesh(sweepGeometry(new THREE.CatmullRomCurve3([new THREE.Vector3(0, 0, 0), new THREE.Vector3(0.02, -0.02, z * 0.04), new THREE.Vector3(-0.02, -0.05, z * 0.08)]), (t) => lerp(0.003, 0.001, t), 16, 5), gold);
    whisker.position.set(0.09, -0.01, z * 0.02); head.add(whisker);
  }
  // tiny Santa hat on the dragon
  const hat = new THREE.Group(); hat.position.set(-0.005, 0.048, 0); hat.rotation.z = 0.3; head.add(hat);
  const cone = new THREE.Mesh(sweepGeometry(new THREE.CatmullRomCurve3([new THREE.Vector3(0, 0, 0), new THREE.Vector3(0, 0.04, 0), new THREE.Vector3(-0.03, 0.06, 0)]), (t) => lerp(0.032, 0.004, t), 24, 16), M.velvet('#b0101f'));
  hat.add(cone);
  const trim = new THREE.Mesh(new THREE.TorusGeometry(0.032, 0.009, 12, 32), M.fur()); trim.rotation.x = Math.PI / 2; hat.add(trim);
  const pom = new THREE.Mesh(new THREE.SphereGeometry(0.011, 16, 12), M.fur()); pom.position.set(-0.032, 0.06, 0); hat.add(pom);
  // wings: satin membrane + golden bones
  const membrane = new THREE.MeshPhysicalMaterial({ color: '#b3162a', roughness: 0.4, sheen: 1, sheenColor: new THREE.Color('#ff8a8a'), transmission: 0.35, thickness: 0.02, side: THREE.DoubleSide });
  const wings = [];
  for (const z of [-1, 1]) {
    const s = new THREE.Shape();
    s.moveTo(0, 0); s.lineTo(0.03, 0.2); s.quadraticCurveTo(0.0, 0.16, -0.04, 0.17); s.quadraticCurveTo(-0.05, 0.11, -0.09, 0.12); s.quadraticCurveTo(-0.08, 0.05, -0.12, 0.04); s.lineTo(0, 0);
    const wGeo = new THREE.ExtrudeGeometry(s, { depth: 0.002, bevelEnabled: false, curveSegments: 16 });
    const pivot = new THREE.Group();
    const wm = new THREE.Mesh(wGeo, membrane); wm.rotation.x = -Math.PI / 2 * z; pivot.add(wm);
    for (const [x, y] of [[0.03, 0.2], [-0.04, 0.17], [-0.09, 0.12]]) {
      const bone = new THREE.Mesh(sweepGeometry(new THREE.CatmullRomCurve3([new THREE.Vector3(0, 0, 0), new THREE.Vector3(x, 0, -y * z)]), (t) => lerp(0.005, 0.0015, t), 8, 6), gold);
      pivot.add(bone);
    }
    dragon.add(pivot); wings.push({ pivot, z });
  }
  const breath = c.track(new ParticleSystem({ max: 700, map: flakeSprite(), rate: 0, life: [0.5, 1.1], size: [0.02, 0.06], color: ['#ffffff', '#9fe0ff', '#cfefff'], sizeEnd: 2, drag: 1.5, spin: [-4, 4], intensity: 2.2 }));
  const glitter = c.track(new ParticleSystem({ max: 400, map: sparkleSprite(), rate: 0, life: [0.6, 1.3], size: [0.02, 0.05], color: ['#ffd36b', '#ff4d6a', '#4dff8a'], gravity: [0, -0.1, 0], intensity: 2.4 }));
  c.root.add(breath, glitter);
  // flight path: lazy figure-eight beside the player; body follows the path history
  const path = (t, out) => out.set(Math.sin(t * 0.6) * 0.28, Math.sin(t * 1.2) * 0.1 + 0.05, Math.sin(t * 1.2) * 0.16);
  const tmp = new THREE.Vector3(), tmp2 = new THREE.Vector3(), fwd = new THREE.Vector3(), mouth = new THREE.Vector3();
  let nextBreath = 1.5, breathing = 0;
  c.on((t, dt) => {
    for (let i = 0; i < SEG; i++) {
      const lag = i * 0.075;
      path(t - lag, tmp); tmp.y += Math.sin(t * 5 - i * 0.55) * 0.018;
      segments[i].position.copy(tmp);
      path(t - lag + 0.02, tmp2); segments[i].lookAt(tmp2); segments[i].rotateY(-Math.PI / 2);
    }
    path(t - SEG * 0.075, tmp); tip.position.copy(tmp); path(t - SEG * 0.075 + 0.05, tmp2); tip.lookAt(tmp2); tip.rotateX(-Math.PI / 2);
    path(t + 0.06, tmp); head.position.copy(tmp); head.position.y += 0.02;
    path(t + 0.12, tmp2); head.lookAt(tmp2); head.rotateY(-Math.PI / 2);
    fwd.subVectors(tmp2, tmp).normalize();
    const sh = segments[3];
    wings.forEach(({ pivot, z }) => { pivot.position.copy(sh.position).add(new THREE.Vector3(0, 0.03, 0)); pivot.quaternion.copy(sh.quaternion); pivot.rotateX(z * (0.5 + Math.sin(t * 8) * 0.6)); });
    if (t > nextBreath) { breathing = 0.9; nextBreath = t + 3.5; }
    if (breathing > 0) {
      breathing -= dt;
      mouth.set(0.1, -0.015, 0); head.localToWorld(mouth); c.root.worldToLocal(mouth);
      for (let i = 0; i < Math.round(dt * 450); i++) breath.spawnOne({ spawn: (P, V) => { P.copy(mouth); V.copy(fwd).multiplyScalar(0.9 + Math.random() * 0.4).add(new THREE.Vector3((Math.random() - 0.5) * 0.25, (Math.random() - 0.5) * 0.25, (Math.random() - 0.5) * 0.25)); } });
    }
    if (Math.random() < dt * 30) { const s = segments[(Math.random() * SEG) | 0]; glitter.spawnOne({ spawn: (P, V) => { P.copy(s.position); V.set(0, -0.05, 0); } }); }
    breath.tick(dt); glitter.tick(dt);
  });
  return c;
}

