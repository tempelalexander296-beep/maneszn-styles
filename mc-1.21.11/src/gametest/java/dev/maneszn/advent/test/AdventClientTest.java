package dev.maneszn.advent.test;

import dev.maneszn.advent.AdventData;
import dev.maneszn.advent.AdventMod;
import dev.maneszn.advent.client.CalendarScreen;
import dev.maneszn.advent.core.CosmeticSlot;
import dev.maneszn.advent.core.Reward;
import dev.maneszn.advent.core.Rewards;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerPlayer;

/** Automated in-client QA: wears every cosmetic once and screenshots it, then the calendar GUI. */
public final class AdventClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext ctx) {
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			sp.getClientWorld().waitForChunksRender();
			sp.getServer().runCommand("time set noon");
			sp.getServer().runCommand("advent debug day 12");
			ctx.setScreen(() -> new com.maneszn.styles.gui.StylesScreen(null));
			ctx.waitTicks(60);
			ctx.takeScreenshot("styles-main");
			ctx.setScreen(() -> null);
			for (Reward r : Rewards.ALL) {
				if (System.getProperty("advent.allCosmetics") == null && r.day() % 6 != 0) continue;
				String eq = r.slot().name() + "=" + r.id();
				sp.getServer().runOnServer(server -> {
					ServerPlayer p = server.getPlayerList().getPlayers().get(0);
					p.setAttached(AdventMod.DATA, new AdventData(0x1FFFFFE, eq));
					p.snapTo(p.getX(), p.getY(), p.getZ(), 0, 0);
				});
				boolean back = r.slot() == CosmeticSlot.BACK;
				ctx.runOnClient(mc -> mc.options.setCameraType(back ? CameraType.THIRD_PERSON_BACK : CameraType.THIRD_PERSON_FRONT));
				ctx.waitTicks(25);
				ctx.takeScreenshot(String.format("cosmetic-%02d-%s", r.day(), r.id()));
			}
			ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			sp.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).setAttached(AdventMod.DATA, new AdventData(0b1110111011100, "HEAD=santa_hat")));
			ctx.setScreen(CalendarScreen::new);
			ctx.waitTicks(30);
			ctx.takeScreenshot("gui-calendar");
			ctx.runOnClient(mc -> ((CalendarScreen) mc.screen).ui.showReveal(24));
			ctx.waitTicks(80);
			ctx.takeScreenshot("gui-reveal-24");
			ctx.setScreen(() -> null);
		}
	}
}
