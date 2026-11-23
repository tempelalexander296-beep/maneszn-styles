import * as THREE from 'three';
import { sweepGeometry, lerp } from './geometry.js';

/** Stylised faceless display mannequin (≈1.8 m) + mount points for every cosmetic slot. */
export const MOUNTS = Object.freeze({
  head: new THREE.Vector3(0, 1.62, 0),
  back: new THREE.Vector3(0, 1.33, -0.15),
  hand: new THREE.Vector3(0.36, 0.92, 0.1),
  float: new THREE.Vector3(0.58, 1.62, 0.12),
  companion: new THREE.Vector3(-0.72, 1.3, 0.22),
  aura: new THREE.Vector3(0, 0, 0),
  trail: new THREE.Vector3(0, 0, 0),
});

export function buildMannequin() {
  const g = new THREE.Group();
  const skin = new THREE.MeshPhysicalMaterial({ color: '#2b3344', roughness: 0.32, metalness: 0.1, clearcoat: 0.8, clearcoatRoughness: 0.2, sheen: 0.6, sheenColor: new THREE.Color('#8fb3ff') });
  const accent = new THREE.MeshPhysicalMaterial({ color: '#c9a45a', metalness: 1, roughness: 0.25 });
  const add = (geo, mat = skin) => { const m = new THREE.Mesh(geo, mat); m.castShadow = true; m.receiveShadow = true; g.add(m); return m; };
  const limb = (pts, r0, r1) => add(sweepGeometry(new THREE.CatmullRomCurve3(pts.map((p) => new THREE.Vector3(...p))), (t) => lerp(r0, r1, t), 32, 20));

  const head = add(new THREE.SphereGeometry(0.115, 48, 32)); head.scale.set(0.92, 1.1, 1); head.position.y = 1.62;
  limb([[0, 1.43, 0], [0, 1.52, 0.005]], 0.048, 0.042); // neck
  // torso via lathe (chest → waist → hips)
  const torsoProfile = [[0.0, 0.92], [0.15, 0.93], [0.17, 0.98], [0.15, 1.08], [0.135, 1.16], [0.16, 1.28], [0.19, 1.36], [0.17, 1.42], [0.06, 1.46], [0, 1.465]].map(([x, y]) => new THREE.Vector2(x, y));
  const torso = add(new THREE.LatheGeometry(torsoProfile, 48)); torso.scale.z = 0.62;
  const belt = add(new THREE.TorusGeometry(0.142, 0.008, 10, 64), accent); belt.rotation.x = Math.PI / 2; belt.scale.y = 0.62; belt.position.y = 1.0;
  for (const s of [-1, 1]) {
    add(new THREE.SphereGeometry(0.058, 24, 16)).position.set(s * 0.2, 1.39, 0);
    limb([[s * 0.21, 1.38, 0], [s * 0.27, 1.16, 0.0], [s * 0.31, 1.02, 0.04]], 0.05, 0.038);    // upper/fore arm
    limb([[s * 0.31, 1.02, 0.04], [s * 0.34, 0.92, 0.09]], 0.036, 0.03);
    add(new THREE.SphereGeometry(0.04, 20, 14)).position.set(s * 0.345, 0.9, 0.1);           // hand
    limb([[s * 0.085, 0.95, 0], [s * 0.095, 0.5, 0.01], [s * 0.1, 0.09, 0]], 0.075, 0.045);     // leg
    const foot = add(new THREE.SphereGeometry(0.06, 24, 16)); foot.scale.set(0.8, 0.5, 1.6); foot.position.set(s * 0.1, 0.045, 0.05);
  }
  const emblem = add(new THREE.OctahedronGeometry(0.02, 0), new THREE.MeshStandardMaterial({ color: '#000', emissive: '#ffd36b', emissiveIntensity: 3 }));
  emblem.position.set(0, 1.28, 0.115);
  return g;
}
