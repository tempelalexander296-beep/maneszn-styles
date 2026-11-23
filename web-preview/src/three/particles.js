import * as THREE from 'three';
import { glowSprite } from './textures.js';

/**
 * Efficient CPU-simulated, GPU-rendered particle system (single draw call, typed arrays,
 * ring-buffer recycling, soft sprites with rotation, size & alpha over life).
 */
const vert = /* glsl */`
  attribute float aSize; attribute float aAlpha; attribute float aRot; attribute vec3 aColor;
  uniform float uScale;
  varying float vAlpha; varying float vRot; varying vec3 vColor;
  void main(){
    vec4 mv = modelViewMatrix * vec4(position,1.0);
    gl_Position = projectionMatrix * mv;
    gl_PointSize = aSize * uScale / max(0.001, -mv.z);
    vAlpha = aAlpha; vRot = aRot; vColor = aColor;
  }`;
const frag = /* glsl */`
  uniform sampler2D uMap; uniform float uIntensity;
  varying float vAlpha; varying float vRot; varying vec3 vColor;
  void main(){
    vec2 uv = gl_PointCoord - 0.5;
    float c = cos(vRot), s = sin(vRot);
    uv = mat2(c,-s,s,c) * uv + 0.5;
    vec4 t = texture2D(uMap, uv);
    float a = t.a * t.r * vAlpha;
    if (a < 0.003) discard;
    gl_FragColor = vec4(vColor * uIntensity * t.rgb, a);
  }`;

export const particleUniformScale = { value: 600 };

export class ParticleSystem extends THREE.Points {
  constructor({
    max = 1000, map = glowSprite(), rate = 100, life = [1, 2], size = [0.05, 0.1],
    color = ['#ffffff'], gravity = [0, 0, 0], drag = 0, sizeEnd = 1, fadeIn = 0.1, fadeOut = 0.4,
    spin = [0, 0], intensity = 1.6, blending = THREE.AdditiveBlending, spawn = null, force = null,
    prewarm = 0, emitting = true,
  } = {}) {
    const geo = new THREE.BufferGeometry();
    const pos = new Float32Array(max * 3);
    geo.setAttribute('position', new THREE.BufferAttribute(pos, 3).setUsage(THREE.DynamicDrawUsage));
    geo.setAttribute('aSize', new THREE.BufferAttribute(new Float32Array(max), 1).setUsage(THREE.DynamicDrawUsage));
    geo.setAttribute('aAlpha', new THREE.BufferAttribute(new Float32Array(max), 1).setUsage(THREE.DynamicDrawUsage));
    geo.setAttribute('aRot', new THREE.BufferAttribute(new Float32Array(max), 1).setUsage(THREE.DynamicDrawUsage));
    geo.setAttribute('aColor', new THREE.BufferAttribute(new Float32Array(max * 3), 3).setUsage(THREE.DynamicDrawUsage));
    geo.boundingSphere = new THREE.Sphere(new THREE.Vector3(), 50);
    const mat = new THREE.ShaderMaterial({
      uniforms: { uMap: { value: map }, uScale: particleUniformScale, uIntensity: { value: intensity } },
      vertexShader: vert, fragmentShader: frag, transparent: true, depthWrite: false, blending,
    });
    super(geo, mat);
    this.frustumCulled = false;
    Object.assign(this, { max, rate, life, size, gravity, drag, sizeEnd, fadeIn, fadeOut, spin, spawnFn: spawn, forceFn: force, emitting });
    this.palette = color.map((c) => new THREE.Color(c));
    this.vel = new Float32Array(max * 3);
    this.age = new Float32Array(max).fill(1e9);
    this.lifeA = new Float32Array(max).fill(1);
    this.baseSize = new Float32Array(max);
    this.spinA = new Float32Array(max);
    this.cursor = 0; this.acc = 0; this.time = 0;
    this._p = new THREE.Vector3(); this._v = new THREE.Vector3(); this._c = new THREE.Color();
    if (prewarm > 0) for (let t = 0; t < prewarm; t += 1 / 30) this.tick(1 / 30);
  }

  /** Emit n particles immediately (bursts). */
  burst(n, overrides) {
    for (let i = 0; i < n; i++) this.spawnOne(overrides);
  }

  spawnOne(overrides) {
    const i = this.cursor; this.cursor = (this.cursor + 1) % this.max;
    const P = this._p.set(0, 0, 0), V = this._v.set(0, 0, 0), C = this._c.copy(this.palette[(Math.random() * this.palette.length) | 0]);
    const o = { size: this.size[0] + Math.random() * (this.size[1] - this.size[0]), life: this.life[0] + Math.random() * (this.life[1] - this.life[0]) };
    (overrides?.spawn ?? this.spawnFn)?.(P, V, C, o, this.time);
    const pos = this.geometry.attributes.position.array, col = this.geometry.attributes.aColor.array;
    pos[i * 3] = P.x; pos[i * 3 + 1] = P.y; pos[i * 3 + 2] = P.z;
    this.vel[i * 3] = V.x; this.vel[i * 3 + 1] = V.y; this.vel[i * 3 + 2] = V.z;
    col[i * 3] = C.r; col[i * 3 + 1] = C.g; col[i * 3 + 2] = C.b;
    this.age[i] = 0; this.lifeA[i] = o.life; this.baseSize[i] = o.size;
    this.geometry.attributes.aRot.array[i] = Math.random() * Math.PI * 2;
    this.spinA[i] = this.spin[0] + Math.random() * (this.spin[1] - this.spin[0]);
  }

  tick(dt) {
    dt = Math.min(dt, 0.05);
    this.time += dt;
    if (this.emitting) {
      this.acc += this.rate * dt;
      while (this.acc >= 1) { this.spawnOne(); this.acc -= 1; }
    }
    const a = this.geometry.attributes;
    const pos = a.position.array, sz = a.aSize.array, al = a.aAlpha.array, rot = a.aRot.array;
    const [gx, gy, gz] = this.gravity, dragK = Math.max(0, 1 - this.drag * dt);
    for (let i = 0; i < this.max; i++) {
      const age = (this.age[i] += dt), L = this.lifeA[i];
      if (age >= L) { al[i] = 0; sz[i] = 0; continue; }
      const k = i * 3;
      this.vel[k] = (this.vel[k] + gx * dt) * dragK;
      this.vel[k + 1] = (this.vel[k + 1] + gy * dt) * dragK;
      this.vel[k + 2] = (this.vel[k + 2] + gz * dt) * dragK;
      if (this.forceFn) this.forceFn(k, pos, this.vel, dt, this.time, age / L);
      pos[k] += this.vel[k] * dt; pos[k + 1] += this.vel[k + 1] * dt; pos[k + 2] += this.vel[k + 2] * dt;
      const t = age / L;
      const fin = this.fadeIn > 0 ? Math.min(1, t / this.fadeIn) : 1;
      const fout = this.fadeOut > 0 ? Math.min(1, (1 - t) / this.fadeOut) : 1;
      al[i] = fin * fout;
      sz[i] = this.baseSize[i] * (1 + (this.sizeEnd - 1) * t);
      rot[i] += this.spinA[i] * dt;
    }
    a.position.needsUpdate = a.aSize.needsUpdate = a.aAlpha.needsUpdate = a.aRot.needsUpdate = a.aColor.needsUpdate = true;
  }

  dispose() { this.geometry.dispose(); this.material.dispose(); }
}

/**
 * Camera-independent ribbon trail: a strip that follows a moving anchor with fading alpha and
 * colour gradient. Rendered additively for an energy look.
 */
export class RibbonTrail extends THREE.Mesh {
  constructor({ length = 80, width = 0.12, colorA = '#ff3355', colorB = '#ffd36b', intensity = 2.2, twist = 0 } = {}) {
    const geo = new THREE.BufferGeometry();
    const pos = new Float32Array(length * 2 * 3), uv = new Float32Array(length * 2 * 2), idx = [];
    for (let i = 0; i < length; i++) {
      uv.set([i / (length - 1), 0, i / (length - 1), 1], i * 4);
      if (i < length - 1) { const a = i * 2; idx.push(a, a + 1, a + 2, a + 1, a + 3, a + 2); }
    }
    geo.setAttribute('position', new THREE.BufferAttribute(pos, 3).setUsage(THREE.DynamicDrawUsage));
    geo.setAttribute('uv', new THREE.BufferAttribute(uv, 2));
    geo.setIndex(idx);
    const mat = new THREE.ShaderMaterial({
      uniforms: { cA: { value: new THREE.Color(colorA) }, cB: { value: new THREE.Color(colorB) }, uI: { value: intensity }, uT: { value: 0 } },
      vertexShader: `varying vec2 vUv; void main(){ vUv=uv; gl_Position=projectionMatrix*modelViewMatrix*vec4(position,1.0);} `,
      fragmentShader: `uniform vec3 cA; uniform vec3 cB; uniform float uI; uniform float uT; varying vec2 vUv;
        void main(){ float edge = 1.0 - pow(abs(vUv.y-0.5)*2.0, 2.0);
          float head = pow(1.0 - vUv.x, 1.6);
          float shimmer = 0.75 + 0.25*sin(vUv.x*60.0 - uT*12.0);
          vec3 c = mix(cA, cB, vUv.x);
          gl_FragColor = vec4(c*uI*shimmer, edge*head); }`,
      transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, side: THREE.DoubleSide,
    });
    super(geo, mat);
    this.frustumCulled = false;
    this.points = Array.from({ length }, () => new THREE.Vector3());
    this.width = width; this.twist = twist; this.inited = false; this.len = length;
    this._side = new THREE.Vector3(); this._up = new THREE.Vector3(0, 1, 0);
  }

  push(p, t = 0) {
    if (!this.inited) { this.points.forEach((q) => q.copy(p)); this.inited = true; }
    const last = this.points.pop(); last.copy(p); this.points.unshift(last);
    const pos = this.geometry.attributes.position.array;
    for (let i = 0; i < this.len; i++) {
      const a = this.points[i], b = this.points[Math.min(i + 1, this.len - 1)];
      const dir = this._side.subVectors(a, b);
      if (dir.lengthSq() < 1e-8) dir.set(1, 0, 0);
      const tw = this.twist * (i / this.len) * 6 + t * this.twist;
      this._up.set(Math.sin(tw), Math.cos(tw), 0);
      dir.cross(this._up).normalize().multiplyScalar(this.width * (1 - i / this.len * 0.6));
      pos[i * 6] = a.x + dir.x; pos[i * 6 + 1] = a.y + dir.y; pos[i * 6 + 2] = a.z + dir.z;
      pos[i * 6 + 3] = a.x - dir.x; pos[i * 6 + 4] = a.y - dir.y; pos[i * 6 + 5] = a.z - dir.z;
    }
    this.geometry.attributes.position.needsUpdate = true;
    this.material.uniforms.uT.value = t;
  }
}
