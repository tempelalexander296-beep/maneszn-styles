package dev.maneszn.advent.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.maneszn.advent.AdventMod;
import dev.maneszn.advent.mesh.Mesh;
import dev.maneszn.advent.mesh.Tex;
import dev.maneszn.advent.render.Layer;
import dev.maneszn.advent.render.Sink;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** Sink → 26.3 submit pipeline (one custom-geometry node per mesh). */
final class PoseSink implements Sink {
	static final Identifier ATLAS = AdventMod.id("textures/cosmetic/atlas.png");
	private static final RenderType SOLID = RenderTypes.entityCutout(ATLAS), TRANSLUCENT = RenderTypes.entityTranslucent(ATLAS), GLOW = RenderTypes.entityTranslucentEmissive(ATLAS);
	private static final int FULL_BRIGHT = 0xF000F0;

	final PoseStack ps; final SubmitNodeCollector out; final int light;
	PoseSink(PoseStack ps, SubmitNodeCollector out, int light) { this.ps = ps; this.out = out; this.light = light; }

	@Override public void push() { ps.pushPose(); }
	@Override public void pop() { ps.popPose(); }
	@Override public void translate(float x, float y, float z) { ps.translate(x, y, z); }
	@Override public void rotateX(float r) { ps.rotate(Axis.XP, r); }
	@Override public void rotateY(float r) { ps.rotate(Axis.YP, r); }
	@Override public void rotateZ(float r) { ps.rotate(Axis.ZP, r); }
	@Override public void scale(float x, float y, float z) { ps.scale(x, y, z); }

	@Override public void mesh(Mesh m, Tex tex, Layer layer, int argb) {
		RenderType rt = layer == Layer.SOLID ? SOLID : layer == Layer.TRANSLUCENT ? TRANSLUCENT : GLOW;
		int li = layer == Layer.GLOW ? FULL_BRIGHT : light;
		final float[] d = m.data; final int verts = m.quads * 4;
		out.submitCustomGeometry(ps, rt, (pose, vc) -> {
			for (int i = 0; i < verts; i++) {
				int o = i * Mesh.STRIDE;
				vc.addVertex(pose, d[o], d[o + 1], d[o + 2]).setColor(argb).setUv(tex.u(d[o + 6]), tex.v(d[o + 7]))
					.setOverlay(OverlayTexture.NO_OVERLAY).setLight(li).setNormal(pose, d[o + 3], d[o + 4], d[o + 5]);
			}
		});
	}
}
