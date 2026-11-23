package dev.maneszn.advent.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Server-authoritative event clock. Doors unlock at 00:00 in the configured zone
 * (default: the server's zone). java.time handles DST/offsets correctly.
 */
public final class AdventClock {
	public static final int MONTH = 12, FIRST_DAY = 1, LAST_DAY = 24;

	private ZoneId zone;
	private Long overrideEpochMs; // debug/QA time travel (ops only)

	public AdventClock(ZoneId zone) { this.zone = zone; }

	public ZoneId zone() { return zone; }
	public void setZone(ZoneId zone) { this.zone = zone; }
	public void setOverride(Long epochMs) { this.overrideEpochMs = epochMs; }
	public boolean hasOverride() { return overrideEpochMs != null; }

	public long nowMs() { return overrideEpochMs != null ? overrideEpochMs : System.currentTimeMillis(); }
	public ZonedDateTime now() { return Instant.ofEpochMilli(nowMs()).atZone(zone); }

	/** 0 = before Dec 1, 1..24 = active day, 25 = event over. */
	public int currentDay() {
		LocalDate d = now().toLocalDate();
		if (d.getMonthValue() < MONTH) return 0;
		if (d.getDayOfMonth() > LAST_DAY) return LAST_DAY + 1;
		return Math.max(0, d.getDayOfMonth() < FIRST_DAY ? 0 : d.getDayOfMonth());
	}

	public int year() { return now().getYear(); }

	/** Epoch ms of local midnight for the given December day of the current year. */
	public long midnightOf(int decemberDay) {
		return LocalDate.of(year(), MONTH, Math.min(decemberDay, 31)).atStartOfDay(zone).toInstant().toEpochMilli();
	}

	/** Epoch ms when the next door unlocks, or -1 if none left. */
	public long nextUnlockMs() {
		int cur = currentDay();
		if (cur >= LAST_DAY) return -1;
		return midnightOf(Math.max(cur + 1, FIRST_DAY));
	}

	public long christmasEveMs() { return midnightOf(LAST_DAY); }

	/** Sets the debug clock to noon of a December day (0 = Nov 30). */
	public void travelTo(int decemberDay) {
		LocalDate d = decemberDay <= 0 ? LocalDate.of(year(), 11, 30) : LocalDate.of(year(), MONTH, decemberDay);
		overrideEpochMs = d.atTime(12, 0).atZone(zone).toInstant().toEpochMilli();
	}
}
