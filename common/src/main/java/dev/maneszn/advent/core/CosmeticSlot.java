package dev.maneszn.advent.core;

/** Where a cosmetic is worn. One cosmetic per slot can be equipped at a time. */
public enum CosmeticSlot {
	HEAD("Kopf"), BACK("Rücken"), HAND("Hand"), FLOAT("Schwebend"), COMPANION("Begleiter"), AURA("Aura"), TRAIL("Trail");

	public final String label;
	CosmeticSlot(String label) { this.label = label; }
}
