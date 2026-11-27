package dev.maneszn.advent.test;

import com.maneszn.styles.config.ModConfig;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;

/** Close-up QA shots of the rebuilt v24 auras, wings and hip swords worn by the player. */
public final class CosmeticShowcaseTest implements FabricClientGameTest {
	private static final String[][] SETS = {
		{ "engel", "feuerkreis", "goldschwert" }, { "drache", "schnee", "diamantschwert" }, { "fee", "herzen", "katana_huefte" },
		{ "kristall", "runenkreis", "netheritschwert" }, { "phoenix", "flammen", "rubinschwert" }, { "owner_diamantschwingen", "owner_diamantkranz", "owner_klinge" },
		{ "schmetterling_blau", "blaetterwirbel", "holzschwert" }, { "cyber", "admin_bannkreis", "lila_klinge" },
	};

	@Override
	public void runTest(ClientGameTestContext ctx) {
		if (System.getProperty("ms.showcase") == null) return;
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			sp.getClientWorld().waitForChunksRender();
			sp.getServer().runCommand("time set noon");
			for (String[] set : SETS) {
				ctx.runOnClient(mc -> {
					var lo = ModConfig.get().cosmetics; lo.clear();
					lo.put("wings", set[0]); lo.put("aura", set[1]); lo.put("hip", set[2]);
				});
				for (CameraType cam : new CameraType[] { CameraType.THIRD_PERSON_BACK, CameraType.THIRD_PERSON_FRONT }) {
					ctx.runOnClient(mc -> mc.options.setCameraType(cam));
					ctx.waitTicks(20);
					ctx.takeScreenshot("show-" + set[0] + "-" + (cam == CameraType.THIRD_PERSON_BACK ? "back" : "front"));
				}
			}
			ctx.runOnClient(mc -> { ModConfig.get().cosmetics.clear(); mc.options.setCameraType(CameraType.FIRST_PERSON); });
		}
	}
}
