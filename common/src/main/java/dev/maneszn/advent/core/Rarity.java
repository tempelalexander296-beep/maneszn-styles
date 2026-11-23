package dev.maneszn.advent.core;

/** Rarity tiers. Colours are ARGB and drive door frames, name colours and FX. */
public enum Rarity {
	COMMON("Common", 0xFFC9D3DD, 0xFFE9F1F8),
	UNCOMMON("Uncommon", 0xFF4FD07A, 0xFF9CF5B8),
	RARE("Rare", 0xFF4AA8FF, 0xFFA5D6FF),
	EPIC("Epic", 0xFFB45CFF, 0xFFDCB0FF),
	LEGENDARY("Legendary", 0xFFFFB52E, 0xFFFFE29A),
	MYTHIC("Mythic", 0xFFFF3B5C, 0xFFFFD36B);

	public final String label;
	public final int color;
	public final int glow;

	Rarity(String label, int color, int glow) { this.label = label; this.color = color; this.glow = glow; }
}
