import * as THREE from 'three';

/** Minimal cosmetic container: a root object + per-frame updaters. */
export function cosmetic(slot) {
  const root = new THREE.Group();
  const updaters = [];
  const disposables = [];
  return {
    slot, root,
    on(fn) { updaters.push(fn); return fn; },
    track(d) { disposables.push(d); return d; },
    update(t, dt) { for (const fn of updaters) fn(t, dt); },
    dispose() { disposables.forEach((d) => d.dispose?.()); },
  };
}

/** Showcase path used by every trail cosmetic (feet height, around the avatar). */
export function trailPath(t, out = new THREE.Vector3()) {
  const a = t * 1.25;
  return out.set(Math.cos(a) * 1.15, 0.45 + Math.sin(t * 2.5) * 0.22, Math.sin(a) * 1.15);
}
