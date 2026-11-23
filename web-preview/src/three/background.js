import * as THREE from 'three';
import { christmasTree, giftBox, rand, TAU } from './geometry.js';
import { M } from './materials.js';
import { ParticleSystem, particleUniformScale } from './particles.js';
import { flakeSprite, glowSprite, noiseTexture } from './textures.js';

/**
 * Atmospheric 3D winter landscape behind the calendar: snowy hills, decorated trees with
 * blinking lights, gifts, a distant forest, warm bokeh lights, fog and volumetric-looking snowfall.
 */
export class WinterBackground {
  constructor(canvas) {
    const r = this.renderer = new THREE.WebGLRenderer({ canvas, antialias: true, powerPreference: 'low-power' });
    r.setPixelRatio(Math.min(window.devicePixelRatio, 1.25));
    r.toneMapping = THREE.ACESFilmicToneMapping; r.toneMappingExposure = 1.1;
    r.outputColorSpace = THREE.SRGBColorSpace;
    const s = this.scene = new THREE.Scene();
    s.background = new THREE.Color('#0a1328');
    s.fog = new THREE.FogExp2('#101c38', 0.035);
    this.camera = new THREE.PerspectiveCamera(45, 1, 0.1, 200);
    this.camera.position.set(0, 2.2, 9);
    this.uScale = 1;

    s.add(new THREE.HemisphereLight('#7ea6ff', '#1a1020', 0.9));
    const moon = new THREE.DirectionalLight('#bcd4ff', 1.4); moon.position.set(-6, 10, -4); s.add(moon);
    this.warm = [];
    for (const [x, z] of [[-6, -1], [6, -2], [0, -6]]) { const l = new THREE.PointLight('#ffb35c', 6, 14, 1.6); l.position.set(x, 1.5, z); s.add(l); this.warm.push(l); }

    // sky glow / moon
    const moonDisc = new THREE.Sprite(new THREE.SpriteMaterial({ map: glowSprite(), color: '#dfe9ff', fog: false, depthWrite: false, blending: THREE.AdditiveBlending }));
    moonDisc.scale.setScalar(14); moonDisc.position.set(-12, 16, -60); s.add(moonDisc);

    // terrain
    const g = new THREE.PlaneGeometry(120, 80, 160, 100);
    const p = g.attributes.position;
    for (let i = 0; i < p.count; i++) {
      const x = p.getX(i), y = p.getY(i);
      const d = Math.max(0, -y - 4) * 0.12;
      p.setZ(i, Math.sin(x * 0.18) * 0.6 + Math.cos(y * 0.22 + x * 0.05) * 0.5 + d * d * 0.6 + Math.sin(x * 0.7 + y) * 0.08);
    }
    g.computeVertexNormals();
    const ground = new THREE.Mesh(g, M.snow({ color: new THREE.Color('#dbe6f8'), roughness: 0.8, bumpMap: noiseTexture(21, 512, 6), bumpScale: 2 }));
    ground.rotation.x = -Math.PI / 2; ground.position.y = -0.4; s.add(ground);

    // hero trees
    this.trees = [];
    [[-5.2, -1.5, 3.4], [5.4, -2.5, 3.8], [-8.5, -6, 4.2], [8.8, -7, 4.5], [-2.8, -9, 3], [3.2, -11, 3.4]].forEach(([x, z, h], i) => {
      const t = christmasTree({ height: h, seed: 70 + i }); t.position.set(x, -0.4, z); s.add(t); this.trees.push(t);
    });
    // distant forest (instanced)
    const coneGeo = new THREE.ConeGeometry(0.7, 2.4, 7); coneGeo.translate(0, 1.2, 0);
    const forest = new THREE.InstancedMesh(coneGeo, new THREE.MeshStandardMaterial({ color: '#0e3322', roughness: 0.9, flatShading: true }), 220);
    const R = rand(5), m = new THREE.Matrix4();
    for (let i = 0; i < 220; i++) {
      const x = (R() - 0.5) * 90, z = -14 - R() * 40, sc = 0.8 + R() * 1.6;
      m.compose(new THREE.Vector3(x, -0.4 + Math.sin(x * 0.18) * 0.5, z), new THREE.Quaternion(), new THREE.Vector3(sc, sc * (0.9 + R() * 0.5), sc));
      forest.setMatrixAt(i, m);
    }
    s.add(forest);
    // gifts in the snow
    [[-3.6, 0.6, 0.7, 'red', 'stars'], [-3.0, 1.2, 0.45, 'green', 'dots'], [3.8, 0.2, 0.8, 'gold', 'stripes'], [4.6, 1.0, 0.5, 'white', 'snowflakes'], [-4.4, 1.5, 0.35, 'night', 'stars']].forEach(([x, z, sz, pal, motif], i) => {
      const b = giftBox({ w: sz, h: sz * 0.9, d: sz, palette: pal, motif, ribbon: i % 2 ? '#c8102e' : '#ffd36b', ribbonMetal: !(i % 2) });
      b.position.set(x, -0.4, z); b.rotation.y = i * 0.7; s.add(b);
    });
    // bokeh lights
    const bokeh = new THREE.Group();
    const colors = ['#ffcf6b', '#ff6b6b', '#ffe8b0', '#8fd3ff'];
    for (let i = 0; i < 40; i++) {
      const sp = new THREE.Sprite(new THREE.SpriteMaterial({ map: glowSprite(), color: colors[i % 4], transparent: true, opacity: 0.35, blending: THREE.AdditiveBlending, depthWrite: false, fog: false }));
      sp.position.set((R() - 0.5) * 30, R() * 7, -2 - R() * 18); sp.scale.setScalar(0.3 + R() * 0.9);
      sp.userData.ph = R() * TAU; bokeh.add(sp);
    }
    s.add(bokeh); this.bokeh = bokeh;
    // mist layer
    const mist = new THREE.Mesh(new THREE.PlaneGeometry(120, 6), new THREE.MeshBasicMaterial({ map: glowSprite(), color: '#6f88c4', transparent: true, opacity: 0.25, depthWrite: false }));
    mist.position.set(0, 0.6, -10); s.add(mist);
    // snowfall: near and far layers
    this.snow = new ParticleSystem({
      max: 2600, map: flakeSprite(), rate: 420, life: [6, 9], size: [0.05, 0.14], color: ['#ffffff', '#e4f0ff'], spin: [-1, 1], intensity: 1.2, fadeIn: 0.08,
      spawn: (P, V) => { P.set((Math.random() - 0.5) * 30, 9, 8 - Math.random() * 24); V.set(0.2, -1.1 - Math.random() * 0.6, 0); },
      force: (k, P, V, dt, t) => { V[k] += Math.sin(t * 0.5 + P[k + 1]) * 0.25 * dt; },
      prewarm: 9,
    });
    s.add(this.snow);
    this.mouse = new THREE.Vector2();
    window.addEventListener('pointermove', (e) => { this.mouse.set(e.clientX / innerWidth - 0.5, e.clientY / innerHeight - 0.5); }, { passive: true });
    this.paused = false; this.time = 0;
    this.resize(); window.addEventListener('resize', () => this.resize());
    const loop = (now) => {
      requestAnimationFrame(loop);
      if (this.paused || document.hidden) { this.last = now; return; }
      const dt = Math.min(0.05, (now - (this.last ?? now)) / 1000); this.last = now; this.time += dt;
      this.tick(dt);
    };
    requestAnimationFrame(loop);
  }

  resize() {
    const w = innerWidth, h = innerHeight;
    this.renderer.setSize(w, h, false);
    this.camera.aspect = w / h; this.camera.updateProjectionMatrix();
    this.pScale = h * this.renderer.getPixelRatio() * 0.5 / Math.tan(THREE.MathUtils.degToRad(this.camera.fov / 2));
  }

  tick(dt) {
    const t = this.time;
    this.camera.position.x += ((this.mouse.x * 1.4 + Math.sin(t * 0.05) * 0.8) - this.camera.position.x) * 0.03;
    this.camera.position.y += ((2.2 - this.mouse.y * 0.6) - this.camera.position.y) * 0.03;
    this.camera.lookAt(0, 1.4, -4);
    this.trees.forEach((tr, i) => tr.userData.lights?.userData.update(t + i));
    this.bokeh.children.forEach((b) => { b.material.opacity = 0.2 + 0.2 * Math.sin(t * 0.8 + b.userData.ph); });
    this.warm.forEach((l, i) => { l.intensity = 6 + Math.sin(t * 2 + i) * 0.8; });
    this.snow.tick(dt);
    const shared = particleUniformScale.value; particleUniformScale.value = this.pScale;
    this.renderer.render(this.scene, this.camera);
    particleUniformScale.value = shared;
  }
}
