/**
 * Timezone-aware clock for the event.
 * - Converts an absolute instant into the calendar date of the configured zone
 *   (local zone or a fixed IANA zone) using Intl, so DST and offsets are handled by the platform.
 * - Optionally aligns with the trusted server time (HTTP Date header of our own origin).
 * - Supports a debug override for QA ("time travel").
 */
export class EventClock {
  constructor({ timeZone = 'local' } = {}) {
    this.timeZone = timeZone === 'local'
      ? Intl.DateTimeFormat().resolvedOptions().timeZone
      : timeZone;
    this.offsetMs = 0;        // server - client
    this.override = null;     // fixed epoch ms for debugging
    this._fmt = new Intl.DateTimeFormat('en-CA', {
      timeZone: this.timeZone,
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit', second: '2-digit',
      hourCycle: 'h23',
    });
  }

  now() {
    return this.override ?? (Date.now() + this.offsetMs);
  }

  /** Wall-clock parts of an instant in the event zone. */
  partsAt(epochMs) {
    const out = {};
    for (const p of this._fmt.formatToParts(new Date(epochMs))) {
      if (p.type !== 'literal') out[p.type] = Number(p.value);
    }
    return out; // { year, month, day, hour, minute, second }
  }

  today() {
    return this.partsAt(this.now());
  }

  /**
   * Absolute instant at which the wall clock in the event zone reads Y-M-D 00:00:00.
   * Iterative offset correction – robust across DST transitions.
   */
  zonedMidnight(year, month, day) {
    const target = Date.UTC(year, month - 1, day, 0, 0, 0);
    let guess = target;
    for (let i = 0; i < 4; i++) {
      const p = this.partsAt(guess);
      const asUtc = Date.UTC(p.year, p.month - 1, p.day, p.hour, p.minute, p.second);
      const diff = asUtc - target;
      if (diff === 0) break;
      guess -= diff;
    }
    return guess;
  }

  /** Fetch the server time from our own origin (no third party). Silently keeps the local clock on failure. */
  async syncWithServer(url = typeof location !== 'undefined' ? location.href : null) {
    if (!url || typeof fetch === 'undefined') return false;
    try {
      const t0 = Date.now();
      const res = await fetch(url, { method: 'HEAD', cache: 'no-store' });
      const t1 = Date.now();
      const header = res.headers.get('date');
      if (!header) return false;
      const server = Date.parse(header);
      if (Number.isNaN(server)) return false;
      const offset = server + (t1 - t0) / 2 - t1;
      // HTTP Date has 1s resolution: ignore tiny offsets, they are just rounding.
      this.offsetMs = Math.abs(offset) > 1500 ? offset : 0;
      return true;
    } catch {
      return false;
    }
  }
}
