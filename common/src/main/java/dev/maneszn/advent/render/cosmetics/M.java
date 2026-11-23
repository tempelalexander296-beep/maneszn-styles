package dev.maneszn.advent.render.cosmetics;

/** Small math helpers for animation code. */
final class M {
	static final float PI = (float) Math.PI;
	static float sin(double a) { return (float) Math.sin(a); }
	static float cos(double a) { return (float) Math.cos(a); }
	static float lerp(float a, float b, float t) { return a + (b - a) * t; }
	static float clamp01(float x) { return Math.max(0, Math.min(1, x)); }
	/** Pulsing ARGB brightness for blinking lights. */
	static int pulse(int rgb, float k) {
		int r = (int) (((rgb >> 16) & 255) * k), g = (int) (((rgb >> 8) & 255) * k), b = (int) ((rgb & 255) * k);
		return 0xFF000000 | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
	}
	static int alpha(int argb, float a) { return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | (argb & 0xFFFFFF); }
	private M() {}
}
