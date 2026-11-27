package dev.maneszn.advent.test;

import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.tools.DropLock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** QA for the drop lock. Run with -Pmsdroplock. */
public final class DropLockTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext ctx) {
		if (System.getProperty("ms.droplock") == null) return;
		try (TestSingleplayerContext sp = ctx.worldBuilder().adjustSettings(c -> c.setAllowCommands(true)).create()) {
			sp.getClientWorld().waitForChunksRender();
			sp.getServer().runCommand("gamemode survival @a");
			sp.getServer().runCommand("time set noon");
			sp.getServer().runCommand("clear @a");
			sp.getServer().runOnServer(s -> {
				var p = s.getPlayerList().getPlayers().get(0);
				p.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
				p.getInventory().setItem(1, new ItemStack(Items.DIRT, 16));
				p.getInventory().setSelectedSlot(0);
			});
			ctx.waitTicks(10);
			ctx.runOnClient(mc -> { mc.player.getInventory().setSelectedSlot(0); ModConfig.get().lockedItems.clear(); DropLock.toggle(mc.player.getMainHandItem()); });
			ctx.waitTicks(5);

			// 1) Q in hand
			ctx.runOnClient(mc -> { mc.player.drop(false); mc.player.drop(true); });
			ctx.waitTicks(10);
			check(sp, "Q / Ctrl+Q in hand", Items.DIAMOND_SWORD, 0, 1, 0);

			// 2) inventory: Q over slot, pick up + click outside
			ctx.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
			ctx.waitTicks(10);
			ctx.takeScreenshot("droplock-inventory");
			ctx.runOnClient(mc -> {
				int id = mc.player.containerMenu.containerId;
				mc.gameMode.handleInventoryMouseClick(id, 36, 0, ClickType.THROW, mc.player);
				mc.gameMode.handleInventoryMouseClick(id, 36, 1, ClickType.THROW, mc.player);
				mc.gameMode.handleInventoryMouseClick(id, 36, 0, ClickType.PICKUP, mc.player);
				mc.gameMode.handleInventoryMouseClick(id, -999, 0, ClickType.PICKUP, mc.player);
				mc.gameMode.handleInventoryMouseClick(id, -999, 1, ClickType.PICKUP, mc.player);
				mc.gameMode.handleInventoryMouseClick(id, 36, 0, ClickType.PICKUP, mc.player);
			});
			ctx.waitTicks(10);
			check(sp, "inventory Q / outside click", Items.DIAMOND_SWORD, 0, 1, 0);

			// 3) control: unlocked dirt still drops
			ctx.runOnClient(mc -> mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 37, 0, ClickType.THROW, mc.player));
			ctx.setScreen(() -> null);
			ctx.waitTicks(10);
			check(sp, "unlocked dirt still drops one", Items.DIRT, 1, 15, 1);

			// 4) unlock → sword drops again
			ctx.runOnClient(mc -> { DropLock.toggle(mc.player.getMainHandItem()); mc.player.drop(false); });
			ctx.waitTicks(10);
			check(sp, "after unlocking, sword drops", Items.DIAMOND_SWORD, 0, 0, 1);
			ctx.takeScreenshot("droplock-hotbar");
			System.out.println("[DropLockTest] ALL OK");
		}
	}

	/** slot = where the item should be; expectCount in that slot; expectEntities = dropped item entities of that item. */
	private static void check(TestSingleplayerContext sp, String label, net.minecraft.world.item.Item item, int slot, int expectCount, int expectEntities) {
		AtomicInteger inSlot = new AtomicInteger(), entities = new AtomicInteger();
		AtomicReference<String> where = new AtomicReference<>("");
		sp.getServer().runOnServer(s -> {
			var p = s.getPlayerList().getPlayers().get(0);
			ItemStack st = p.getInventory().getItem(slot);
			inSlot.set(st.is(item) ? st.getCount() : 0);
			for (ItemEntity e : p.level().getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(16))) if (e.getItem().is(item)) entities.addAndGet(e.getItem().getCount());
			where.set(p.getInventory().getItem(slot).toString());
		});
		boolean ok = inSlot.get() == expectCount && entities.get() == expectEntities;
		System.out.println("[DropLockTest] " + (ok ? "PASS " : "FAIL ") + label + " slot=" + where.get() + " dropped=" + entities.get());
		if (!ok) throw new AssertionError(label);
	}
}
