package dev.maneszn.advent.test;

import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.gui.CodeScreen;
import dev.maneszn.advent.AdventData;
import dev.maneszn.advent.AdventMod;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** QA for the admin tools: /advent give|take and the rank/advent section of the code screen. Run with -Pmsadmin. */
public final class AdminToolsTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext ctx) {
		if (System.getProperty("ms.admin") == null) return;
		try (TestSingleplayerContext sp = ctx.worldBuilder().adjustSettings(c -> c.setAllowCommands(true)).create()) {
			sp.getClientWorld().waitForChunksRender();
			AtomicReference<String> name = new AtomicReference<>();
			sp.getServer().runOnServer(s -> name.set(s.getPlayerList().getPlayers().get(0).getName().getString()));

			sp.getServer().runCommand("advent give " + name.get());
			expect(sp, 0x1FFFFFE, "ultimate_christmas_aura", true, "give all");
			sp.getServer().runCommand("advent take " + name.get() + " 24");
			expect(sp, 0x1FFFFFE & ~(1 << 24), "ultimate_christmas_aura", false, "take day 24");
			sp.getServer().runCommand("advent give " + name.get() + " 6");
			sp.getServer().runCommand("advent take " + name.get());
			expect(sp, 0, "frozen_crown", false, "take all");

			// the same through the code screen button path (client sends the command; needs OP like in a real world)
			ctx.runOnClient(mc -> mc.getConnection().sendCommand("advent give " + name.get()));
			ctx.waitTicks(20);
			expect(sp, 0x1FFFFFE, "ultimate_christmas_aura", true, "client command give");

			sp.getServer().runCommand("advent take " + name.get() + " 1");
			ctx.setScreen(() -> com.maneszn.styles.gui.StylesScreen.withAdvent(null));
			ctx.waitTicks(40);
			ctx.takeScreenshot("advent-in-cosmetics");
			ctx.setScreen(() -> null);

			ctx.runOnClient(mc -> ModConfig.get().entCache = new java.util.ArrayList<>(List.of("owner", "admin", "legend", "vip")));
			ctx.setScreen(() -> new CodeScreen(null));
			ctx.waitTicks(20);
			ctx.takeScreenshot("admin-codescreen");
			ctx.setScreen(() -> null);
			System.out.println("[AdminToolsTest] ALL OK");
		}
	}

	private static void expect(TestSingleplayerContext sp, int mask, String id, boolean equipped, String label) {
		sp.getServer().runOnServer(s -> {
			AdventData d = s.getPlayerList().getPlayers().get(0).getAttachedOrElse(AdventMod.DATA, AdventData.EMPTY);
			boolean eq = d.equipped().contains(id);
			if (d.claimedMask() != mask || eq != equipped)
				throw new AssertionError(label + ": mask=" + Integer.toHexString(d.claimedMask()) + " equipped=" + d.equipped());
			System.out.println("[AdminToolsTest] PASS " + label + " mask=" + Integer.toHexString(d.claimedMask()) + " equipped=" + d.equipped());
		});
	}
}
