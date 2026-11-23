package dev.maneszn.advent.core;

import java.util.EnumMap;
import java.util.Map;

/**
 * Per-player persistent state: claimed doors (bitmask, bit d = day d) and equipped cosmetic per slot.
 * Kept free of Minecraft types so it is shared by every MC version module.
 */
public final class PlayerAdvent {
	private int claimedMask;
	private final EnumMap<CosmeticSlot, String> equipped = new EnumMap<>(CosmeticSlot.class);

	public int claimedMask() { return claimedMask; }
	public void setClaimedMask(int mask) { this.claimedMask = mask & 0x1FFFFFE; }
	public boolean isClaimed(int day) { return (claimedMask & (1 << day)) != 0; }
	public int claimedCount() { return Integer.bitCount(claimedMask); }

	public Map<CosmeticSlot, String> equipped() { return equipped; }

	public DoorState doorState(int day, int currentDay) {
		if (isClaimed(day)) return DoorState.CLAIMED;
		if (currentDay > AdventClock.LAST_DAY) return DoorState.MISSED;
		if (day > currentDay) return DoorState.LOCKED;
		return DoorState.AVAILABLE; // today, or catch-up of an earlier day while the event runs
	}

	public enum ClaimResult { OK, INVALID_DAY, ALREADY_CLAIMED, LOCKED, MISSED }

	/** Server-side, idempotent: a door can never be claimed twice. Auto-equips the new cosmetic. */
	public ClaimResult claim(int day, int currentDay) {
		Reward r = Rewards.byDay(day);
		if (r == null) return ClaimResult.INVALID_DAY;
		switch (doorState(day, currentDay)) {
			case CLAIMED: return ClaimResult.ALREADY_CLAIMED;
			case LOCKED: return ClaimResult.LOCKED;
			case MISSED: return ClaimResult.MISSED;
			default: break;
		}
		claimedMask |= 1 << day;
		equipped.put(r.slot(), r.id());
		return ClaimResult.OK;
	}

	/** Equip (id) or unequip (null) — only owned cosmetics. */
	public boolean equip(CosmeticSlot slot, String id) {
		if (id == null || id.isEmpty()) { equipped.remove(slot); return true; }
		Reward r = Rewards.byId(id);
		if (r == null || r.slot() != slot || !isClaimed(r.day())) return false;
		equipped.put(slot, id);
		return true;
	}

	/** Compact wire/disk form: "slot=id;slot=id". */
	public String equippedString() {
		StringBuilder sb = new StringBuilder();
		equipped.forEach((s, id) -> { if (!sb.isEmpty()) sb.append(';'); sb.append(s.name()).append('=').append(id); });
		return sb.toString();
	}

	public static EnumMap<CosmeticSlot, String> parseEquipped(String s) {
		EnumMap<CosmeticSlot, String> out = new EnumMap<>(CosmeticSlot.class);
		if (s == null || s.isEmpty()) return out;
		for (String part : s.split(";")) {
			int i = part.indexOf('=');
			if (i <= 0) continue;
			try {
				CosmeticSlot slot = CosmeticSlot.valueOf(part.substring(0, i));
				Reward r = Rewards.byId(part.substring(i + 1));
				if (r != null && r.slot() == slot) out.put(slot, r.id());
			} catch (IllegalArgumentException ignored) { }
		}
		return out;
	}

	public void loadEquipped(String s) { equipped.clear(); equipped.putAll(parseEquipped(s)); }
}
