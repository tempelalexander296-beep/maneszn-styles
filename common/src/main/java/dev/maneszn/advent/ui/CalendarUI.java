package dev.maneszn.advent.ui;

import dev.maneszn.advent.core.CosmeticSlot;
import dev.maneszn.advent.core.DoorState;
import dev.maneszn.advent.core.PlayerAdvent;
import dev.maneszn.advent.core.Rarity;
import dev.maneszn.advent.core.Reward;
import dev.maneszn.advent.core.Rewards;
import dev.maneszn.advent.mesh.Tex;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The complete advent-calendar interface, version independent.
 * Views: CALENDAR (24 3D gift doors) · REVEAL (opening sequence + 3D preview) · COLLECTION (equip).
 */
public final class CalendarUI {
	enum View { CALENDAR, REVEAL, COLLECTION }

	private final Bridge game;
	private View view = View.CALENDAR;
	private final long start = System.nanoTime();
	private float now;
	private int mx, my;

	// opening sequence
	private int openingDay = -1; private float openingStart; private boolean claimConfirmed; private int pendingResult = -1;
	private int revealDay = -1; private float revealStart;
	private String hoverPreview;
	private String toast; private float toastUntil;
	private float spin, dragFrom = Float.NaN;

	private final List<Btn> buttons = new ArrayList<>();
	private final List<float[]> particles = new ArrayList<>(); // x y vx vy life maxLife argb size
	private final float[][] snow = new float[90][4];
	private final Random rnd = new Random();

	public CalendarUI(Bridge game) {
		this.game = game;
		for (float[] s : snow) { s[0] = rnd.nextFloat(); s[1] = rnd.nextFloat(); s[2] = .3f + rnd.nextFloat(); s[3] = rnd.nextFloat() * 6; }
	}

	/* ================================================================ input */
	public void mouseMoved(double x, double y) { mx = (int) x; my = (int) y; }

	public boolean mouseClicked(double x, double y, int button) {
		mx = (int) x; my = (int) y;
		for (Btn b : buttons) if (b.hit(mx, my)) { game.sound("click"); b.action.run(); return true; }
		if (view == View.CALENDAR && openingDay < 0) {
			int d = hoverDay;
			if (d > 0) {
				DoorState st = state().doorState(d, game.currentDay());
				switch (st) {
					case AVAILABLE -> startOpening(d);
					case CLAIMED -> { revealDay = d; revealStart = now - 10; view = View.REVEAL; }
					case LOCKED -> { game.sound("locked"); toast("🔒 Türchen " + d + " öffnet am " + d + ". Dezember · noch " + span(game.unlockMsOf(d) - game.nowMs())); }
					case MISSED -> toast("Türchen " + d + " wurde verpasst – das Event ist vorbei.");
				}
				return true;
			}
		}
		if (view != View.CALENDAR) { dragFrom = (float) x; return true; }
		return false;
	}

	public void mouseDragged(double x, double y) { if (!Float.isNaN(dragFrom)) { spin += ((float) x - dragFrom) * 2; dragFrom = (float) x; } }
	public void mouseReleased() { dragFrom = Float.NaN; }

	/** ESC: go back one level; returns true if handled (screen stays open). */
	public boolean back() {
		if (view != View.CALENDAR) { view = View.CALENDAR; game.preview(null, 1); return true; }
		return false;
	}

	public void onClaimResult(int day, int resultOrdinal) {
		if (day != openingDay) return;
		pendingResult = resultOrdinal;
		if (resultOrdinal == PlayerAdvent.ClaimResult.OK.ordinal()) claimConfirmed = true;
		else { openingDay = -1; toast(switch (PlayerAdvent.ClaimResult.values()[resultOrdinal]) { case ALREADY_CLAIMED -> "Bereits eingesammelt."; case LOCKED -> "Dieses Türchen ist noch gesperrt."; case MISSED -> "Event beendet."; default -> "Ungültiges Türchen."; }); }
	}

	private void startOpening(int day) {
		openingDay = day; openingStart = now; claimConfirmed = false; pendingResult = -1;
		game.claim(day); // server validates: date, time zone, never twice
		game.sound(day == 24 ? "mythic" : "open");
	}

	private PlayerAdvent state() { return game.state(); }
	private void toast(String s) { toast = s; toastUntil = now + 3.5f; }

	/* ================================================================ render */
	private int hoverDay = -1;

	public void render(Gfx g, float partial) {
		now = (System.nanoTime() - start) / 1e9f;
		buttons.clear();
		int W = g.width(), H = g.height();
		background(g, W, H);
		switch (view) {
			case CALENDAR -> calendar(g, W, H);
			case REVEAL -> reveal(g, W, H);
			case COLLECTION -> collection(g, W, H);
		}
		particles(g);
		for (Btn b : buttons) b.draw(g, b.hit(mx, my));
		if (toast != null && now < toastUntil) {
			int tw = g.textWidth(toast) + 16, x = (W - tw) / 2, y = H - 30;
			Draw.panel(g, x, y, x + tw, y + 16, 0xF0101828, 0xFFFFD36B);
			g.text(toast, x + 8, y + 4, 0xFFFFFFFF, false);
		}
	}

	/* ---------------------------------------------------------------- background: winter night */
	private void background(Gfx g, int W, int H) {
		g.gradient(0, 0, W, H, 0xF2060B1A, 0xF21A0A12);
		Draw.circle(g, (int) (W * .14f), (int) (H * .16f), 34, 0x10DFE9FF); Draw.circle(g, (int) (W * .14f), (int) (H * .16f), 20, 0x18DFE9FF); Draw.circle(g, (int) (W * .14f), (int) (H * .16f), 11, 0x60EAF2FF);
		// forest silhouettes with fairy lights
		for (int layer = 0; layer < 2; layer++) {
			int col = layer == 0 ? 0xFF0B1C1A : 0xFF071310, base = H - (layer == 0 ? 6 : 0);
			for (int i = -1; i < W / 26 + 2; i++) {
				int cx = i * 26 + (layer * 13) + (int) (Math.sin(i * 3.7) * 6), th = 34 + (int) (Math.abs(Math.sin(i * 1.9 + layer)) * 30) - layer * 6;
				Draw.pine(g, cx, base, th, col);
				if (layer == 1 && i % 2 == 0) for (int k = 0; k < 4; k++) {
					float bl = .4f + .6f * (float) (.5 + .5 * Math.sin(now * 2.4 + i * 1.3 + k * 2));
					int ly = base - th / 5 - k * th / 6, lx = cx + (int) ((k % 2 == 0 ? -1 : 1) * th * .1f * (4 - k) / 4f);
					int c = new int[] { 0xFFD36B, 0xFF5A5A, 0x8FD3FF, 0x9EFFA8 }[(i + k) & 3];
					g.fill(lx, ly, lx + 2, ly + 2, Draw.alpha(c, bl));
				}
			}
		}
		g.fill(0, H - 4, W, H, 0xFFDDE7F5);
		for (float[] s : snow) {
			float y = (s[1] + now * .04f * s[2]) % 1f, x = (s[0] + (float) Math.sin(now * .5f + s[3]) * .01f) % 1f;
			int sz = s[2] > 1.05f ? 2 : 1;
			g.fill((int) (x * W), (int) (y * H), (int) (x * W) + sz, (int) (y * H) + sz, Draw.alpha(0xFFFFFF, .35f + s[2] * .4f));
		}
	}

	/* ---------------------------------------------------------------- banner + countdown */
	private int banner(Gfx g, int W, int top) {
		String k = "WINTER EVENT · 24 EXKLUSIVE COSMETICS";
		g.text(k, (W - g.textWidth(k)) / 2, top, 0xFFFFE7A8, false);
		Draw.extruded(g, "CHRISTMAS", W / 2, top + 11, 1.3f, 0xFFFF5A6E, 0xFF4A0716, true);
		float s = Math.min(3f, Math.max(1.6f, W / 260f));
		Draw.extruded(g, "ADVENT CALENDAR", W / 2, top + 24, s, 0xFFFFD36B, 0xFF5A3407, true);
		int y = top + 24 + (int) (g.lineHeight() * s) + 5;
		long nowMs = game.nowMs(); int day = game.currentDay();
		String cd;
		if (day < 24) cd = "Heiligabend in " + span(game.christmasEveMs() - nowMs) + (day == 0 ? "  ·  Event startet am 1. Dezember" : "");
		else if (day == 24) cd = "✨ Heiligabend – das große Finale ist freigeschaltet!";
		else cd = "Frohe Weihnachten! Das Event ist beendet.";
		String nx = game.nextUnlockMs() > 0 ? "Nächstes Türchen in " + span(game.nextUnlockMs() - nowMs) : "";
		String line = nx.isEmpty() ? cd : cd + "   ·   " + nx;
		int tw = g.textWidth(line) + 20;
		Draw.panel(g, (W - tw) / 2, y, (W + tw) / 2, y + 15, 0xE04A0716, 0x90FFD36B);
		g.text(line, (W - tw) / 2 + 10, y + 4, 0xFFFFFFFF, false);
		return y + 20;
	}

	/* ---------------------------------------------------------------- calendar view */
	private void calendar(Gfx g, int W, int H) {
		int top = banner(g, W, 6);
		int footer = 24, gap = Math.max(3, W / 160);
		int availH = H - top - footer - 6;
		int c = Math.min((W - 24 - 7 * gap) / 8, (availH - 3 * gap) / 4);
		c = Math.max(22, c);
		int gridW = 8 * c + 7 * gap, x0 = (W - gridW) / 2, y0 = top + Math.max(0, (availH - (4 * c + 3 * gap)) / 2);
		hoverDay = -1;
		PlayerAdvent st = state(); int cur = game.currentDay();
		for (int d = 1; d <= 23; d++) {
			int i = d - 1, col = i % 6, row = i / 6;
			int x = x0 + col * (c + gap), y = y0 + row * (c + gap);
			boolean hov = openingDay < 0 && mx >= x && my >= y && mx < x + c && my < y + c;
			if (hov) hoverDay = d;
			door(g, Rewards.byDay(d), x, y, c, c, st.doorState(d, cur), hov);
		}
		medallion(g, x0 + 5 * (c + gap), y0 + 3 * (c + gap), c, st.claimedCount());
		int bx = x0 + 6 * (c + gap), bw = 2 * c + gap, bh = 4 * c + 3 * gap;
		boolean hov24 = openingDay < 0 && mx >= bx && my >= y0 && mx < bx + bw && my < y0 + bh;
		if (hov24) hoverDay = 24;
		door(g, Rewards.byDay(24), bx, y0, bw, bh, st.doorState(24, cur), hov24);
		// progress + footer buttons
		int n = st.claimedCount(), fy = H - footer;
		String prog = n + " / 24 eingesammelt";
		int pw = Math.max(40, gridW - 180 - g.textWidth(prog) - 10);
		Draw.bar(g, x0, fy + 6, x0 + pw, fy + 12, n / 24f, now);
		g.text(prog, x0 + pw + 6, fy + 5, 0xFFFFE7A8, true);
		buttons.add(new Btn(x0 + gridW - 170, fy + 1, 90, 18, "✦ Sammlung", true, () -> { view = View.COLLECTION; }));
		buttons.add(new Btn(x0 + gridW - 74, fy + 1, 74, 18, "Schließen", false, game::close));
		if (hoverDay > 0 && openingDay < 0) tooltip(g, Rewards.byDay(hoverDay), st.doorState(hoverDay, cur));
		// opening sequence progression
		if (openingDay > 0) {
			float t = now - openingStart;
			if (claimConfirmed && t > (openingDay == 24 ? 2.4f : 1.5f)) { revealDay = openingDay; revealStart = now; openingDay = -1; view = View.REVEAL; game.sound(revealDay == 24 ? "reveal_mythic" : "reveal"); }
			else if (!claimConfirmed && t > 4f) { openingDay = -1; toast("Keine Antwort vom Server."); }
		}
	}

	private static Tex paper(Reward r) {
		int a = r.doorColorA();
		if (r.rarity() == Rarity.MYTHIC) return Tex.PAPER_ROYAL_PLAID;
		return switch (a) {
			case 0xFF8E0E1C -> r.day() % 2 == 0 ? Tex.PAPER_RED_STARS : Tex.PAPER_RED_PLAID;
			case 0xFF0D4A2C -> Tex.PAPER_GREEN_DOTS;
			case 0xFFA8781F -> Tex.PAPER_GOLD_STRIPES;
			case 0xFFECE7DE -> Tex.PAPER_WHITE_FLAKES;
			case 0xFF0C1A33 -> Tex.PAPER_NIGHT_STARS;
			default -> Tex.PAPER_ICE_FLAKES;
		};
	}

	/** A 3D gift-box door: extruded depth, wrapping paper, ribbon, bow, embossed number, state overlay. */
	private void door(Gfx g, Reward r, int x, int y, int w, int h, DoorState st, boolean hover) {
		boolean big = r.day() == 24, opening = openingDay == r.day();
		float ot = opening ? now - openingStart : 0;
		int depth = Math.max(3, Math.min(w, h) / 9) + (hover && st == DoorState.AVAILABLE ? 2 : 0);
		if (hover && st == DoorState.AVAILABLE) { x -= 1; y -= 2; }
		if (opening && ot < 1.0f) { x += (int) (Math.sin(ot * 60) * 1.5f * Math.min(1, ot * 2)); }
		int rc = r.rarity().color;
		// glow behind available / opening / mythic doors
		if (st == DoorState.AVAILABLE || opening || big) {
			float p = opening ? Math.min(1, ot * 1.5f) : .5f + .5f * (float) Math.sin(now * 3.5f + r.day());
			int glowC = st == DoorState.AVAILABLE || opening ? 0xFFD36B : rc;
			for (int i = 6; i >= 1; i--) Draw.frame(g, x - i, y - i, x + w + i, y + h + i, Draw.alpha(glowC, (opening ? .5f : .18f) * p * (7 - i) / 6f));
		}
		// extrusion (depth) → reads as a real 3D box
		int side = Draw.shade(r.doorColorA(), .45f);
		for (int i = depth; i >= 1; i--) g.fill(x + i, y + i, x + w + i, y + h + i, Draw.shade(side, 1f - i * .04f));
		boolean showOpen = st == DoorState.CLAIMED || (opening && ot > .7f);
		if (showOpen) {
			// inside of the box with the reward glow
			g.gradient(x, y, x + w, y + h, 0xFF14080C, 0xFF05070E);
			float pulse = .55f + .45f * (float) Math.sin(now * 2 + r.day());
			Draw.circle(g, x + w / 2, y + h / 2, Math.min(w, h) / 3, Draw.alpha(rc, .22f * pulse));
			Draw.circle(g, x + w / 2, y + h / 2, Math.min(w, h) / 6, Draw.alpha(r.rarity().glow, .35f * pulse));
			Draw.starShape(g, x + w / 2, y + h / 2, Math.min(w, h) / 5, Draw.alpha(r.rarity().glow, .9f), now * .6f);
			Draw.frame(g, x, y, x + w, y + h, rc);
			String type = r.type().toUpperCase();
			if (g.textWidth(type) < w - 4) g.text(type, x + (w - g.textWidth(type)) / 2, y + h - 11, Draw.alpha(rc, .9f), true);
			// the lid swung open on its hinge (foreshortened)
			float k = opening ? Math.min(1, (ot - .7f) / .6f) : 1f;
			int lidW = Math.max(2, (int) (w * (1 - .82f * easeOut(k))));
			paperFace(g, r, x, y - (int) (1 * k), lidW, h + (int) (2 * k), big, true);
			if (st == DoorState.CLAIMED) Draw.check(g, x + w - 13, y + 3, 10);
		} else {
			paperFace(g, r, x, y, w, h, big, false);
			if (st == DoorState.LOCKED) { g.fill(x, y, x + w, y + h, 0x88000000); Draw.lock(g, x + w - 13, y + 3, 10); }
			if (st == DoorState.MISSED) { g.fill(x, y, x + w, y + h, 0xAA202430); g.text("–", x + w - 9, y + 3, 0xFFAAB2C0, false); }
			if (st == DoorState.AVAILABLE) {
				float p = .5f + .5f * (float) Math.sin(now * 4);
				Draw.frame(g, x - 1, y - 1, x + w + 1, y + h + 1, Draw.alpha(0xFFD36B, .6f + .4f * p));
				String b = big ? "FINALE" : r.day() < game.currentDay() ? "NACHHOLEN" : "HEUTE"; int bw = g.textWidth(b) + 6;
				g.fill(x + (w - bw) / 2, y - 5, x + (w + bw) / 2, y + 5, 0xFFFFD36B); g.text(b, x + (w - bw) / 2 + 3, y - 4, 0xFF2A1505, false);
			}
		}
		// legendary / mythic animated golden frame
		if (r.rarity().ordinal() >= Rarity.LEGENDARY.ordinal()) Draw.runningFrame(g, x, y, w, h, now, r.rarity() == Rarity.MYTHIC);
		else if (r.rarity() == Rarity.EPIC) Draw.frame(g, x, y, x + w, y + h, Draw.alpha(Rarity.EPIC.color, .8f));
		// opening FX: snowflakes + sparkle burst
		if (opening) {
			if (ot > .5f && ot < .9f && rnd.nextFloat() < .7f) for (int i = 0; i < 3; i++) spark(x + w / 2f, y + h / 2f, 1.6f, 0xFFFFFFFF, 2);
			if (ot > 1.2f && ot < 1.3f) for (int i = 0; i < (big ? 120 : 45); i++) spark(x + w / 2f, y + h / 2f, big ? 4.5f : 3f, i % 3 == 0 ? r.rarity().glow : i % 3 == 1 ? rc : 0xFFFFFFFF, 1 + rnd.nextInt(2));
			if (big && ot > 1.3f) g.fill(0, 0, g.width(), g.height(), Draw.alpha(0xFFE7A8, Math.max(0, .85f - (ot - 1.3f) * .9f)));
		}
	}

	private void paperFace(Gfx g, Reward r, int x, int y, int w, int h, boolean big, boolean lid) {
		g.tile(paper(r), x, y, w, h, 0, 0, big ? 1.4f : 1, big ? 2.4f : 1, 0xFFFFFFFF);
		g.gradient(x, y, x + w, y + h / 2, 0x38FFFFFF, 0x00FFFFFF);
		g.gradient(x, y + h / 2, x + w, y + h, 0x00000000, 0x55000000);
		int rib = r.ribbonColor(), rw = Math.max(2, Math.min(w, h) / 8);
		Draw.ribbonV(g, x + (w - rw) / 2, y, rw, h, rib);
		if (!lid) {
			Draw.ribbonH(g, x, y + (h - rw) / 2, w, rw, rib);
			int s = Math.max(4, Math.min(w, h) / 5);
			Draw.bow(g, x + w / 2, y + h / 2, s, rib);
			float ns = big ? Math.max(2.5f, Math.min(w, h) / 22f) : Math.max(1f, w / 30f);
			Draw.extruded(g, String.valueOf(r.day()), x + 3 + (int) (g.textWidth(String.valueOf(r.day())) * ns / 2), y + 3, ns, big ? 0xFFFFD36B : 0xFFFFFFFF, 0xFF1A0508, true);
			if (big) {
				Draw.extruded(g, "MYTHIC", x + w / 2, y + h - 40, 1.2f, 0xFFFF7A8E, 0xFF1A0206, true);
				Draw.extruded(g, "ULTIMATE", x + w / 2, y + h - 28, 1f, 0xFFFFE7A8, 0xFF3D0412, true);
				Draw.extruded(g, "CHRISTMAS AURA", x + w / 2, y + h - 18, 1f, 0xFFFFE7A8, 0xFF3D0412, true);
				Draw.rays(g, x + w / 2, y + h / 2, Math.min(w, h), now * .3f, 0x14FFD36B);
			}
			int gem = r.rarity().color; g.fill(x + w - 6, y + h - 6, x + w - 2, y + h - 2, gem); Draw.frame(g, x + w - 7, y + h - 7, x + w - 1, y + h - 1, 0xFF000000);
		} else g.fill(x, y, x + w, y + h, 0x30000000);
		Draw.frame(g, x, y, x + w, y + h, 0x60000000);
	}

	/** Decorative progress medallion filling the free grid cell. */
	private void medallion(Gfx g, int x, int y, int c, int n) {
		for (int i = 3; i >= 1; i--) g.fill(x + i, y + i, x + c + i, y + c + i, 0xFF05070E);
		g.gradient(x, y, x + c, y + c, 0xFF1B2A4A, 0xFF0A1020);
		Draw.frame(g, x, y, x + c, y + c, 0xFFB8862B);
		Draw.pine(g, x + c / 2, y + c - 6, c - 16, 0xFF1F6A3A);
		Draw.starShape(g, x + c / 2, y + 10, 5, 0xFFFFD36B, now);
		for (int k = 0; k < 5; k++) { float bl = .4f + .6f * (float) (.5 + .5 * Math.sin(now * 3 + k * 1.7)); g.fill(x + c / 2 - 6 + k * 3, y + c / 2 + (k % 2) * 6, x + c / 2 - 4 + k * 3, y + c / 2 + 2 + (k % 2) * 6, Draw.alpha(new int[] { 0xFFD36B, 0xFF5A5A, 0x8FD3FF }[k % 3], bl)); }
		String t = n + "/24"; g.text(t, x + (c - g.textWidth(t)) / 2, y + c - 10, 0xFFFFFFFF, true);
	}

	private void tooltip(Gfx g, Reward r, DoorState st) {
		String[] lines = { r.day() + ". Dezember – " + r.name(), r.rarity().label + " · " + r.type() + " · " + r.slot().label, r.description(),
			switch (st) { case LOCKED -> "🔒 Gesperrt"; case AVAILABLE -> "✦ Klicken zum Öffnen!"; case CLAIMED -> "✔ Eingesammelt – klicken zum Ansehen"; case MISSED -> "Verpasst"; } };
		int w = 0; for (String l : lines) w = Math.max(w, g.textWidth(l));
		int x = Math.min(mx + 10, g.width() - w - 14), y = Math.max(4, my - 50), h = lines.length * 11 + 6;
		Draw.panel(g, x, y, x + w + 12, y + h, 0xF20A1020, r.rarity().color);
		for (int i = 0; i < lines.length; i++) g.text(lines[i], x + 6, y + 4 + i * 11, i == 0 ? r.rarity().glow : i == 3 ? 0xFFFFD36B : 0xFFDDE4F0, i == 0);
	}

	/* ---------------------------------------------------------------- reveal view */
	private void reveal(Gfx g, int W, int H) {
		Reward r = Rewards.byDay(revealDay); if (r == null) { view = View.CALENDAR; return; }
		float t = now - revealStart, intro = clamp01(t / (r.day() == 24 ? 2.6f : 1.4f));
		boolean mythic = r.rarity() == Rarity.MYTHIC;
		int pw = (int) (W * .52f), cx = pw / 2 + 10, cy = H / 2;
		// light rays + rarity halo behind the 3D preview
		Draw.rays(g, cx, cy, (int) (H * 1.1f), now * (mythic ? .35f : .2f), Draw.alpha(r.rarity().glow, mythic ? .16f : .08f));
		for (int i = 5; i >= 1; i--) Draw.circle(g, cx, cy + 10, (int) (H * .09f * i), Draw.alpha(r.rarity().color, .035f * (6 - i)));
		// pedestal
		Draw.ellipse(g, cx, (int) (H * .84f), (int) (H * .22f), (int) (H * .045f), 0xC0141821);
		Draw.ellipse(g, cx, (int) (H * .84f), (int) (H * .22f), (int) (H * .045f), Draw.alpha(r.rarity().color, .35f), true);
		boolean big = r.slot() == CosmeticSlot.AURA || r.slot() == CosmeticSlot.TRAIL;
		int size = (int) (H * (mythic ? .17f : big ? .22f : .3f));
		game.preview(r.id(), easeOut(intro));
		float yaw = spin + (Float.isNaN(dragFrom) ? now * 28 : 0);
		g.player(10, 18, pw + 10, H - 10, size, yaw, mythic ? -.15f : 0f);
		if (t < .5f) g.fill(0, 0, W, H, Draw.alpha(mythic ? 0xFFE7A8 : 0xFFFFFF, (.5f - t) * 1.6f));
		if (rnd.nextFloat() < (mythic ? .9f : .35f)) spark(cx + (rnd.nextFloat() - .5f) * pw * .8f, H, .9f, rnd.nextBoolean() ? r.rarity().glow : 0xFFFFE7A8, 1);
		// info card (fades in = step 6: name + rarity)
		float a = clamp01((t - (mythic ? 2.0f : 1.0f)) / .6f);
		int x = pw + 20, w = W - x - 16, y = (int) (H * .2f);
		if (a > 0) {
			Draw.panel(g, x - 8, y - 10, x + w, y + 130, Draw.alpha(0x0A1020, .85f * a), Draw.alpha(r.rarity().color, a));
			g.text("TÜRCHEN " + r.day() + " · " + r.day() + ". DEZEMBER", x, y, Draw.alpha(0xFFE7A8, a), false);
			String rl = r.rarity().label.toUpperCase(); int rw = g.textWidth(rl) + 12;
			int pill = mythic ? Draw.lerpColor(0xFFFF3B5C, 0xFFFFB52E, .5f + .5f * (float) Math.sin(now * 3)) : r.rarity().color;
			g.fill(x, y + 13, x + rw, y + 26, Draw.alpha(Draw.shade(pill, .5f), a)); Draw.frame(g, x, y + 13, x + rw, y + 26, Draw.alpha(pill, a));
			g.text(rl, x + 6, y + 16, Draw.alpha(0xFFFFFF, a), true);
			float ns = Math.min(2.2f, (w - 6) / (float) Math.max(1, g.textWidth(r.name())));
			Draw.extruded(g, r.name(), x + (int) (g.textWidth(r.name()) * ns / 2), y + 32, ns, Draw.alpha(r.rarity().glow, a), Draw.alpha(Draw.shade(r.rarity().color, .3f), a), true);
			g.text(r.description(), x, y + 34 + (int) (9 * ns) + 6, Draw.alpha(0xFFDDE4F0, a), false);
			g.text(r.type() + " · Slot: " + r.slot().label, x, y + 34 + (int) (9 * ns) + 18, Draw.alpha(0xFF9AA6BC, a), false);
			if (a >= 1) {
				boolean equipped = r.id().equals(state().equipped().get(r.slot()));
				int by = y + 96;
				buttons.add(new Btn(x, by, 100, 18, equipped ? "✔ Angelegt" : "Anlegen", !equipped, () -> game.equip(r.slot(), equipped ? "" : r.id())));
				buttons.add(new Btn(x + 106, by, 100, 18, "Zurück", false, () -> { view = View.CALENDAR; game.preview(null, 1); }));
			}
		}
		g.text("Ziehen zum Drehen", 14, H - 14, 0x80FFFFFF, false);
	}

	/* ---------------------------------------------------------------- collection view */
	private void collection(Gfx g, int W, int H) {
		Draw.extruded(g, "DEINE SAMMLUNG", W / 2, 8, 2f, 0xFFFFD36B, 0xFF5A3407, true);
		PlayerAdvent st = state();
		int pw = (int) (W * .34f);
		g.player(8, 30, pw, H - 30, (int) (H * .2f), spin + (Float.isNaN(dragFrom) ? now * 25 : 0), -.1f);
		game.preview(hoverPreview, 1); hoverPreview = null;
		int x0 = pw + 10, y = 32, rowH = Math.max(18, (H - 70) / 7);
		for (CosmeticSlot slot : CosmeticSlot.values()) {
			g.text(slot.label.toUpperCase(), x0, y + 5, 0xFFFFE7A8, true);
			int x = x0 + 70;
			for (Reward r : Rewards.ALL) {
				if (r.slot() != slot) continue;
				boolean owned = st.isClaimed(r.day()), eq = r.id().equals(st.equipped().get(slot));
				int bw = g.textWidth(r.name()) + 12;
				if (x + bw > W - 8) break;
				boolean hov = owned && mx >= x && my >= y && mx < x + bw && my < y + 16;
				if (hov) hoverPreview = r.id();
				int fillC = !owned ? 0xC0202430 : eq ? Draw.shade(r.rarity().color, .55f) : 0xD0101828;
				for (int i = 2; i >= 1; i--) g.fill(x + i, y + i, x + bw + i, y + 16 + i, 0xFF05070E);
				g.fill(x, y - (hov ? 1 : 0), x + bw, y + 16 - (hov ? 1 : 0), fillC);
				Draw.frame(g, x, y - (hov ? 1 : 0), x + bw, y + 16 - (hov ? 1 : 0), owned ? r.rarity().color : 0xFF3A3F4C);
				g.text(owned ? r.name() : "???", x + 6, y + 4 - (hov ? 1 : 0), owned ? (eq ? 0xFFFFFFFF : r.rarity().glow) : 0xFF6A7080, false);
				if (owned) { final int fx = x, fw = bw, fy = y; buttons.add(new Btn(fx, fy, fw, 16, "", false, () -> game.equip(slot, eq ? "" : r.id())).invisible()); }
				x += bw + 4;
			}
			y += rowH;
		}
		g.text("Klicken = anlegen / ablegen · Hover = Vorschau", x0, H - 22, 0x90FFFFFF, false);
		buttons.add(new Btn(W - 90, H - 24, 80, 18, "Zurück", false, () -> view = View.CALENDAR));
	}

	/* ---------------------------------------------------------------- UI particles */
	private void spark(float x, float y, float speed, int argb, int size) {
		float a = rnd.nextFloat() * (float) Math.PI * 2, v = (.3f + rnd.nextFloat()) * speed;
		particles.add(new float[] { x, y, (float) Math.cos(a) * v, (float) Math.sin(a) * v - speed * .6f, 0, .8f + rnd.nextFloat() * .8f, Float.intBitsToFloat(argb), size });
	}

	private void particles(Gfx g) {
		for (int i = particles.size() - 1; i >= 0; i--) {
			float[] p = particles.get(i);
			p[4] += 1 / 60f; if (p[4] > p[5]) { particles.remove(i); continue; }
			p[0] += p[2]; p[1] += p[3]; p[3] += .05f; p[2] *= .98f;
			int c = Float.floatToRawIntBits(p[6]), s = (int) p[7];
			g.fill((int) p[0], (int) p[1], (int) p[0] + s, (int) p[1] + s, Draw.alpha(c, 1 - p[4] / p[5]));
		}
		if (particles.size() > 600) particles.subList(0, particles.size() - 600).clear();
	}

	/* ---------------------------------------------------------------- helpers */
	static float clamp01(float x) { return Math.max(0, Math.min(1, x)); }
	static float easeOut(float t) { t = clamp01(t); return 1 - (1 - t) * (1 - t) * (1 - t); }

	static String span(long ms) {
		long s = Math.max(0, ms / 1000), d = s / 86400, h = (s % 86400) / 3600, m = (s % 3600) / 60;
		return d > 0 ? String.format("%dT %02d:%02d:%02d", d, h, m, s % 60) : String.format("%02d:%02d:%02d", h, m, s % 60);
	}

	/** 3D push button. */
	final class Btn {
		final int x, y, w, h; final String label; final boolean gold; final Runnable action; boolean visible = true;
		Btn(int x, int y, int w, int h, String label, boolean gold, Runnable action) { this.x = x; this.y = y; this.w = w; this.h = h; this.label = label; this.gold = gold; this.action = action; }
		Btn invisible() { visible = false; return this; }
		boolean hit(int px, int py) { return px >= x && py >= y && px < x + w && py < y + h; }
		void draw(Gfx g, boolean hov) {
			if (!visible) return;
			int off = hov ? 1 : 0;
			for (int i = 3; i >= 1; i--) g.fill(x + i - 1, y + i, x + w + i - 1, y + h + i, gold ? 0xFF8A5C12 : 0xFF05070E);
			if (gold) g.gradient(x, y - off, x + w, y + h - off, 0xFFFFF1C2, 0xFFE3B553); else g.gradient(x, y - off, x + w, y + h - off, hov ? 0xFF2A3450 : 0xFF1A2236, 0xFF0E1424);
			Draw.frame(g, x, y - off, x + w, y + h - off, gold ? 0xFFFFF6D8 : 0x80FFE7A8);
			g.text(label, x + (w - g.textWidth(label)) / 2, y + (h - 8) / 2 - off, gold ? 0xFF2A1505 : 0xFFFFFFFF, !gold);
		}
	}
}
