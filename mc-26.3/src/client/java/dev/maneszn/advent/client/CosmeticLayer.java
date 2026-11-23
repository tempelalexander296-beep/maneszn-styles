package dev.maneszn.advent.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.maneszn.advent.core.CosmeticSlot;
import dev.maneszn.advent.render.CosmeticModel;
import dev.maneszn.advent.render.Wearer;
import dev.maneszn.advent.render.cosmetics.Cosmetics;
import dev.maneszn.advent.core.Rewards;
import java.util.EnumMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;

/** Renders all equipped advent cosmetics on a player (works in world + GUI previews). */
public final class CosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private final Wearer wearer = new Wearer();
	private static boolean failedOnce;

	public CosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) { super(parent); }

	@Override
	public void submit(PoseStack ps, SubmitNodeCollector out, int light, AvatarRenderState state, float yRot, float xRot) {
		if (state.isInvisible || state.isSpectator) return;
		try {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null) return;
			Entity e = mc.level.getEntity(state.id);
			EnumMap<CosmeticSlot, String> eq = ClientAdvent.equippedOf(e);
			boolean isLocal = mc.player != null && state.id == mc.player.getId();
			boolean preview = isLocal && mc.gui.screen() instanceof CalendarScreen;
			float intro = 1;
			if (preview && ClientAdvent.previewId != null) {
				var r = Rewards.byId(ClientAdvent.previewId);
				if (r != null) { eq.put(r.slot(), r.id()); intro = ClientAdvent.previewIntro; }
			}
			if (eq.isEmpty()) return;
			wearer.time = state.ageInTicks / 20f; wearer.walkSpeed = Math.min(1, state.walkAnimationSpeed); wearer.preview = preview;
			PoseSink sink = new PoseSink(ps, out, light);
			PlayerModel model = getParentModel();
			for (var en : eq.entrySet()) {
				CosmeticModel m = Cosmetics.get(en.getValue());
				if (m == null) continue;
				boolean isPreviewed = preview && en.getValue().equals(ClientAdvent.previewId);
				wearer.intro = isPreviewed ? intro : 1;
				ps.pushPose();
				mount(ps, model, en.getKey(), isPreviewed ? intro : 1);
				m.render(sink, wearer);
				ps.popPose();
			}
		} catch (Throwable t) {
			if (!failedOnce) { failedOnce = true; dev.maneszn.advent.AdventMod.LOG.error("Cosmetic render failed (further errors suppressed)", t); }
		}
	}

	/** Moves from model space (y down, facing -z) into the cosmetic's mount space (y up, facing +z). */
	private static void mount(PoseStack ps, PlayerModel model, CosmeticSlot slot, float intro) {
		switch (slot) {
			case HEAD -> { model.head.translateAndRotate(ps); ps.scale(1, -1, -1); ps.translate(0, 0.25f, 0); ps.scale(2f, 2f, 2f); }
			case BACK -> { model.body.translateAndRotate(ps); ps.scale(1, -1, -1); ps.translate(0, -0.17f, -0.15f); }
			case HAND -> { model.rightArm.translateAndRotate(ps); ps.scale(1, -1, -1); ps.translate(-0.06f, -0.62f, 0.02f); ps.rotateDegrees(Axis.XP, 80); ps.scale(0.9f, 0.9f, 0.9f); }
			default -> { ps.translate(0, 1.501f, 0); ps.scale(1, -1, -1); }
		}
		if (intro < 1) {
			float k = Math.max(0.001f, intro);
			ps.translate(0, (1 - k) * -0.4f, 0);
			ps.rotate(Axis.YP, (1 - k) * 6f);
			ps.scale(k, k, k);
		}
	}
}
