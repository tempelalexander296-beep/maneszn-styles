package dev.maneszn.advent.client;

import dev.maneszn.advent.AdventData;
import dev.maneszn.advent.AdventMod;
import dev.maneszn.advent.core.CosmeticSlot;
import dev.maneszn.advent.core.PlayerAdvent;
import dev.maneszn.advent.net.Payloads;
import java.util.EnumMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Client-side mirror of the server clock + the GUI preview override. */
public final class ClientAdvent {
	private ClientAdvent() {}

	public static Payloads.Clock clock = new Payloads.Clock(0, System.currentTimeMillis(), -1, 0, "UTC", false);
	public static long clockReceivedAt = System.currentTimeMillis();
	public static String previewId; public static float previewIntro = 1;
	/** true while another menu (Maneszn Styles → Cosmetics → Adventskalender) shows the preview on the local player. */
	public static boolean externalPreview;

	public static long serverNow() { return clock.serverNowMs() + (System.currentTimeMillis() - clockReceivedAt); }

	public static EnumMap<CosmeticSlot, String> equippedOf(Entity e) {
		if (!(e instanceof Player p)) return new EnumMap<>(CosmeticSlot.class);
		AdventData d = p.getAttachedOrElse(AdventMod.DATA, AdventData.EMPTY);
		return PlayerAdvent.parseEquipped(d.equipped());
	}

	public static PlayerAdvent localState() {
		Player p = Minecraft.getInstance().player;
		return (p == null ? AdventData.EMPTY : p.getAttachedOrElse(AdventMod.DATA, AdventData.EMPTY)).toLogic();
	}
}
