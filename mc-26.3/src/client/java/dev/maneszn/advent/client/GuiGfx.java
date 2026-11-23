package dev.maneszn.advent.client;

import dev.maneszn.advent.mesh.Tex;
import dev.maneszn.advent.ui.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Gfx → 26.3 retained-mode GUI extractor. */
final class GuiGfx implements Gfx {
	private static final int ATLAS_SIZE = 2048, TILE = ATLAS_SIZE / Tex.GRID;
	private final GuiGraphicsExtractor g; private final Font font; private final int w, h;
	GuiGfx(GuiGraphicsExtractor g, Font font, int w, int h) { this.g = g; this.font = font; this.w = w; this.h = h; }

	@Override public int width() { return w; }
	@Override public int height() { return h; }
	@Override public void fill(int x0, int y0, int x1, int y1, int c) { if ((c >>> 24) != 0) g.fill(x0, y0, x1, y1, c); }
	@Override public void gradient(int x0, int y0, int x1, int y1, int a, int b) { g.fillGradient(x0, y0, x1, y1, a, b); }
	@Override public void tile(Tex t, int x, int y, int ww, int hh, float u0, float v0, float u1, float v1, int argb) {
		// tiles repeat by splitting into whole tiles + remainder
		float su = u1 - u0, sv = v1 - v0;
		int nx = (int) Math.ceil(su), ny = (int) Math.ceil(sv);
		for (int i = 0; i < nx; i++) for (int j = 0; j < ny; j++) {
			float fu = Math.min(1, su - i), fv = Math.min(1, sv - j);
			int px = x + (int) (ww * (i / su)), py = y + (int) (hh * (j / sv)), pw = (int) Math.ceil(ww * fu / su), ph = (int) Math.ceil(hh * fv / sv);
			float tu = (t.ordinal() % Tex.GRID) * TILE + 2, tv = (t.ordinal() / Tex.GRID) * TILE + 2;
			g.blit(RenderPipelines.GUI_TEXTURED, PoseSink.ATLAS, px, py, tu, tv, pw, ph, (int) ((TILE - 4) * fu), (int) ((TILE - 4) * fv), ATLAS_SIZE, ATLAS_SIZE, argb);
		}
	}
	@Override public void text(String s, int x, int y, int argb, boolean shadow) { if ((argb >>> 24) > 4) g.text(font, s, x, y, argb, shadow); }
	@Override public int textWidth(String s) { return font.width(s); }
	@Override public int lineHeight() { return font.lineHeight; }
	@Override public void push() { g.pose().pushMatrix(); }
	@Override public void pop() { g.pose().popMatrix(); }
	@Override public void translate(float x, float y) { g.pose().translate(x, y); }
	@Override public void scale(float s) { g.pose().scale(s, s); }
	@Override public void rotate(float rad) { g.pose().rotate(rad); }

	@Override public void player(int x0, int y0, int x1, int y1, int size, float yawDeg, float offsetY) {
		var mc = Minecraft.getInstance(); var p = mc.player; if (p == null) return;
		EntityRenderDispatcher d = mc.getEntityRenderDispatcher();
		EntityRenderState rs = d.getRenderer(p).createRenderState(p, 1.0F);
		rs.shadowPieces.clear(); rs.outlineColor = 0;
		if (rs instanceof LivingEntityRenderState l) { l.bodyRot = 180; l.yRot = 0; l.xRot = 0; l.boundingBoxWidth /= l.scale; l.boundingBoxHeight /= l.scale; l.scale = 1; }
		Quaternionf rot = new Quaternionf().rotateZ((float) Math.PI).rotateY((float) Math.toRadians(yawDeg));
		Quaternionf cam = new Quaternionf().rotateX((float) Math.toRadians(-8));
		rot.mul(cam);
		g.entity(rs, size, new Vector3f(0, rs.boundingBoxHeight / 2 + offsetY, 0), rot, cam, x0, y0, x1, y1);
	}
}
