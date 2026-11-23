/**
 * Event configuration.
 *
 * timeZone:
 *   'local'          → doors unlock at local midnight of the player (default, like most live events)
 *   IANA name        → doors unlock simultaneously worldwide at midnight of that zone, e.g. 'Europe/Berlin'
 */
export const EVENT_CONFIG = Object.freeze({
  id: 'xmas-advent',
  month: 12,          // December
  firstDay: 1,
  lastDay: 24,
  timeZone: 'local',
  // Unclaimed past doors can be caught up until the event ends (after Dec 24 → "missed").
  allowCatchUp: true,
  // Try to read the trusted server time from the HTTP "Date" header of our own origin.
  useServerTime: true,
});
