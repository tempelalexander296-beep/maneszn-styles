import * as THREE from 'three';
import { RoundedBoxGeometry } from 'three/addons/geometries/RoundedBoxGeometry.js';
import { M } from '../materials.js';
import { giftBox, bowMesh, hollySprig, sweepGeometry, ornamentBall } from '../geometry.js';
import { ParticleSystem } from '../particles.js';
import { sparkleSprite } from '../textures.js';
import { cosmetic } from './base.js';

/* ------------------------------------------------------------- Day 5: Gift Backpack */
export function giftBackpack() {
  const c = cosmetic('back');
  const g = new THREE.Group(); g.position.set(0, -0.2, -0.07); c.root.add(g);
  const pack = giftBox({ w: 0.34, h: 0.4, d: 0.2, palette: 'red', motif: 'plaid', ribbon: '#ffd36b', ribbonMetal: true, bow: false });
  pack.position.z = -0.1; g.add(pack);
  // front pocket = a smaller present
  const pocket = giftBox({ w: 0.22, h: 0.14, d: 0.07, palette: 'green', motif: 'dots', ribbon: '#ffffff', bow: false });
  pocket.position.set(0, 0.04, -0.235); g.add(pocket);
  const bow = bowMesh(0.2, M.gold({ roughness: 0.2 })); bow.position.set(0, 0.42, -0.1); g.add(bow);
  const holly = hollySprig(0.12); holly.position.set(0.12, 0.4, -0.02); holly.rotation.set(-0.3, 0.4, -0.5); g.add(holly);
  // shoulder straps wrapping around the avatar's shoulders
  const strapMat = M.satin('#7a0c22');
  for (const s of [-1, 1]) {
    const curve = new THREE.CatmullRomCurve3([
      new THREE.Vector3(s * 0.1, 0.36, -0.02), new THREE.Vector3(s * 0.12, 0.47, 0.05), new THREE.Vector3(s * 0.13, 0.4, 0.2),
      new THREE.Vector3(s * 0.13, 0.15, 0.24), new THREE.Vector3(s * 0.12, 0.02, 0.12), new THREE.Vector3(s * 0.12, 0.03, -0.02),
    ]);
    g.add(new THREE.Mesh(sweepGeometry(curve, () => ({ x: 0.022, y: 0.006 }), 64, 8), strapMat));
    const buckle = new THREE.Mesh(new RoundedBoxGeometry(0.05, 0.035, 0.012, 2, 0.004), M.gold()); buckle.position.copy(curve.getPointAt(0.55)).add(new THREE.Vector3(0, 0, 0.01)); g.add(buckle);
  }
  // gift tag + hanging ornament charm
  const tag = new THREE.Mesh(new RoundedBoxGeometry(0.07, 0.045, 0.004, 2, 0.002), M.paper('white', 'stars', 0.3)); tag.position.set(-0.13, 0.27, -0.215); tag.rotation.z = 0.25; g.add(tag);
  const charm = ornamentBall('#ffd36b', 0.03); charm.position.set(0.15, 0.2, -0.215); g.add(charm);
  g.traverse((o) => { if (o.isMesh) o.castShadow = true; });
  const sparkle = c.track(new ParticleSystem({ max: 100, map: sparkleSprite(), rate: 14, life: [0.4, 0.9], size: [0.03, 0.06], color: ['#ffd36b', '#ffffff'], intensity: 2.6,
    spawn: (P) => { P.set((Math.random() - 0.5) * 0.36, -0.2 + Math.random() * 0.45, -0.3); }, prewarm: 1 }));
  c.root.add(sparkle);
  c.on((t, dt) => { g.position.y = -0.2 + Math.abs(Math.sin(t * 2)) * 0.008; charm.rotation.z = Math.sin(t * 2.4) * 0.3; sparkle.tick(dt); });
  return c;
}
