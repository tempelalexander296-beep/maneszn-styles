import * as THREE from 'three';
import { candyStripes, giftPaper, noiseTexture } from './textures.js';

/** PBR material library. Every material is MeshPhysicalMaterial / MeshStandardMaterial (metal-rough workflow). */
const C = (hex) => new THREE.Color(hex);

export const M = {
  gold: (o = {}) => new THREE.MeshPhysicalMaterial({
    color: C('#ffcf6b'), metalness: 1, roughness: 0.22, clearcoat: 0.4, clearcoatRoughness: 0.15,
    bumpMap: noiseTexture(3, 512, 2), bumpScale: 0.15, ...o,
  }),
  roseGold: (o = {}) => new THREE.MeshPhysicalMaterial({ color: C('#f2b48c'), metalness: 1, roughness: 0.28, ...o }),
  silver: (o = {}) => new THREE.MeshPhysicalMaterial({ color: C('#e6edf5'), metalness: 1, roughness: 0.18, ...o }),
  darkMetal: (o = {}) => new THREE.MeshStandardMaterial({ color: C('#1b1d22'), metalness: 0.9, roughness: 0.4, ...o }),

  /** Transmissive ice with iridescence and frost-noise roughness. */
  ice: (o = {}) => new THREE.MeshPhysicalMaterial({
    color: C('#d8f1ff'), metalness: 0, roughness: 0.08, transmission: 0.92, thickness: 0.35, ior: 1.31,
    attenuationColor: C('#7cc4ff'), attenuationDistance: 0.9, iridescence: 0.35, iridescenceIOR: 1.3,
    clearcoat: 1, clearcoatRoughness: 0.05, roughnessMap: noiseTexture(11, 512, 1.5), specularIntensity: 1,
    emissive: C('#2a77c7'), emissiveIntensity: 0.12, ...o,
  }),
  glass: (o = {}) => new THREE.MeshPhysicalMaterial({
    color: C('#ffffff'), metalness: 0, roughness: 0.02, transmission: 1, thickness: 0.05, ior: 1.5,
    clearcoat: 1, specularIntensity: 1, ...o,
  }),

  /** Emissive glow (values > 1 feed the bloom pass). */
  glow: (hex, intensity = 3, o = {}) => new THREE.MeshStandardMaterial({
    color: C('#000000'), emissive: C(hex), emissiveIntensity: intensity, roughness: 1, metalness: 0, toneMapped: true, ...o,
  }),
  gem: (hex, o = {}) => new THREE.MeshPhysicalMaterial({
    color: C(hex), metalness: 0, roughness: 0.05, transmission: 0.6, thickness: 0.2, ior: 2.1,
    emissive: C(hex), emissiveIntensity: 0.9, clearcoat: 1, iridescence: 0.4, ...o,
  }),

  candy: (stripe = '#c8102e', thin = '#2f8f46', repeatU = 4, o = {}) => {
    const map = candyStripes({ stripe, thin }).clone();
    map.needsUpdate = true; map.repeat.set(repeatU, 1);
    return new THREE.MeshPhysicalMaterial({ map, roughness: 0.18, clearcoat: 1, clearcoatRoughness: 0.06, sheen: 0.2, ...o });
  },

  paper: (palette = 'red', motif = 'stars', repeat = 1, o = {}) => {
    const map = giftPaper(palette, motif).clone();
    map.needsUpdate = true; map.repeat.set(repeat, repeat);
    return new THREE.MeshPhysicalMaterial({ map, roughness: 0.45, clearcoat: 0.6, clearcoatRoughness: 0.25, ...o });
  },
  satin: (hex, o = {}) => new THREE.MeshPhysicalMaterial({
    color: C(hex), roughness: 0.32, sheen: 1, sheenColor: C(hex).lerp(C('#ffffff'), 0.5), sheenRoughness: 0.35, clearcoat: 0.3, ...o,
  }),
  velvet: (hex, o = {}) => new THREE.MeshPhysicalMaterial({
    color: C(hex), roughness: 0.85, sheen: 1, sheenColor: C(hex).lerp(C('#ff8a8a'), 0.4), sheenRoughness: 0.5,
    bumpMap: noiseTexture(5, 512, 4), bumpScale: 0.6, ...o,
  }),
  fur: (o = {}) => new THREE.MeshPhysicalMaterial({
    color: C('#f7f5f1'), roughness: 1, sheen: 1, sheenColor: C('#ffffff'), sheenRoughness: 0.8,
    bumpMap: noiseTexture(9, 512, 8), bumpScale: 2.5, ...o,
  }),
  snow: (o = {}) => new THREE.MeshPhysicalMaterial({
    color: C('#f4f8ff'), roughness: 0.7, sheen: 0.6, sheenColor: C('#cfe6ff'), bumpMap: noiseTexture(13, 512, 3), bumpScale: 1.4,
    emissive: C('#9cc8ff'), emissiveIntensity: 0.03, ...o,
  }),
  matte: (hex, o = {}) => new THREE.MeshStandardMaterial({ color: C(hex), roughness: 0.65, metalness: 0, ...o }),
  plastic: (hex, o = {}) => new THREE.MeshPhysicalMaterial({ color: C(hex), roughness: 0.25, clearcoat: 0.8, ...o }),
  ornament: (hex, o = {}) => new THREE.MeshPhysicalMaterial({
    color: C(hex), metalness: 0.85, roughness: 0.12, clearcoat: 1, clearcoatRoughness: 0.03, iridescence: 0.15, ...o,
  }),
  leaf: (o = {}) => new THREE.MeshPhysicalMaterial({ color: C('#0f5a2a'), roughness: 0.3, clearcoat: 0.9, side: THREE.DoubleSide, ...o }),
  berry: (o = {}) => new THREE.MeshPhysicalMaterial({ color: C('#c40f22'), roughness: 0.15, clearcoat: 1, emissive: C('#5a0008'), emissiveIntensity: 0.4, ...o }),
};
