package dev.maneszn.advent.ui;

import dev.maneszn.advent.core.CosmeticSlot;
import dev.maneszn.advent.core.PlayerAdvent;

/** What the calendar UI needs from the game (implemented per MC version). */
public interface Bridge {
	int currentDay();
	long nowMs();
	long nextUnlockMs();
	long christmasEveMs();
	String zone();
	/** Epoch ms when door `day` unlocks (00:00 in the server's event zone). */
	default long unlockMsOf(int day) {
		java.time.ZoneId z = java.time.ZoneId.of(zone());
		int y = java.time.Instant.ofEpochMilli(nowMs()).atZone(z).getYear();
		return java.time.LocalDate.of(y, 12, day).atStartOfDay(z).toInstant().toEpochMilli();
	}
	PlayerAdvent state();
	void claim(int day);
	void equip(CosmeticSlot slot, String id);
	/** Force-show a cosmetic on the local player preview (null clears). */
	void preview(String id, float intro);
	void sound(String kind);
	void close();
}
