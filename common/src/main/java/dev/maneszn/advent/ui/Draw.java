package dev.maneszn.advent.ui;

/** Drawing primitives built from rectangles (works with every GUI backend). */
final class Draw {
	private Draw() {}

	static int alpha(int rgb, float a) { return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | (rgb & 0xFFFFFF); }
	static int shade(int argb, float k) {
		int r = (int) (((argb >> 16) & 255) * k), g = (int) (((argb >> 8) & 255) * k), b = (int) ((argb & 255) * k);
		return (argb & 0xFF000000) | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
	}
	static int lerpColor(int a, int b, float t) {
		int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t)), g = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t)), bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * t));
		return 0xFF000000 | (r << 16) | (g << 8) | bl;
	}

	static void frame(Gfx g, int x0, int y0, int x1, int y1, int c) { g.fill(x0, y0, x1, y0 + 1, c); g.fill(x0, y1 - 1, x1, y1, c); g.fill(x0, y0, x0 + 1, y1, c); g.fill(x1 - 1, y0, x1, y1, c); }

	static void panel(Gfx g, int x0, int y0, int x1, int y1, int bg, int border) {
		for (int i = 3; i >= 1; i--) g.fill(x0 + i, y0 + i, x1 + i, y1 + i, 0x40000000);
		g.fill(x0, y0, x1, y1, bg); g.gradient(x0, y0, x1, y0 + Math.max(2, (y1 - y0) / 3), 0x18FFFFFF, 0); frame(g, x0, y0, x1, y1, border);
	}

	static void circle(Gfx g, int cx, int cy, int r, int c) {
		for (int dy = -r; dy <= r; dy++) { int dx = (int) Math.sqrt(r * r - dy * dy); g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, c); }
	}
	static void ellipse(Gfx g, int cx, int cy, int rx, int ry, int c) { ellipse(g, cx, cy, rx, ry, c, false); }
	static void ellipse(Gfx g, int cx, int cy, int rx, int ry, int c, boolean outline) {
		for (int dy = -ry; dy <= ry; dy++) {
			int dx = (int) (rx * Math.sqrt(1 - (dy / (float) ry) * (dy / (float) ry)));
			if (outline) { g.fill(cx - dx, cy + dy, cx - dx + 2, cy + dy + 1, c); g.fill(cx + dx - 1, cy + dy, cx + dx + 1, cy + dy + 1, c); }
			else g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, c);
		}
	}

	static void pine(Gfx g, int cx, int base, int h, int c) {
		for (int i = 0; i < h; i++) { int w = (int) ((i / (float) h) * h * .42f * (1 + .25f * ((i % 8) / 8f))); g.fill(cx - w, base - h + i, cx + w + 1, base - h + i + 1, c); }
		g.fill(cx - 1, base - 3, cx + 2, base, 0xFF1A0E08);
	}

	/** Text with a stacked-shadow extrusion (3D lettering). centerX = horizontal centre. */
	static void extruded(Gfx g, String s, int centerX, int y, float scale, int face, int deep, boolean center) {
		int w = g.textWidth(s);
		g.push(); g.translate(centerX, y); g.scale(scale); g.translate(center ? -w / 2f : 0, 0);
		for (int i = 3; i >= 1; i--) g.text(s, 0, i, shade(deep, 1f - i * .12f), false);
		g.text(s, 0, 0, face, false);
		g.pop();
	}

	static void ribbonV(Gfx g, int x, int y, int w, int h, int c) {
		g.fill(x, y, x + w, y + h, c); g.fill(x, y, x + 1, y + h, shade(c, .6f)); g.fill(x + w - 1, y, x + w, y + h, shade(c, .6f));
		g.fill(x + w / 2, y, x + w / 2 + 1, y + h, alpha(0xFFFFFF, .45f));
	}
	static void ribbonH(Gfx g, int x, int y, int w, int h, int c) {
		g.fill(x, y, x + w, y + h, c); g.fill(x, y, x + w, y + 1, shade(c, .6f)); g.fill(x, y + h - 1, x + w, y + h, shade(c, .6f));
		g.fill(x, y + h / 2, x + w, y + h / 2 + 1, alpha(0xFFFFFF, .45f));
	}
	static void bow(Gfx g, int cx, int cy, int s, int c) {
		int d = shade(c, .65f);
		ellipse(g, cx - s / 2 - 1, cy - s / 4, s / 2, s / 3, d); ellipse(g, cx + s / 2 + 1, cy - s / 4, s / 2, s / 3, d);
		ellipse(g, cx - s / 2, cy - s / 4 - 1, s / 2 - 1, s / 3 - 1, c); ellipse(g, cx + s / 2, cy - s / 4 - 1, s / 2 - 1, s / 3 - 1, c);
		circle(g, cx, cy - s / 5, Math.max(1, s / 5), shade(c, 1.2f));
	}
	static void lock(Gfx g, int x, int y, int s) {
		g.fill(x - 1, y - 1, x + s + 1, y + s + 2, 0xD005080F);
		g.fill(x + s / 4, y + 1, x + s - s / 4, y + 2, 0xFF8A94A8); g.fill(x + s / 4, y + 1, x + s / 4 + 1, y + s / 2, 0xFF8A94A8); g.fill(x + s - s / 4 - 1, y + 1, x + s - s / 4, y + s / 2, 0xFF8A94A8);
		g.fill(x + 1, y + s / 2, x + s - 1, y + s, 0xFF7D889C); g.fill(x + s / 2 - 1, y + s / 2 + 2, x + s / 2 + 1, y + s - 2, 0xFF20242C);
	}
	static void check(Gfx g, int x, int y, int s) {
		circle(g, x + s / 2, y + s / 2, s / 2 + 1, 0xFF0B2A17); circle(g, x + s / 2, y + s / 2, s / 2, 0xFF1F8A45);
		for (int i = 0; i < s / 3; i++) g.fill(x + 2 + i, y + s / 2 + i - 1, x + 4 + i, y + s / 2 + i + 1, 0xFFFFFFFF);
		for (int i = 0; i < s / 2; i++) g.fill(x + 2 + s / 3 + i, y + s / 2 + s / 3 - i - 2, x + 4 + s / 3 + i, y + s / 2 + s / 3 - i, 0xFFFFFFFF);
	}
	static void starShape(Gfx g, int cx, int cy, int r, int c, float rot) {
		g.push(); g.translate(cx, cy); g.rotate(rot);
		for (int i = 0; i < 4; i++) { g.rotate((float) Math.PI / 4); g.fill(-1, -r, 1, r, c); }
		g.pop(); circle(g, cx, cy, Math.max(1, r / 4), c);
	}
	static void rays(Gfx g, int cx, int cy, int len, float rot, int c) {
		g.push(); g.translate(cx, cy); g.rotate(rot);
		for (int i = 0; i < 12; i++) { g.rotate((float) Math.PI / 6); g.fill(-len / 60 - 1, 0, len / 60 + 1, len, c); }
		g.pop();
	}
	static void bar(Gfx g, int x0, int y0, int x1, int y1, float k, float t) {
		g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFF05070E); g.fill(x0, y0, x1, y1, 0xFF1A2030);
		int w = (int) ((x1 - x0) * Math.max(0, Math.min(1, k)));
		if (w > 0) { g.gradient(x0, y0, x0 + w, y1, 0xFFFFE39A, 0xFFE3B553); g.fill(x0, y0, x0 + w, y0 + 1, 0x80FFFFFF); }
	}
	/** Animated running highlight around a golden frame (legendary / mythic). */
	static void runningFrame(Gfx g, int x, int y, int w, int h, float t, boolean mythic) {
		frame(g, x - 1, y - 1, x + w + 1, y + h + 1, mythic ? 0xFFFF3B5C : 0xFFB8862B);
		frame(g, x, y, x + w, y + h, 0xFFFFD36B);
		int per = 2 * (w + h); int seg = Math.max(8, per / 8);
		for (int k = 0; k < (mythic ? 3 : 2); k++) {
			int p0 = (int) ((t * 90 + k * per / (mythic ? 3f : 2f)) % per);
			for (int i = 0; i < seg; i++) {
				int p = (p0 + i) % per; float a = 1 - i / (float) seg; int px, py;
				if (p < w) { px = x + p; py = y; } else if (p < w + h) { px = x + w - 1; py = y + p - w; } else if (p < 2 * w + h) { px = x + w - 1 - (p - w - h); py = y + h - 1; } else { px = x; py = y + h - 1 - (p - 2 * w - h); }
				g.fill(px - 1, py - 1, px + 2, py + 2, alpha(0xFFFFFF, a));
			}
		}
	}
}
