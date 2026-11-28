package dev.maneszn.advent.test;

import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.pack.StylePackBuilder;
import com.maneszn.styles.style.Category;
import com.maneszn.styles.style.Style;
import com.maneszn.styles.style.StyleRegistry;
import com.maneszn.styles.style.Target;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** QA: skies from resource packs show up under "Himmel" and render. Run with -Pmssky (test packs in run/resourcepacks). */
public final class SkyPackTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext ctx) {
		if (System.getProperty("ms.sky") == null) return;
		ctx.runOnClient(mc -> {
			try {
				java.nio.file.Path src = java.nio.file.Path.of(System.getProperty("ms.sky"));
				java.nio.file.Path dst = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
				try (var walk = java.nio.file.Files.walk(src)) {
					for (java.nio.file.Path f : walk.toList()) {
						java.nio.file.Path to = dst.resolve(src.relativize(f).toString());
						if (java.nio.file.Files.isDirectory(f)) java.nio.file.Files.createDirectories(to);
						else java.nio.file.Files.copy(f, to, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					}
				}
			} catch (java.io.IOException e) { throw new RuntimeException(e); }
			ModConfig.get().scanResourcePacks = true;
			StyleRegistry.get().reload();
			List<Style> skies = StyleRegistry.get().styles("sky");
			Category cat = StyleRegistry.get().category("sky");
			Target t = cat.targets().get(0);
			StringBuilder sb = new StringBuilder();
			for (Style s : skies) if (s.source() == Style.Source.PACK) sb.append(s.id()).append(" -> ").append(s.resolve(t).map(Object::toString).orElse("-")).append("; ");
			System.out.println("[SkyPackTest] pack skies: " + sb);
			boolean a = sb.indexOf("pack_sunset_sky_pack -> /assets/minecraft/optifine/sky/world0/sky_main.png") >= 0;
			boolean b = sb.indexOf("pack_fsb_skybox ->") >= 0 && sb.indexOf("box.png") >= 0;
			boolean c = sb.indexOf("pack_no_sky") < 0;
			System.out.println("[SkyPackTest] " + (a ? "PASS" : "FAIL") + " optifine main layer (not stars overlay)");
			System.out.println("[SkyPackTest] " + (b ? "PASS" : "FAIL") + " fabricskyboxes single sprite");
			System.out.println("[SkyPackTest] " + (c ? "PASS" : "FAIL") + " pack without 3x2 sky ignored");
			if (!(a && b && c)) throw new AssertionError("sky detection");
			ModConfig.get().active.put("sky", "pack_sunset_sky_pack");
			StylePackBuilder.applyAndReload();
		});
		ctx.waitTicks(100);
		try (TestSingleplayerContext sp = ctx.worldBuilder().create()) {
			sp.getClientWorld().waitForChunksRender();
			sp.getServer().runCommand("time set noon");
			sp.getServer().runOnServer(s -> { var p = s.getPlayerList().getPlayers().get(0); p.snapTo(p.getX(), p.getY() + 20, p.getZ(), 30, -35); });
			ctx.waitTicks(40);
			ctx.takeScreenshot("sky-pack-world");
			ctx.setScreen(() -> com.maneszn.styles.gui.StylesScreen.withCategory(null, "sky"));
			ctx.waitTicks(40);
			ctx.takeScreenshot("sky-pack-menu");
			ctx.setScreen(() -> null);
			System.out.println("[SkyPackTest] ALL OK");
		}
	}
}
