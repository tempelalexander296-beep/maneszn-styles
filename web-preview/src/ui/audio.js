/** Tiny WebAudio synth for bell chimes, whooshes and the finale fanfare (no audio files needed). */
export class Sfx {
  constructor() { this.enabled = true; this.ctx = null; }
  ensure() {
    if (!this.enabled) return null;
    if (!this.ctx) { try { this.ctx = new (window.AudioContext || window.webkitAudioContext)(); } catch { return null; } }
    if (this.ctx.state === 'suspended') this.ctx.resume();
    return this.ctx;
  }
  bell(freq = 880, when = 0, gain = 0.18, decay = 2.2) {
    const ctx = this.ensure(); if (!ctx) return;
    const t = ctx.currentTime + when;
    const out = ctx.createGain(); out.gain.value = gain; out.connect(ctx.destination);
    for (const [mult, amp] of [[1, 1], [2.0, 0.5], [2.76, 0.35], [5.4, 0.2], [8.9, 0.08]]) {
      const o = ctx.createOscillator(); o.type = 'sine'; o.frequency.value = freq * mult;
      const g = ctx.createGain(); g.gain.setValueAtTime(0, t); g.gain.linearRampToValueAtTime(amp, t + 0.005); g.gain.exponentialRampToValueAtTime(0.0001, t + decay / mult ** 0.3);
      o.connect(g).connect(out); o.start(t); o.stop(t + decay + 0.1);
    }
  }
  whoosh(dur = 0.6, when = 0) {
    const ctx = this.ensure(); if (!ctx) return;
    const t = ctx.currentTime + when;
    const buf = ctx.createBuffer(1, ctx.sampleRate * dur, ctx.sampleRate);
    const d = buf.getChannelData(0); for (let i = 0; i < d.length; i++) d[i] = (Math.random() * 2 - 1) * (1 - i / d.length);
    const src = ctx.createBufferSource(); src.buffer = buf;
    const f = ctx.createBiquadFilter(); f.type = 'bandpass'; f.Q.value = 1.2; f.frequency.setValueAtTime(400, t); f.frequency.exponentialRampToValueAtTime(3000, t + dur);
    const g = ctx.createGain(); g.gain.value = 0.12;
    src.connect(f).connect(g).connect(ctx.destination); src.start(t);
  }
  sparkle(when = 0) { [1568, 2093, 2637, 3136].forEach((f, i) => this.bell(f, when + i * 0.06, 0.05, 0.8)); }
  open(rank) {
    this.whoosh(0.5);
    const base = [523.25, 587.33, 659.25, 698.46, 783.99, 880][rank] ?? 523.25;
    [1, 1.25, 1.5, 2].forEach((m, i) => this.bell(base * m, 0.35 + i * 0.12, 0.12, 2));
    this.sparkle(0.9);
  }
  finale() {
    this.whoosh(1.4);
    const notes = [392, 493.88, 587.33, 783.99, 987.77, 1174.66, 1567.98];
    notes.forEach((f, i) => this.bell(f, 0.6 + i * 0.16, 0.13, 3));
    [783.99, 987.77, 1174.66].forEach((f) => this.bell(f, 2.0, 0.12, 4));
    this.sparkle(2.2); this.sparkle(2.8);
  }
  tick() { this.bell(1760, 0, 0.03, 0.25); }
}
