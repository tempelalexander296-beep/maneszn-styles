import { REWARDS } from '../data/rewards.js';

export const DoorState = Object.freeze({
  LOCKED: 'locked',       // future day
  AVAILABLE: 'available', // today (or catch-up), not yet claimed
  CLAIMED: 'claimed',
  MISSED: 'missed',       // event is over and the door was never opened
});

/**
 * Pure calendar logic. No DOM – fully unit-testable.
 */
export class AdventCalendar {
  constructor({ clock, store, config }) {
    this.clock = clock;
    this.store = store;
    this.config = config;
    this.state = store.load();
    this.listeners = new Set();
  }

  /** Event year: the December that is current or upcoming in the event zone. */
  get year() {
    const t = this.clock.today();
    return t.year;
  }

  /**
   * Current event day in the event zone:
   *  0     → before Dec 1 (event not started)
   *  1..24 → active day
   *  25    → event over
   */
  currentDay() {
    const t = this.clock.today();
    const { month, firstDay, lastDay } = this.config;
    if (t.month < month) return 0;
    if (t.month > month) return lastDay + 1;
    if (t.day < firstDay) return 0;
    if (t.day > lastDay) return lastDay + 1;
    return t.day;
  }

  isClaimed(day) {
    return Boolean(this.state.claims[day]);
  }

  doorState(day) {
    if (this.isClaimed(day)) return DoorState.CLAIMED;
    const cur = this.currentDay();
    if (cur > this.config.lastDay) return DoorState.MISSED;
    if (day > cur) return DoorState.LOCKED;
    if (day === cur) return DoorState.AVAILABLE;
    return this.config.allowCatchUp ? DoorState.AVAILABLE : DoorState.MISSED;
  }

  /** Idempotent claim – a door can never be claimed twice. */
  claim(day) {
    const reward = REWARDS[day - 1];
    if (!reward) return { ok: false, reason: 'invalid-day' };
    if (this.isClaimed(day)) return { ok: false, reason: 'already-claimed', reward };
    const st = this.doorState(day);
    if (st !== DoorState.AVAILABLE) return { ok: false, reason: st, reward };
    this.state.claims[day] = { id: reward.id, at: new Date(this.clock.now()).toISOString() };
    this.store.save(this.state);
    this.emit();
    return { ok: true, reward };
  }

  claimedRewards() {
    return REWARDS.filter((r) => this.isClaimed(r.day));
  }

  /** Instant at which the next door unlocks (or null). */
  nextUnlockAt() {
    const cur = this.currentDay();
    if (cur >= this.config.lastDay) return null;
    const nextDay = Math.max(cur + 1, this.config.firstDay);
    return this.clock.zonedMidnight(this.year, this.config.month, nextDay);
  }

  /** Christmas Eve 00:00 in the event zone (the finale). */
  christmasEveAt() {
    return this.clock.zonedMidnight(this.year, this.config.month, this.config.lastDay);
  }

  reset() {
    this.state = { claims: {} };
    this.store.clear();
    this.emit();
  }

  onChange(fn) { this.listeners.add(fn); return () => this.listeners.delete(fn); }
  emit() { for (const fn of this.listeners) fn(this); }
}
