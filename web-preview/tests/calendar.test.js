import { test } from 'node:test';
import assert from 'node:assert/strict';
import { EventClock } from '../src/core/clock.js';
import { LocalClaimStore } from '../src/core/storage.js';
import { AdventCalendar, DoorState } from '../src/core/calendar.js';
import { EVENT_CONFIG } from '../src/config.js';

const memoryBackend = () => {
  const m = new Map();
  return { getItem: (k) => m.get(k) ?? null, setItem: (k, v) => m.set(k, v), removeItem: (k) => m.delete(k), _m: m };
};

const make = (iso, tz = 'Europe/Berlin', backend = memoryBackend()) => {
  const clock = new EventClock({ timeZone: tz });
  clock.override = Date.parse(iso);
  const store = new LocalClaimStore('t', backend);
  return { clock, backend, cal: new AdventCalendar({ clock, store, config: EVENT_CONFIG }) };
};

test('before December everything is locked', () => {
  const { cal } = make('2026-11-23T12:00:00Z');
  assert.equal(cal.currentDay(), 0);
  for (let d = 1; d <= 24; d++) assert.equal(cal.doorState(d), DoorState.LOCKED);
  assert.equal(cal.claim(1).ok, false);
});

test('timezone: Dec 1 00:30 Berlin is still Nov 30 in UTC but day 1 in Berlin', () => {
  const iso = '2026-11-30T23:30:00Z';
  assert.equal(make(iso, 'Europe/Berlin').cal.currentDay(), 1);
  assert.equal(make(iso, 'UTC').cal.currentDay(), 0);
  assert.equal(make(iso, 'America/New_York').cal.currentDay(), 0);
});

test('zonedMidnight returns the correct instant', () => {
  const { clock } = make('2026-12-05T10:00:00Z', 'Europe/Berlin');
  assert.equal(new Date(clock.zonedMidnight(2026, 12, 24)).toISOString(), '2026-12-23T23:00:00.000Z');
  const ny = new EventClock({ timeZone: 'America/New_York' });
  assert.equal(new Date(ny.zonedMidnight(2026, 12, 1)).toISOString(), '2026-12-01T05:00:00.000Z');
});

test('claim once, never twice, persists across reloads', () => {
  const { cal, backend, clock } = make('2026-12-05T10:00:00Z');
  assert.equal(cal.doorState(5), DoorState.AVAILABLE);
  assert.equal(cal.doorState(6), DoorState.LOCKED);
  assert.equal(cal.claim(5).ok, true);
  assert.equal(cal.claim(5).reason, 'already-claimed');
  assert.equal(cal.claim(6).ok, false);
  const reloaded = new AdventCalendar({ clock, store: new LocalClaimStore('t', backend), config: EVENT_CONFIG });
  assert.equal(reloaded.doorState(5), DoorState.CLAIMED);
});

test('catch-up during the event, missed after it', () => {
  const { cal, clock } = make('2026-12-10T10:00:00Z');
  assert.equal(cal.doorState(3), DoorState.AVAILABLE);
  cal.claim(3);
  clock.override = Date.parse('2026-12-27T10:00:00Z');
  assert.equal(cal.currentDay(), 25);
  assert.equal(cal.doorState(3), DoorState.CLAIMED);
  assert.equal(cal.doorState(4), DoorState.MISSED);
  assert.equal(cal.claim(4).ok, false);
});

test('tampered storage is rejected', () => {
  const backend = memoryBackend();
  backend.setItem('t', JSON.stringify({ v: 1, claims: { 24: { id: 'x' } }, sig: 'deadbeef' }));
  const { cal } = make('2026-12-02T10:00:00Z', 'Europe/Berlin', backend);
  assert.equal(cal.isClaimed(24), false);
});

test('next unlock is the following local midnight', () => {
  const { cal } = make('2026-12-05T10:00:00Z', 'Europe/Berlin');
  assert.equal(new Date(cal.nextUnlockAt()).toISOString(), '2026-12-05T23:00:00.000Z');
});
