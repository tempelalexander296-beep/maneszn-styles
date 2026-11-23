import * as THREE from 'three';
import { RoundedBoxGeometry } from 'three/addons/geometries/RoundedBoxGeometry.js';
import { M } from '../materials.js';
import { candyCane, giftBox, hollySprig, ornamentBall, sweepGeometry, bowMesh, TAU } from '../geometry.js';
import { ParticleSystem } from '../particles.js';
import { sparkleSprite } from '../textures.js';
import { cosmetic } from './base.js';

/* ------------------------------------------------------------- Day 3: Candy Cane Sword */
export function candyCaneSword() {
  const c = cosmetic('hand');
  const g = new THREE.Group(); c.root.add(g);
  // blade: tapered profile with fuller, twisted along its length like pulled sugar
  const s = new THREE.Shape();
  s.moveTo(-0.045, 0); s.lineTo(-0.05, 0.6); s.quadraticCurveTo(-0.045, 0.74, 0, 0.84); s.quadraticCurveTo(0.045, 0.74, 0.05, 0.6); s.lineTo(0.045, 0); s.lineTo(-0.045, 0);
  const bladeGeo = new THREE.ExtrudeGeometry(s, { depth: 0.012, bevelEnabled: true, bevelThickness: 0.012, bevelSize: 0.02, bevelSegments: 6, curveSegments: 32, steps: 1 });
  bladeGeo.translate(0, 0, -0.006);
  const pos = bladeGeo.attributes.position;
  for (let i = 0; i < pos.count; i++) {
    const y = pos.getY(i), x = pos.getX(i), z = pos.getZ(i), a = y * 1.4;
    pos.setXYZ(i, x * Math.cos(a) - z * Math.sin(a), y, x * Math.sin(a) + z * Math.cos(a));
  }
  bladeGeo.computeVertexNormals();
  const bladeMat = M.candy('#c8102e', '#2f8f46', 1);
  bladeMat.map.repeat.set(8, 8);
  const blade = new THREE.Mesh(bladeGeo, bladeMat); blade.position.y = 0.1; blade.castShadow = true; g.add(blade);
  // glowing sugar edge
  const edge = new THREE.Mesh(new THREE.CylinderGeometry(0.003, 0.003, 0.8, 6), M.glow('#ffe0e6', 3)); edge.position.y = 0.48; g.add(edge);
  // crossguard: two candy-cane hooks + golden heart with gem
  for (const side of [-1, 1]) {
    const cane = candyCane(0.12, 0.016, '#c8102e');
    cane.position.set(0, 0.09, 0); cane.rotation.z = side * -Math.PI / 2; cane.scale.x = side;
    g.add(cane);
  }
  const guard = new THREE.Mesh(new THREE.SphereGeometry(0.035, 24, 16), M.gold()); guard.scale.set(1.2, 0.8, 0.7); guard.position.y = 0.09; g.add(guard);
  const gem = new THREE.Mesh(new THREE.OctahedronGeometry(0.018, 1), M.gem('#2fd06a', { emissiveIntensity: 1.5 })); gem.position.set(0, 0.09, 0.024); g.add(gem);
  const holly = hollySprig(0.09); holly.position.set(0.04, 0.1, 0.02); holly.rotation.z = -0.6; g.add(holly);
  // grip wrapped in green-white ribbon, pommel ornament
  const grip = new THREE.Mesh(new THREE.CylinderGeometry(0.018, 0.02, 0.16, 32, 1), M.candy('#1f7a3f', null, 1)); grip.material.map.repeat.set(1, 3); grip.position.y = -0.0; g.add(grip);
  const pommel = ornamentBall('#c8102e', 0.03); pommel.rotation.x = Math.PI; pommel.position.y = -0.1; g.add(pommel);
  const sparks = c.track(new ParticleSystem({ max: 200, map: sparkleSprite(), rate: 35, life: [0.5, 1.2], size: [0.02, 0.05], color: ['#ffffff', '#ff6b81', '#ffd36b'], intensity: 2.4, gravity: [0, -0.1, 0],
    spawn: (P, V) => { P.set((Math.random() - 0.5) * 0.08, 0.15 + Math.random() * 0.75, (Math.random() - 0.5) * 0.04); V.set((Math.random() - 0.5) * 0.06, 0, (Math.random() - 0.5) * 0.06); }, prewarm: 1.5 }));
  g.add(sparks);
  g.rotation.set(0.25, 0, -0.18);
  c.on((t, dt) => { g.rotation.y = Math.sin(t * 0.6) * 0.25; g.position.y = Math.sin(t * 1.4) * 0.01; sparks.tick(dt); });
  return c;
}

/* ------------------------------------------------------------- Day 14: Present Hammer */
export function presentHammer() {
  const c = cosmetic('hand');
  const g = new THREE.Group(); c.root.add(g);
  const head = new THREE.Group(); head.position.y = 0.62; g.add(head);
  const box = giftBox({ w: 0.36, h: 0.22, d: 0.22, palette: 'green', motif: 'plaid', ribbon: '#c8102e' });
  box.position.y = -0.11; head.add(box);
  // golden strike faces
  for (const side of [-1, 1]) {
    const face = new THREE.Mesh(new RoundedBoxGeometry(0.03, 0.2, 0.2, 3, 0.01), M.gold());
    face.position.x = side * 0.195; head.add(face);
    const stud = new THREE.Mesh(new THREE.SphereGeometry(0.03, 20, 14), M.gold()); stud.position.x = side * 0.21; stud.scale.x = 0.5; head.add(stud);
  }
  const bigBow = bowMesh(0.16, M.satin('#c8102e')); bigBow.position.y = 0.13; head.add(bigBow);
  const tag = new THREE.Mesh(new RoundedBoxGeometry(0.06, 0.04, 0.004, 2, 0.002), M.paper('gold', 'stars', 0.2)); tag.position.set(0.12, 0.03, 0.115); tag.rotation.z = 0.3; head.add(tag);
  // magic ribbon ring orbiting the head
  const ring = new THREE.Mesh(new THREE.TorusGeometry(0.3, 0.006, 8, 120), M.glow('#ffd36b', 4)); ring.rotation.x = Math.PI / 2.3; head.add(ring);
  // handle
  const handle = new THREE.Mesh(new THREE.CylinderGeometry(0.022, 0.026, 0.62, 32), M.candy('#c8102e', '#ffd36b', 1));
  handle.material.map.repeat.set(1, 5); handle.position.y = 0.2; handle.castShadow = true; g.add(handle);
  for (const y of [-0.11, 0.49]) { const cap = new THREE.Mesh(new THREE.CylinderGeometry(0.032, 0.032, 0.03, 24), M.gold()); cap.position.y = y; g.add(cap); }
  const pommel = ornamentBall('#ffd36b', 0.035); pommel.rotation.x = Math.PI; pommel.position.y = -0.15; g.add(pommel);
  const sparks = c.track(new ParticleSystem({ max: 300, map: sparkleSprite(), rate: 50, life: [0.6, 1.2], size: [0.03, 0.07], color: ['#ffd36b', '#ff3355', '#2be07a'], intensity: 2.5, gravity: [0, -0.4, 0],
    spawn: (P, V) => { const a = Math.random() * TAU; P.set(Math.cos(a) * 0.3, 0.62 + Math.sin(a) * 0.1, Math.sin(a) * 0.3); V.set(0, 0.1, 0); }, prewarm: 1 }));
  g.add(sparks);
  g.rotation.set(0.15, 0, -0.25);
  g.position.y = -0.05;
  c.on((t, dt) => { ring.rotation.z = t * 2; g.rotation.y = Math.sin(t * 0.5) * 0.3; head.rotation.x = Math.sin(t * 1.3) * 0.03; sparks.tick(dt); });
  return c;
}

