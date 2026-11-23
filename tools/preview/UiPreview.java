import dev.maneszn.advent.core.CosmeticSlot;
import dev.maneszn.advent.core.PlayerAdvent;
import dev.maneszn.advent.mesh.Tex;
import dev.maneszn.advent.ui.Bridge;
import dev.maneszn.advent.ui.CalendarUI;
import dev.maneszn.advent.ui.Gfx;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import javax.imageio.ImageIO;

/** Renders the calendar UI with Java2D at Minecraft GUI scale (QA only, not part of the mod). */
public final class UiPreview {
	public static void main(String[] a) throws Exception {
		BufferedImage atlas = ImageIO.read(new File(a[0]));
		int W = 480, H = 270, SCALE = 3, day = Integer.parseInt(a[2]);
		PlayerAdvent st = new PlayerAdvent();
		for (int d = 1; d < day; d += 1) if (d % 4 != 0) st.claim(d, day);
		long now = java.time.LocalDate.of(2026, 12, Math.max(1, day)).atTime(15, 30).atZone(java.time.ZoneId.of("Europe/Berlin")).toInstant().toEpochMilli();
		Bridge br = new Bridge() {
			public int currentDay() { return day; } public long nowMs() { return now; } public long nextUnlockMs() { return now + 8 * 3600_000L + 1234_000; }
			public long christmasEveMs() { return now + (24 - day) * 86_400_000L; } public String zone() { return "Europe/Berlin"; } public PlayerAdvent state() { return st; }
			public void claim(int d) {} public void equip(CosmeticSlot s, String id) {} public void preview(String id, float i) {} public void sound(String k) {} public void close() {}
		};
		CalendarUI ui = new CalendarUI(br);
		BufferedImage img = new BufferedImage(W * SCALE, H * SCALE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = img.createGraphics(); g2.scale(SCALE, SCALE);
		Font font = new Font(Font.MONOSPACED, Font.BOLD, 8);
		g2.setFont(font); FontMetrics fm = g2.getFontMetrics();
		ArrayDeque<AffineTransform> stack = new ArrayDeque<>();
		Gfx g = new Gfx() {
			public int width() { return W; } public int height() { return H; }
			public void fill(int x0, int y0, int x1, int y1, int c) { g2.setColor(new Color(c, true)); g2.fillRect(Math.min(x0, x1), Math.min(y0, y1), Math.abs(x1 - x0), Math.abs(y1 - y0)); }
			public void gradient(int x0, int y0, int x1, int y1, int t, int b) { g2.setPaint(new GradientPaint(0, y0, new Color(t, true), 0, y1, new Color(b, true))); g2.fillRect(x0, y0, x1 - x0, y1 - y0); }
			public void tile(Tex t, int x, int y, int w, int h, float u0, float v0, float u1, float v1, int argb) {
				int T = 256, tx = (t.ordinal() % 8) * T, ty = (t.ordinal() / 8) * T;
				g2.drawImage(atlas, x, y, x + w, y + h, tx, ty, tx + (int) (T * Math.min(1, u1)), ty + (int) (T * Math.min(1, v1)), null);
			}
			public void text(String s, int x, int y, int c, boolean sh) { if (sh) { g2.setColor(new Color(0x80000000, true)); g2.drawString(s, x + 1, y + 8); } g2.setColor(new Color(c, true)); g2.drawString(s, x, y + 7); }
			public int textWidth(String s) { return fm.stringWidth(s); } public int lineHeight() { return 9; }
			public void push() { stack.push(g2.getTransform()); } public void pop() { g2.setTransform(stack.pop()); }
			public void translate(float x, float y) { g2.translate(x, y); } public void scale(float s) { g2.scale(s, s); } public void rotate(float r) { g2.rotate(r); }
			public void player(int x0, int y0, int x1, int y1, int size, float yaw, float off) { g2.setColor(new Color(0x803A4A6A, true)); int cx = (x0 + x1) / 2, cy = (y0 + y1) / 2; g2.fillRoundRect(cx - size / 3, cy - size, size * 2 / 3, size * 2, 8, 8); }
		};
		ui.mouseMoved(150, 120);
		ui.render(g, 0);
		ImageIO.write(img, "png", new File(a[1]));
	}
}
