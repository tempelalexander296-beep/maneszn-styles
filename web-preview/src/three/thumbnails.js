import * as THREE from 'three';
import { RoomEnvironment } from 'three/addons/environments/RoomEnvironment.js';
import { EffectComposer } from 'three/addons/postprocessing/EffectComposer.js';
import { RenderPass } from 'three/addons/postprocessing/RenderPass.js';
import { UnrealBloomPass } from 'three/addons/postprocessing/UnrealBloomPass.js';
import { OutputPass } from 'three/addons/postprocessing/OutputPass.js';
import { COSMETIC_BUILDERS } from './cosmetics/index.js';
import { disposeObject } from './geometry.js';
import { particleUniformScale, RibbonTrail } from './particles.js';

/** Bounds of solid geometry only (ignores particles, sprites, ribbons). */
function solidBounds(root) {
  const box = new THREE.Box3(), tmp = new THREE.Box3();
  root.updateMatrixWorld(true);
  root.traverse((o) => {
    if (!o.visible || o.isPoints || o.isSprite || o instanceof RibbonTrail || o.isLine || !o.geometry) return;
    if (o.isInstancedMesh) { o.computeBoundingBox(); tmp.copy(o.boundingBox).applyMatrix4(o.matrixWorld); }
    else { if (!o.geometry.boundingBox) o.geometry.computeBoundingBox(); tmp.copy(o.geometry.boundingBox).applyMatrix4(o.matrixWorld); }
    box.union(tmp);
  });
  return box;
}

/**
 * Renders door preview images of every cosmetic with a temporary offscreen renderer
 * (bloom included). Yields between items so the UI stays responsive.
 */
export async function renderThumbnails(rewards, size, onThumb) {
  const canvas = document.createElement('canvas');
  const renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: true, preserveDrawingBuffer: true });
  renderer.setPixelRatio(1); renderer.setSize(size, size, false);
  renderer.toneMapping = THREE.ACESFilmicToneMapping; renderer.outputColorSpace = THREE.SRGBColorSpace;
  renderer.setClearColor(0x000000, 0);
  const scene = new THREE.Scene();
  const pmrem = new THREE.PMREMGenerator(renderer);
  scene.environment = pmrem.fromScene(new RoomEnvironment(), 0.04).texture;
  scene.add(new THREE.HemisphereLight('#cfe2ff', '#40200c', 0.8));
  const key = new THREE.DirectionalLight('#fff0d8', 2.4); key.position.set(2, 4, 3); scene.add(key);
  const rim = new THREE.DirectionalLight('#8fc4ff', 2); rim.position.set(-3, 2, -3); scene.add(rim);
  const camera = new THREE.PerspectiveCamera(30, 1, 0.01, 100);
  const rt = new THREE.WebGLRenderTarget(size, size, { type: THREE.HalfFloatType });
  const composer = new EffectComposer(renderer, rt);
  composer.addPass(new RenderPass(scene, camera));
  composer.addPass(new UnrealBloomPass(new THREE.Vector2(size, size), 0.6, 0.4, 0.9));
  composer.addPass(new OutputPass());
  const prevScale = particleUniformScale.value;

  for (const reward of rewards) {
    const c = COSMETIC_BUILDERS[reward.id]();
    scene.add(c.root);
    // advance animation a little for a good-looking pose + populated particles
    for (let i = 0; i < 20; i++) c.update(1.2 + i / 30, 1 / 30);
    const b = solidBounds(c.root);
    const centre = b.getCenter(new THREE.Vector3()), sz = b.getSize(new THREE.Vector3());
    const radius = Math.max(sz.x, sz.y, sz.z) * 0.62 || 1;
    const dir = c.slot === 'back' ? new THREE.Vector3(0.45, 0.25, -1) : new THREE.Vector3(0.55, 0.35, 1);
    const dist = radius / Math.sin(THREE.MathUtils.degToRad(camera.fov / 2));
    camera.position.copy(centre).addScaledVector(dir.normalize(), dist);
    camera.near = dist / 50; camera.far = dist * 10; camera.updateProjectionMatrix();
    camera.lookAt(centre);
    particleUniformScale.value = size * 0.5 / Math.tan(THREE.MathUtils.degToRad(camera.fov / 2));
    composer.render();
    onThumb(reward, canvas.toDataURL('image/webp', 0.9));
    scene.remove(c.root); c.dispose(); disposeObject(c.root);
    await new Promise((r) => setTimeout(r, 0));
  }
  particleUniformScale.value = prevScale;
  composer.dispose?.(); rt.dispose(); pmrem.dispose(); renderer.dispose(); renderer.forceContextLoss();
}
