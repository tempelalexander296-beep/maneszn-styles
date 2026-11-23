import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { RoomEnvironment } from 'three/addons/environments/RoomEnvironment.js';
import { EffectComposer } from 'three/addons/postprocessing/EffectComposer.js';
import { RenderPass } from 'three/addons/postprocessing/RenderPass.js';
import { UnrealBloomPass } from 'three/addons/postprocessing/UnrealBloomPass.js';
import { OutputPass } from 'three/addons/postprocessing/OutputPass.js';
import { COSMETIC_BUILDERS } from './cosmetics/index.js';
import { buildMannequin, MOUNTS } from './avatar.js';
import { giftBox, disposeObject, TAU } from './geometry.js';
import { ParticleSystem, particleUniformScale } from './particles.js';
import { flakeSprite, sparkleSprite, setMaxAnisotropy, glowSprite } from './textures.js';
import { M } from './materials.js';
import { RARITIES } from '../data/rarities.js';

const FRAMING = {
  head: { target: [0, 1.62, 0], pos: [0.9, 1.85, 1.25] },
  back: { target: [0, 1.25, -0.2], pos: [2.1, 1.75, -2.6] },
  hand: { target: [0.35, 1.15, 0.1], pos: [1.6, 1.45, 1.9] },
  float: { target: [0.45, 1.55, 0.1], pos: [1.5, 1.85, 1.9] },
  companion: { target: [-0.55, 1.35, 0.15], pos: [-1.2, 1.7, 2.3] },
  aura: { target: [0, 1.05, 0], pos: [2.6, 2.0, 3.6] },
  trail: { target: [0, 0.8, 0], pos: [2.4, 2.1, 3.4] },
  mythic: { target: [0, 1.35, 0], pos: [3.4, 2.4, 4.6] },
};

const ease = (t) => 1 - Math.pow(1 - Math.min(Math.max(t, 0), 1), 3);
const easeInOut = (t) => { t = Math.min(Math.max(t, 0), 1); return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2; };

/**
 * The 3D showcase: physically based renderer, IBL, soft shadows, bloom, orbit controls,
 * mannequin + pedestal, reveal sequences and 4K capture.
 */
export class ShowcaseStage {
  constructor(canvas) {
    this.canvas = canvas;
    const r = this.renderer = new THREE.WebGLRenderer({ canvas, antialias: true, alpha: false, powerPreference: 'high-performance', preserveDrawingBuffer: false });
    r.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    r.toneMapping = THREE.ACESFilmicToneMapping;
    r.toneMappingExposure = 1.05;
    r.outputColorSpace = THREE.SRGBColorSpace;
    r.shadowMap.enabled = true;
    r.shadowMap.type = THREE.PCFShadowMap;
    setMaxAnisotropy(r.capabilities.getMaxAnisotropy());

    const scene = this.scene = new THREE.Scene();
    scene.background = new THREE.Color('#070b14');
    scene.fog = new THREE.FogExp2('#0a1020', 0.06);
    const pmrem = new THREE.PMREMGenerator(r);
    scene.environment = pmrem.fromScene(new RoomEnvironment(), 0.04).texture;
    scene.environmentIntensity = 0.75;

    this.camera = new THREE.PerspectiveCamera(35, 1, 0.05, 100);
    this.camera.position.set(3, 2, 4);
    this.controls = new OrbitControls(this.camera, canvas);
    Object.assign(this.controls, { enableDamping: true, dampingFactor: 0.06, minDistance: 0.6, maxDistance: 9, maxPolarAngle: Math.PI * 0.52, autoRotate: true, autoRotateSpeed: 0.6, enablePan: false });

    this.buildLights();
    this.buildSet();
    this.avatar = buildMannequin(); scene.add(this.avatar);

    this.composer = new EffectComposer(r);
    this.composer.addPass(new RenderPass(scene, this.camera));
    this.bloom = new UnrealBloomPass(new THREE.Vector2(512, 512), 0.75, 0.55, 0.88);
    this.composer.addPass(this.bloom);
    this.composer.addPass(new OutputPass());

    this.current = null; this.mount = new THREE.Group(); scene.add(this.mount);
    this.seq = null; this.time = 0; this.running = false; this.last = 0;
    this.camFrom = new THREE.Vector3(); this.camTo = new THREE.Vector3(); this.tgtFrom = new THREE.Vector3(); this.tgtTo = new THREE.Vector3(); this.camT = 1;
    this.resizeObs = new ResizeObserver(() => this.resize()); this.resizeObs.observe(canvas.parentElement);
    this.resize();
  }

  buildLights() {
    const s = this.scene;
    s.add(new THREE.HemisphereLight('#bcd7ff', '#2a1408', 0.5));
    const key = this.key = new THREE.DirectionalLight('#ffe2b8', 2.6);
    key.position.set(3, 6, 4); key.castShadow = true;
    key.shadow.mapSize.set(2048, 2048); key.shadow.radius = 4; key.shadow.bias = -0.0004; key.shadow.normalBias = 0.02;
    Object.assign(key.shadow.camera, { left: -3.5, right: 3.5, top: 4, bottom: -2, near: 0.5, far: 20 });
    s.add(key);
    const rim = new THREE.DirectionalLight('#7fb8ff', 2.2); rim.position.set(-4, 3, -5); s.add(rim);
    const fill = new THREE.DirectionalLight('#ffb2c0', 0.5); fill.position.set(-3, 1.5, 3); s.add(fill);
    this.rarityLight = new THREE.PointLight('#ffd36b', 2, 4, 2); this.rarityLight.position.set(0, 0.35, 0.9); s.add(this.rarityLight);
  }

  buildSet() {
    const s = this.scene;
    // backdrop: gradient dome
    const dome = new THREE.Mesh(new THREE.SphereGeometry(40, 32, 16), new THREE.ShaderMaterial({
      side: THREE.BackSide, depthWrite: false, fog: false,
      uniforms: { top: { value: new THREE.Color('#050913') }, mid: { value: new THREE.Color('#13213d') }, bot: { value: new THREE.Color('#1a0f12') } },
      vertexShader: 'varying vec3 vP; void main(){ vP = position; gl_Position = projectionMatrix*modelViewMatrix*vec4(position,1.0);} ',
      fragmentShader: 'uniform vec3 top; uniform vec3 mid; uniform vec3 bot; varying vec3 vP; void main(){ float h = normalize(vP).y; vec3 c = h>0.0 ? mix(mid, top, pow(h,0.6)) : mix(mid, bot, pow(-h,0.5)); gl_FragColor = vec4(c,1.0);} ',
    }));
    s.add(dome);
    // floor + pedestal
    const floor = new THREE.Mesh(new THREE.CircleGeometry(12, 64), M.snow({ color: new THREE.Color('#dfe9f7'), roughness: 0.85 }));
    floor.rotation.x = -Math.PI / 2; floor.receiveShadow = true; floor.position.y = -0.121; s.add(floor);
    const ped = new THREE.Group(); ped.position.y = -0.12; s.add(ped);
    const stone = new THREE.MeshPhysicalMaterial({ color: '#141821', roughness: 0.22, metalness: 0.2, clearcoat: 1, clearcoatRoughness: 0.08 });
    const p1 = new THREE.Mesh(new THREE.CylinderGeometry(1.05, 1.12, 0.08, 96), stone); p1.position.y = 0.04; p1.receiveShadow = true; p1.castShadow = true;
    const p2 = new THREE.Mesh(new THREE.CylinderGeometry(0.95, 1.0, 0.06, 96), stone); p2.position.y = 0.11; p2.receiveShadow = true;
    const gold = new THREE.Mesh(new THREE.TorusGeometry(1.0, 0.012, 12, 160), M.gold()); gold.rotation.x = Math.PI / 2; gold.position.y = 0.14;
    this.pedGlow = new THREE.Mesh(new THREE.TorusGeometry(1.085, 0.006, 8, 160), M.glow('#ffd36b', 3)); this.pedGlow.rotation.x = Math.PI / 2; this.pedGlow.position.y = 0.082;
    ped.add(p1, p2, gold, this.pedGlow);
    this.ambientSnow = new ParticleSystem({
      max: 900, map: flakeSprite(), rate: 120, life: [5, 8], size: [0.03, 0.07], color: ['#ffffff', '#d8ecff'], gravity: [0, -0.02, 0], intensity: 1.1, spin: [-1, 1],
      spawn: (P, V) => { P.set((Math.random() - 0.5) * 12, 5, (Math.random() - 0.5) * 12 - 2); V.set(0.05, -0.4, 0); }, prewarm: 8,
    });
    s.add(this.ambientSnow);
  }

  resize() {
    const el = this.canvas.parentElement; if (!el) return;
    const w = el.clientWidth || 1, h = el.clientHeight || 1;
    this.renderer.setSize(w, h, false);
    this.composer.setSize(w, h);
    this.camera.aspect = w / h; this.camera.updateProjectionMatrix();
    particleUniformScale.value = h * this.renderer.getPixelRatio() * 0.5 / Math.tan(THREE.MathUtils.degToRad(this.camera.fov / 2));
  }

  setAvatarVisible(v) { this.avatar.visible = v; }

  frame(slot, instant = false) {
    const f = FRAMING[slot] ?? FRAMING.aura;
    this.camFrom.copy(this.camera.position); this.tgtFrom.copy(this.controls.target);
    this.camTo.set(...f.pos); this.tgtTo.set(...f.target);
    this.camT = instant ? 1 : 0;
    if (instant) { this.camera.position.copy(this.camTo); this.controls.target.copy(this.tgtTo); }
  }

  clear() {
    if (this.current) { this.mount.remove(this.current.root); this.current.dispose(); disposeObject(this.current.root); this.current = null; }
    if (this.seq?.box) { this.scene.remove(this.seq.box); disposeObject(this.seq.box); }
    if (this.seq?.burst) { this.scene.remove(this.seq.burst); this.seq.burst.dispose(); }
    this.seq = null;
  }

  /**
   * Show a reward. With reveal=true, a gift box opens on the pedestal and the cosmetic
   * floats out of it. Resolves once the cosmetic has fully emerged.
   */
  show(reward, { reveal = false } = {}) {
    this.clear();
    const rarity = RARITIES[reward.rarity];
    this.rarityLight.color.set(rarity.color); this.pedGlow.material.emissive.set(rarity.color);
    const c = COSMETIC_BUILDERS[reward.id]();
    this.current = c;
    const mythic = reward.rarity === 'mythic';
    this.mount.position.copy(MOUNTS[c.slot]);
    this.mount.add(c.root);
    this.frame(mythic ? 'mythic' : c.slot, false);
    this.bloom.strength = mythic ? 0.95 : 0.75;
    if (!reveal) { this.mount.scale.setScalar(1); if (c.intro !== undefined) c.intro = 1; return Promise.resolve(); }

    // --- reveal sequence
    const palette = { mythic: ['royal', 'plaid', '#ffd36b'], legendary: ['gold', 'stars', '#7a1020'], epic: ['night', 'snowflakes', '#ffd36b'] }[reward.rarity] ?? ['red', 'stars', '#ffd36b'];
    const box = giftBox({ w: 0.5, h: 0.5, d: 0.5, palette: palette[0], motif: palette[1], ribbon: palette[2], ribbonMetal: true });
    box.position.set(0, 0.02, 0.75); box.scale.setScalar(mythic ? 1.3 : 1);
    const boxGlow = new THREE.Sprite(new THREE.SpriteMaterial({ map: glowSprite(), color: rarity.glow, blending: THREE.AdditiveBlending, depthWrite: false, opacity: 0 }));
    boxGlow.scale.setScalar(1.8); boxGlow.position.y = 0.4; box.add(boxGlow);
    this.scene.add(box);
    const burst = new ParticleSystem({ max: 2500, map: sparkleSprite(), rate: 0, life: [0.8, 2.2], size: [0.03, 0.1], color: [rarity.color, rarity.glow, '#ffffff', '#ffd36b'], gravity: [0, -0.6, 0], drag: 1.4, spin: [-4, 4], intensity: 2.8, emitting: false });
    const flakes = new ParticleSystem({ max: 600, map: flakeSprite(), rate: 0, life: [1, 2.5], size: [0.03, 0.08], color: ['#ffffff', '#cfeeff'], gravity: [0, -0.2, 0], drag: 0.8, spin: [-2, 2], intensity: 2, emitting: false });
    burst.add(flakes);
    this.scene.add(burst);
    this.mount.scale.setScalar(0.001);
    if (c.intro !== undefined) c.intro = 0;
    this.controls.autoRotate = false;
    const finalMount = MOUNTS[c.slot].clone();
    return new Promise((resolve) => {
      this.seq = { t: 0, box, boxGlow, burst, flakes, mythic, finalMount, resolve, opened: false, done: false, dur: mythic ? 7.5 : 3.6 };
      if (mythic) { this.camFrom.copy(this.camera.position); this.camTo.set(0.6, 1.0, 2.6); this.tgtTo.set(0, 0.6, 0.6); this.tgtFrom.copy(this.controls.target); this.camT = 0; }
    });
  }

  updateSequence(dt) {
    const s = this.seq; if (!s || s.done) return;
    s.t += dt;
    const t = s.t, m = s.mythic ? 2 : 1;
    const glowEnd = 0.9 * m, openAt = 1.1 * m, riseEnd = openAt + 1.6 * (s.mythic ? 1.8 : 1);
    // 1) gift glows & shakes, 2) snowflakes appear
    if (t < openAt) {
      const k = t / glowEnd;
      s.boxGlow.material.opacity = Math.min(1, k) * 0.9;
      const shake = Math.min(1, k) * 0.05;
      s.box.rotation.z = Math.sin(t * 40) * shake; s.box.rotation.x = Math.cos(t * 33) * shake * 0.6;
      s.box.position.y = 0.02 + Math.abs(Math.sin(t * 20)) * shake * 0.5;
      if (Math.random() < dt * 40) s.flakes.spawnOne({ spawn: (P, V) => { const a = Math.random() * TAU; P.set(Math.cos(a) * 0.5, 0.2 + Math.random() * 0.6, 0.75 + Math.sin(a) * 0.5); V.set(-Math.cos(a) * 0.15, 0.1, -Math.sin(a) * 0.15); } });
    }
    // 3) lid opens + burst
    if (t >= openAt && !s.opened) {
      s.opened = true;
      const n = s.mythic ? 1600 : 500;
      s.burst.burst(n, { spawn: (P, V) => { P.set(0, 0.55, 0.75); const a = Math.random() * TAU, u = Math.random() * 0.9 + 0.1; V.set(Math.cos(a) * u * 2.2, 1.2 + Math.random() * 2.6, Math.sin(a) * u * 2.2); } });
      s.flakes.burst(s.mythic ? 400 : 120, { spawn: (P, V) => { P.set(0, 0.55, 0.75); V.set((Math.random() - 0.5) * 2.5, Math.random() * 2.5, (Math.random() - 0.5) * 2.5); } });
      this.flash = 1;
    }
    if (s.opened) {
      const lid = s.box.userData.lid; const k = ease((t - openAt) / 0.7);
      lid.position.y = 0.86 * 0.5 + k * 1.4; lid.position.x = k * 0.45; lid.rotation.z = -k * 1.6; lid.rotation.x = k * 0.5;
      s.box.rotation.z *= 0.9; s.box.rotation.x *= 0.9;
      // 4) cosmetic floats out of the gift
      const r = easeInOut((t - openAt) / (riseEnd - openAt));
      const from = new THREE.Vector3(0, 0.55, 0.75);
      this.mount.position.lerpVectors(from, s.finalMount, r);
      this.mount.scale.setScalar(Math.max(0.001, r));
      if (this.current.intro !== undefined) this.current.intro = r;
      // box sinks away
      const sink = ease((t - riseEnd + 0.3) / 0.8);
      s.box.scale.setScalar((s.mythic ? 1.3 : 1) * (1 - sink) + 0.001);
      s.boxGlow.material.opacity = 0.9 * (1 - sink);
      if (s.mythic && t > openAt + 0.3 && this.camT >= 1 && !s.mythicPan) { s.mythicPan = true; this.frame('mythic'); }
    }
    // 5) particles & lights, 6) done → UI shows name + rarity
    if (t >= riseEnd && !s.resolved) { s.resolved = true; this.controls.autoRotate = true; s.resolve(); }
    if (t >= s.dur + 2) { s.done = true; this.scene.remove(s.box); }
  }

  start() {
    if (this.running) return;
    this.running = true; this.last = performance.now();
    const loop = (now) => {
      if (!this.running) return;
      const dt = Math.min(0.05, (now - this.last) / 1000); this.last = now; this.time += dt;
      this.tick(dt);
      requestAnimationFrame(loop);
    };
    requestAnimationFrame(loop);
  }

  stop() { this.running = false; }

  tick(dt) {
    if (this.camT < 1) {
      this.camT = Math.min(1, this.camT + dt / (this.seq?.mythic ? 2.2 : 1.1));
      const k = easeInOut(this.camT);
      this.camera.position.lerpVectors(this.camFrom, this.camTo, k);
      this.controls.target.lerpVectors(this.tgtFrom, this.tgtTo, k);
    }
    this.updateSequence(dt);
    if (this.seq) { this.seq.burst.tick(dt); this.seq.flakes.tick(dt); }
    this.current?.update(this.time, dt);
    this.ambientSnow.tick(dt);
    if (this.flash) { this.flash *= Math.pow(0.02, dt); this.renderer.toneMappingExposure = 1.05 + this.flash * 1.2; if (this.flash < 0.01) { this.flash = 0; this.renderer.toneMappingExposure = 1.05; } }
    this.rarityLight.intensity = 2 + Math.sin(this.time * 2) * 0.6;
    this.controls.update();
    this.composer.render();
  }

  /** Render a 3840×2160 still of the current view and return a PNG blob. */
  async capture4K(width = 3840, height = 2160) {
    const r = this.renderer, prevPR = r.getPixelRatio();
    const el = this.canvas.parentElement; const w = el.clientWidth, h = el.clientHeight;
    r.setPixelRatio(1); r.setSize(width, height, false); this.composer.setPixelRatio?.(1); this.composer.setSize(width, height);
    this.camera.aspect = width / height; this.camera.updateProjectionMatrix();
    const prevScale = particleUniformScale.value;
    particleUniformScale.value = height * 0.5 / Math.tan(THREE.MathUtils.degToRad(this.camera.fov / 2));
    this.composer.render();
    const blob = await new Promise((res) => r.domElement.toBlob(res, 'image/png'));
    r.setPixelRatio(prevPR); this.composer.setPixelRatio?.(prevPR);
    particleUniformScale.value = prevScale;
    this.resize(); void w; void h;
    return blob;
  }
}
