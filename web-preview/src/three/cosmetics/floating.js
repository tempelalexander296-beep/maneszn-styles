import * as THREE from 'three';
import { M } from '../materials.js';
import { christmasTree, bell, hollySprig, bowMesh, sweepGeometry, TAU, lerp } from '../geometry.js';
import { ParticleSystem } from '../particles.js';
import { flakeSprite, sparkleSprite, glowSprite } from '../textures.js';
import { cosmetic } from './base.js';

/* ------------------------------------------------------------- Day 9: Snow Globe */
export function snowGlobe() {
  const c = cosmetic('float');
  const g = new THREE.Group(); c.root.add(g);
  const R = 0.17;
  const glass = new THREE.Mesh(new THREE.SphereGeometry(R, 64, 48), M.glass({ thickness: 0.02, roughness: 0.0, iridescence: 0.2 }));
  glass.position.y = R * 0.85; g.add(glass);
  // base: turned dark wood + gold rings + plaque
  const pts = [[0, 0], [0.16, 0], [0.17, 0.015], [0.155, 0.03], [0.14, 0.06], [0.13, 0.09], [0.145, 0.1], [0, 0.1]].map(([x, y]) => new THREE.Vector2(x, y));
  const base = new THREE.Mesh(new THREE.LatheGeometry(pts, 64), new THREE.MeshPhysicalMaterial({ color: '#3a1d10', roughness: 0.35, clearcoat: 1 }));
  base.position.y = -0.1 + R * 0.85 - R * 0.75; g.add(base);
  for (const y of [0.002, 0.098]) { const r = new THREE.Mesh(new THREE.TorusGeometry(y < 0.05 ? 0.163 : 0.143, 0.006, 10, 96), M.gold()); r.rotation.x = Math.PI / 2; r.position.y = base.position.y + y; g.add(r); }
  const plaque = new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.03, 0.004), M.gold()); plaque.position.set(0, base.position.y + 0.05, 0.14); plaque.rotation.x = -0.25; g.add(plaque);
  // inner diorama
  const scene = new THREE.Group(); scene.position.y = base.position.y + 0.1; g.add(scene);
  const ground = new THREE.Mesh(new THREE.CylinderGeometry(0.13, 0.13, 0.02, 48), M.snow()); scene.add(ground);
  const tree = christmasTree({ height: 0.16, seed: 9 }); tree.position.set(-0.035, 0.01, -0.02); scene.add(tree);
  const tree2 = christmasTree({ height: 0.09, seed: 4, lights: false }); tree2.position.set(0.075, 0.01, -0.04); scene.add(tree2);
  const house = new THREE.Group(); house.position.set(0.045, 0.01, 0.045); house.rotation.y = -0.5; scene.add(house);
  const walls = new THREE.Mesh(new THREE.BoxGeometry(0.05, 0.035, 0.04), M.matte('#7a3a24')); walls.position.y = 0.0175; house.add(walls);
  const roof = new THREE.Mesh(new THREE.CylinderGeometry(0.0, 0.04, 0.03, 4, 1), M.snow()); roof.position.y = 0.05; roof.rotation.y = Math.PI / 4; roof.scale.set(1.0, 1, 0.8); house.add(roof);
  const win = new THREE.Mesh(new THREE.PlaneGeometry(0.012, 0.012), M.glow('#ffb347', 5)); win.position.set(0, 0.018, 0.0205); house.add(win);
  const lamp = new THREE.PointLight('#ffb347', 0.25, 0.4, 2); lamp.position.set(0.045, 0.05, 0.07); scene.add(lamp);
  // swirling snow inside the globe (constrained to the sphere)
  const centre = new THREE.Vector3(0, glass.position.y, 0);
  const snow = c.track(new ParticleSystem({
    max: 400, map: flakeSprite(), rate: 120, life: [2.5, 4], size: [0.006, 0.014], color: ['#ffffff'], intensity: 2.2, spin: [-2, 2],
    spawn: (P, V) => { const a = Math.random() * TAU, r = Math.random() * R * 0.8; P.set(Math.cos(a) * r, centre.y + (Math.random() - 0.3) * R, Math.sin(a) * r); V.set(-Math.sin(a) * 0.08, 0.05, Math.cos(a) * 0.08); },
    force: (k, P, V, dt) => {
      V[k + 1] -= 0.06 * dt;
      const x = P[k], y = P[k + 1] - centre.y, z = P[k + 2]; const d = Math.hypot(x, y, z);
      if (d > R * 0.88) { V[k] -= x / d * 0.6 * dt * 10; V[k + 1] -= y / d * 0.6 * dt * 10; V[k + 2] -= z / d * 0.6 * dt * 10; }
      if (P[k + 1] < scene.position.y + 0.015) { V[k + 1] = Math.abs(V[k + 1]) * 0.1; P[k + 1] = scene.position.y + 0.015; }
    },
    prewarm: 3,
  }));
  g.add(snow);
  c.on((t, dt) => { g.position.y = Math.sin(t * 1.3) * 0.03; g.rotation.y = Math.sin(t * 0.4) * 0.6; snow.tick(dt); tree.userData.lights?.userData.update(t); });
  return c;
}

/* ------------------------------------------------------------- Day 13: Golden Bell */
export function goldenBells() {
  const c = cosmetic('float');
  const g = new THREE.Group(); g.position.y = 0.15; c.root.add(g);
  const gold = M.gold({ roughness: 0.12 });
  const bow = bowMesh(0.12, M.satin('#c8102e')); g.add(bow);
  const holly = hollySprig(0.12); holly.position.set(0, -0.02, -0.02); g.add(holly);
  const bells = [];
  [[-0.07, 0.2, 0.09, 0], [0.07, 0.26, 0.1, 1.3], [0, 0.32, 0.12, 2.1]].forEach(([x, len, size, ph]) => {
    const pivot = new THREE.Group(); pivot.position.set(x, -0.01, 0); g.add(pivot);
    const rib = new THREE.Mesh(new THREE.BoxGeometry(0.012, len, 0.003), M.satin('#c8102e')); rib.position.y = -len / 2; pivot.add(rib);
    const b = bell(gold); b.scale.setScalar(size); b.position.y = -len - size * 1.0; b.castShadow = true; pivot.add(b);
    bells.push({ pivot, b, ph });
  });
  const shine = c.track(new ParticleSystem({ max: 250, map: sparkleSprite(), rate: 40, life: [0.5, 1.2], size: [0.02, 0.06], color: ['#ffd36b', '#ffffff'], intensity: 2.8,
    spawn: (P) => { const b = bells[(Math.random() * 3) | 0]; b.b.getWorldPosition(P); g.worldToLocal(P); P.x += (Math.random() - 0.5) * 0.12; P.y += Math.random() * 0.1; P.z += (Math.random() - 0.5) * 0.12; }, prewarm: 0 }));
  g.add(shine);
  c.on((t, dt) => {
    g.position.y = 0.15 + Math.sin(t * 1.2) * 0.025;
    g.rotation.y = Math.sin(t * 0.5) * 0.5;
    bells.forEach(({ pivot, b, ph }) => { pivot.rotation.z = Math.sin(t * 2.4 + ph) * 0.28; pivot.rotation.x = Math.cos(t * 1.9 + ph) * 0.08; b.userData.clapper.position.x = -Math.sin(t * 2.4 + ph - 0.5) * 0.25; });
    shine.tick(dt);
  });
  return c;
}

/* ------------------------------------------------------------- Day 17: Christmas Lantern */
export function christmasLantern() {
  const c = cosmetic('float');
  const g = new THREE.Group(); c.root.add(g);
  const metal = M.darkMetal({ color: new THREE.Color('#151a16'), metalness: 0.85, roughness: 0.35 });
  const gold = M.gold();
  const W = 0.12, H = 0.2;
  const base = new THREE.Mesh(new THREE.CylinderGeometry(W * 0.85, W * 0.95, 0.025, 4, 1), metal); base.rotation.y = Math.PI / 4; base.position.y = 0; g.add(base);
  const top = base.clone(); top.position.y = H; g.add(top);
  for (let i = 0; i < 4; i++) {
    const a = (i / 4) * TAU + Math.PI / 4;
    const post = new THREE.Mesh(new THREE.CylinderGeometry(0.006, 0.006, H, 8), gold); post.position.set(Math.cos(a) * W * 0.58, H / 2, Math.sin(a) * W * 0.58); g.add(post);
    const pane = new THREE.Mesh(new THREE.PlaneGeometry(W * 0.78, H * 0.92), M.glass({ color: new THREE.Color('#fff3d6'), roughness: 0.08, side: THREE.DoubleSide, transmission: 0.9 }));
    const pa = (i / 4) * TAU; pane.position.set(Math.cos(pa) * W * 0.41, H / 2, Math.sin(pa) * W * 0.41); pane.rotation.y = -pa + Math.PI / 2; g.add(pane);
    // window mullion cross
    const bar = new THREE.Mesh(new THREE.BoxGeometry(W * 0.78, 0.004, 0.004), metal); bar.position.copy(pane.position).setY(H * 0.55); bar.rotation.y = pane.rotation.y; g.add(bar);
  }
  const roof = new THREE.Mesh(new THREE.ConeGeometry(W * 0.95, 0.09, 4, 1), metal); roof.rotation.y = Math.PI / 4; roof.position.y = H + 0.055; g.add(roof);
  const snowCap = new THREE.Mesh(new THREE.ConeGeometry(W * 0.82, 0.05, 4, 1), M.snow()); snowCap.rotation.y = Math.PI / 4; snowCap.position.y = H + 0.08; g.add(snowCap);
  const ring = new THREE.Mesh(new THREE.TorusGeometry(0.025, 0.005, 10, 32), gold); ring.position.y = H + 0.13; g.add(ring);
  const holly = hollySprig(0.1); holly.position.set(0, H + 0.04, W * 0.4); holly.rotation.x = -0.6; g.add(holly);
  const bow = bowMesh(0.06, M.satin('#c8102e')); bow.position.set(0, H + 0.02, W * 0.48); g.add(bow);
  // candle + animated flame
  const candle = new THREE.Mesh(new THREE.CylinderGeometry(0.02, 0.022, 0.08, 24), new THREE.MeshPhysicalMaterial({ color: '#f6ecd6', roughness: 0.5, transmission: 0.2, thickness: 0.05, sheen: 0.4 }));
  candle.position.y = 0.055; g.add(candle);
  const flameMat = new THREE.ShaderMaterial({
    uniforms: { uT: { value: 0 } }, transparent: true, depthWrite: false, blending: THREE.AdditiveBlending,
    vertexShader: `uniform float uT; varying vec2 vUv; void main(){ vUv=uv; vec3 p=position; p.x += sin(uT*9.0+p.y*40.0)*0.003*uv.y; gl_Position=projectionMatrix*modelViewMatrix*vec4(p,1.0);} `,
    fragmentShader: `varying vec2 vUv; uniform float uT; void main(){ float core = smoothstep(0.0,0.6,1.0-vUv.y); vec3 c = mix(vec3(1.0,0.45,0.08), vec3(1.0,0.95,0.7), core); gl_FragColor=vec4(c*3.0, 0.9*(1.0-vUv.y*0.6)); }`,
  });
  const flame = new THREE.Mesh(new THREE.SphereGeometry(0.012, 24, 16), flameMat); flame.scale.set(1, 2.4, 1); flame.position.y = 0.112; g.add(flame);
  const halo = new THREE.Sprite(new THREE.SpriteMaterial({ map: glowSprite(), color: '#ffb347', blending: THREE.AdditiveBlending, depthWrite: false, opacity: 0.8 }));
  halo.scale.setScalar(0.3); halo.position.y = 0.11; g.add(halo);
  const light = new THREE.PointLight('#ffae4a', 1.4, 2.2, 2); light.position.y = 0.12; light.castShadow = false; g.add(light);
  const embers = c.track(new ParticleSystem({ max: 120, map: glowSprite(), rate: 10, life: [1, 2], size: [0.008, 0.015], color: ['#ffb347', '#ffd36b'], gravity: [0, 0.1, 0], intensity: 3,
    spawn: (P, V) => { P.set(0, H + 0.14, 0); V.set((Math.random() - 0.5) * 0.04, 0.05, (Math.random() - 0.5) * 0.04); }, prewarm: 2 }));
  g.add(embers);
  g.traverse((o) => { if (o.isMesh) o.castShadow = o.material !== flameMat; });
  g.position.y = -0.1;
  c.on((t, dt) => {
    flameMat.uniforms.uT.value = t;
    const fl = 0.85 + Math.sin(t * 17) * 0.07 + Math.sin(t * 31) * 0.05 + Math.random() * 0.05;
    light.intensity = 1.4 * fl; halo.material.opacity = 0.7 * fl; flame.scale.y = 2.4 * (0.9 + fl * 0.15);
    g.rotation.z = Math.sin(t * 1.1) * 0.06; g.rotation.y = t * 0.2; g.position.y = -0.1 + Math.sin(t * 1.3) * 0.025;
    embers.tick(dt);
  });
  return c;
}

