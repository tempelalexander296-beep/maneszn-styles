package dev.maneszn.advent.render;

/** Per-frame information about whoever wears the cosmetic. */
public final class Wearer {
	public float time;       // seconds (smooth, partial ticks included)
	public float walkSpeed;  // 0 … 1
	public boolean preview;  // GUI preview → show trails as orbiting showcase
	public float intro = 1f; // 0 → 1 during the reveal animation
}
