package dev.maneszn.advent.core;

import java.util.List;
import static dev.maneszn.advent.core.CosmeticSlot.*;
import static dev.maneszn.advent.core.Rarity.*;

/** The 24 daily rewards (Dec 1 – Dec 24). Door colours give every door its own wrapping. */
public final class Rewards {
	private static final int RED = 0xFF8E0E1C, RED2 = 0xFFB3162A, GREEN = 0xFF0D4A2C, GREEN2 = 0xFF14663C, GOLD = 0xFFA8781F, GOLD2 = 0xFFE3B553,
		WHITE = 0xFFECE7DE, WHITE2 = 0xFFFFFFFF, NIGHT = 0xFF0C1A33, NIGHT2 = 0xFF1B3160, ICE = 0xFF5F9FD0, ICE2 = 0xFFB8DEF5,
		R_GOLD = 0xFFFFD36B, R_RED = 0xFFD01A35, R_GREEN = 0xFF1F8A45, R_WHITE = 0xFFF6F1E6, R_ICE = 0xFFA5D6FF;

	public static final List<Reward> ALL = List.of(
		new Reward(1, "snowflake_wings", "Snowflake Wings", "Flügel aus leuchtenden Schneeflocken", RARE, BACK, "Wings", ICE, ICE2, R_GOLD),
		new Reward(2, "christmas_trail", "Christmas Trail", "Roter und grüner Partikel-Trail", COMMON, TRAIL, "Trail", RED, RED2, R_GREEN),
		new Reward(3, "candy_cane_sword", "Candy Cane Sword", "3D-Schwert im Zuckerstangen-Design", UNCOMMON, HAND, "Weapon", WHITE, WHITE2, R_RED),
		new Reward(4, "snowfall_aura", "Snowfall Aura", "Kleine schwebende Schneeflocken", COMMON, AURA, "Aura", GREEN, GREEN2, R_WHITE),
		new Reward(5, "gift_backpack", "Gift Backpack", "3D-Rucksack in Geschenkoptik", UNCOMMON, BACK, "Backbling", RED, RED2, R_GOLD),
		new Reward(6, "frozen_crown", "Frozen Crown", "Eiskrone mit leuchtenden Kristallen", RARE, HEAD, "Headwear", ICE, ICE2, R_WHITE),
		new Reward(7, "reindeer_horns", "Reindeer Horns", "Weihnachtliches Rentiergeweih", UNCOMMON, HEAD, "Headwear", GREEN, GREEN2, R_RED),
		new Reward(8, "candy_cane_wings", "Candy Cane Wings", "Flügel im rot-weißen Zuckerstangen-Look", RARE, BACK, "Wings", RED, WHITE2, R_WHITE),
		new Reward(9, "snow_globe", "Snow Globe", "Schwebende Schneekugel", UNCOMMON, FLOAT, "Floating", NIGHT, NIGHT2, R_GOLD),
		new Reward(10, "christmas_halo", "Christmas Halo", "Goldener Weihnachts-Heiligenschein", RARE, HEAD, "Halo", WHITE, WHITE2, R_GOLD),
		new Reward(11, "santa_hat", "Santa Hat", "Hochwertige 3D-Weihnachtsmütze", UNCOMMON, HEAD, "Headwear", RED, RED2, R_WHITE),
		new Reward(12, "ice_crystal_trail", "Ice Crystal Trail", "Eisige Kristallpartikel", RARE, TRAIL, "Trail", NIGHT, NIGHT2, R_ICE),
		new Reward(13, "golden_bell", "Golden Bell", "Schwebende goldene Weihnachtsglocken", RARE, FLOAT, "Floating", GREEN, GREEN2, R_GOLD),
		new Reward(14, "present_hammer", "Present Hammer", "3D-Hammer in Geschenkoptik", EPIC, HAND, "Weapon", GREEN, GREEN2, R_RED),
		new Reward(15, "reindeer_companion", "Reindeer Companion", "Kleines schwebendes Rentier", EPIC, COMPANION, "Companion", RED, RED2, R_GOLD),
		new Reward(16, "frozen_wings", "Frozen Wings", "Transparente Eisflügel", EPIC, BACK, "Wings", NIGHT, NIGHT2, R_ICE),
		new Reward(17, "christmas_lantern", "Christmas Lantern", "Leuchtende Weihnachtslaterne", RARE, FLOAT, "Floating", GOLD, GOLD2, R_RED),
		new Reward(18, "snowman_companion", "Snowman Companion", "Kleiner animierter Schneemann", EPIC, COMPANION, "Companion", RED, RED2, R_WHITE),
		new Reward(19, "golden_gift_wings", "Golden Gift Wings", "Goldene Flügel mit Geschenkdetails", LEGENDARY, BACK, "Wings", GOLD, GOLD2, R_RED),
		new Reward(20, "winter_storm_aura", "Winter Storm Aura", "Bewegte Schneesturm-Partikel", EPIC, AURA, "Aura", ICE, ICE2, R_GOLD),
		new Reward(21, "santas_sleigh_trail", "Santa's Sleigh Trail", "Magischer Schlitten-Partikeleffekt", LEGENDARY, TRAIL, "Trail", RED, RED2, R_GOLD),
		new Reward(22, "christmas_dragon", "Christmas Dragon", "Kleiner weihnachtlicher 3D-Drache", LEGENDARY, COMPANION, "Companion", GOLD, GOLD2, R_GREEN),
		new Reward(23, "legendary_ice_crown", "Legendary Ice Crown", "Legendäre Krone aus Eis und Gold", LEGENDARY, HEAD, "Headwear", GOLD, GOLD2, R_ICE),
		new Reward(24, "ultimate_christmas_aura", "ULTIMATE CHRISTMAS AURA", "Exklusive legendäre Weihnachts-Aura – nur am 24. Dezember", MYTHIC, AURA, "Mythic Aura", 0xFF3D0412, 0xFF7A0C22, R_GOLD)
	);

	private Rewards() {}

	public static Reward byDay(int day) { return day >= 1 && day <= ALL.size() ? ALL.get(day - 1) : null; }

	public static Reward byId(String id) {
		for (Reward r : ALL) if (r.id().equals(id)) return r;
		return null;
	}
}
