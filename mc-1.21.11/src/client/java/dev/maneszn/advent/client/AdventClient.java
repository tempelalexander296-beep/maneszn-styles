package dev.maneszn.advent.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.maneszn.advent.AdventMod;
import dev.maneszn.advent.net.Payloads;
import dev.maneszn.advent.render.CosmeticModel;
import dev.maneszn.advent.render.Fx;
import dev.maneszn.advent.render.Wearer;
import dev.maneszn.advent.render.cosmetics.Cosmetics;
import java.util.Random;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;

public final class AdventClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(AdventMod.id("advent"));
	private static KeyMapping OPEN;
	private final Random rnd = new Random();
	private final Wearer fxWearer = new Wearer();

	@Override
	public void onInitializeClient() {
		OPEN = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.adventcosmetics.open", InputConstants.Type.KEYSYM, InputConstants.KEY_K, CATEGORY));
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
			if (renderer instanceof AvatarRenderer<?> avatar) helper.register(new CosmeticLayer(avatar));
		});
		ClientPlayNetworking.registerGlobalReceiver(Payloads.Clock.TYPE, (p, ctx) -> {
			ClientAdvent.clock = p; ClientAdvent.clockReceivedAt = System.currentTimeMillis();
			if (p.open()) ctx.client().setScreen(new CalendarScreen());
		});
		ClientPlayNetworking.registerGlobalReceiver(Payloads.ClaimResult.TYPE, (p, ctx) -> { if (CalendarScreen.current != null) CalendarScreen.current.ui.onClaimResult(p.day(), p.result()); });
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
	}

	private void tick(Minecraft mc) {
		while (OPEN.consumeClick()) if (mc.player != null) mc.setScreen(new CalendarScreen());
		if (mc.level == null || mc.player == null || mc.isPaused()) return;
		for (Player p : mc.level.players()) {
			if (p.isInvisible() || p.isSpectator() || p.distanceToSqr(mc.player) > 64 * 64) continue;
			var eq = ClientAdvent.equippedOf(p);
			if (eq.isEmpty()) continue;
			double yaw = Math.toRadians(p.yBodyRot);
			double lx = Math.cos(yaw), lz = Math.sin(yaw), fx = -Math.sin(yaw), fz = Math.cos(yaw);
			fxWearer.time = p.tickCount / 20f; fxWearer.walkSpeed = (float) Math.min(1, p.getDeltaMovement().horizontalDistance() * 5); fxWearer.preview = false;
			Fx fxs = (t, x, y, z, vx, vy, vz) -> mc.level.addParticle(particle(t),
				p.getX() + lx * x + fx * z, p.getY() + y, p.getZ() + lz * x + fz * z,
				lx * vx + fx * vz, vy, lz * vx + fz * vz);
			for (String id : eq.values()) { CosmeticModel m = Cosmetics.get(id); if (m != null) m.tickFx(fxs, fxWearer, rnd); }
		}
	}

	private static ParticleOptions particle(Fx.Type t) {
		return switch (t) {
			case SNOW -> ParticleTypes.SNOWFLAKE;
			case SPARKLE, END_ROD -> ParticleTypes.END_ROD;
			case GOLD -> new DustParticleOptions(0xFFD36B, 0.8f);
			case RED -> new DustParticleOptions(0xFF2B47, 0.9f);
			case GREEN -> new DustParticleOptions(0x24D86A, 0.9f);
			case ICE -> new DustParticleOptions(0x8FDCFF, 0.8f);
			case WHITE -> new DustParticleOptions(0xFFFFFF, 0.8f);
			case GLOW -> ParticleTypes.GLOW;
			case FIREWORK -> ParticleTypes.FIREWORK;
			case ELECTRIC -> ParticleTypes.ELECTRIC_SPARK;
			case CLOUD -> ParticleTypes.WHITE_ASH;
		};
	}
}
