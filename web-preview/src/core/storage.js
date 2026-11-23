/**
 * Persistent claim storage.
 * The calendar talks to a "ClaimStore" interface (load/save). The default implementation
 * persists into localStorage with a version and integrity hash; a server-backed store
 * with the same interface can be dropped in for authoritative claims.
 */
const SALT = 'xmas-advent::v1::frost';

export function fnv1a(str) {
  let h = 0x811c9dc5;
  for (let i = 0; i < str.length; i++) {
    h ^= str.charCodeAt(i);
    h = Math.imul(h, 0x01000193) >>> 0;
  }
  return h.toString(16).padStart(8, '0');
}

const signPayload = (payload) => fnv1a(SALT + JSON.stringify(payload));

export class LocalClaimStore {
  constructor(key, backend = (typeof localStorage !== 'undefined' ? localStorage : null)) {
    this.key = key;
    this.backend = backend;
    this.memory = null; // fallback when storage is blocked
  }

  load() {
    let raw = null;
    try { raw = this.backend?.getItem(this.key) ?? null; } catch { raw = null; }
    if (raw == null) raw = this.memory;
    if (!raw) return { claims: {} };
    try {
      const parsed = JSON.parse(raw);
      if (parsed?.v !== 1 || typeof parsed.claims !== 'object') return { claims: {} };
      if (parsed.sig !== signPayload(parsed.claims)) {
        console.warn('[advent] storage integrity check failed – progress reset');
        return { claims: {}, tampered: true };
      }
      return { claims: parsed.claims };
    } catch {
      return { claims: {} };
    }
  }

  save(state) {
    const raw = JSON.stringify({ v: 1, claims: state.claims, sig: signPayload(state.claims) });
    this.memory = raw;
    try { this.backend?.setItem(this.key, raw); } catch { /* quota / privacy mode */ }
  }

  clear() {
    this.memory = null;
    try { this.backend?.removeItem(this.key); } catch { /* ignore */ }
  }
}
