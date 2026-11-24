package dev.maneszn.advent.client;

import dev.maneszn.advent.core.CosmeticSlot;
import dev.maneszn.advent.core.PlayerAdvent;
import dev.maneszn.advent.net.Payloads;
import dev.maneszn.advent.ui.Bridge;
import dev.maneszn.advent.ui.CalendarUI;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public final class CalendarScreen extends Screen {
	static CalendarScreen current;
	public final CalendarUI ui;

	public CalendarScreen() {
		super(Component.literal("Christmas Advent Calendar"));
		ui = new CalendarUI(new Bridge() {
			public int currentDay() { return ClientAdvent.clock.currentDay(); }
			public long nowMs() { return ClientAdvent.serverNow(); }
			public long nextUnlockMs() { return ClientAdvent.clock.nextUnlockMs(); }
			public long christmasEveMs() { return ClientAdvent.clock.christmasEveMs(); }
			public String zone() { return ClientAdvent.clock.zone(); }
			public PlayerAdvent state() { return ClientAdvent.localState(); }
			public void claim(int day) { ClientPlayNetworking.send(new Payloads.Claim(day)); }
			public void equip(CosmeticSlot slot, String id) { ClientPlayNetworking.send(new Payloads.Equip(slot.ordinal(), id)); }
			public void preview(String id, float intro) { ClientAdvent.previewId = id; ClientAdvent.previewIntro = intro; }
			public void sound(String kind) {
				var sm = Minecraft.getInstance().getSoundManager();
				switch (kind) {
					case "click" -> sm.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
					case "locked" -> sm.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.6f));
					case "open" -> { sm.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL, 1.2f)); sm.play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f)); }
					case "mythic" -> { sm.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL, 0.8f)); sm.play(SimpleSoundInstance.forUI(SoundEvents.BEACON_ACTIVATE, 1.2f)); }
					case "reveal" -> sm.play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.4f));
					case "reveal_mythic" -> sm.play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f));
					default -> {}
				}
			}
			public void close() { onClose(); }
		});
	}

	@Override protected void init() { current = this; }
	@Override public void removed() { if (current == this) current = null; ClientAdvent.previewId = null; }
	@Override public boolean isPauseScreen() { return false; }
	@Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {}

	@Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		ui.mouseMoved(mouseX, mouseY);
		ui.render(new GuiGfx(g, font, width, height), a);
	}

	@Override public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) { return ui.mouseClicked(e.x(), e.y(), e.button()) || super.mouseClicked(e, doubleClick); }
	@Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) { ui.mouseDragged(e.x(), e.y()); return true; }
	@Override public boolean mouseReleased(MouseButtonEvent e) { ui.mouseReleased(); return super.mouseReleased(e); }
	@Override public boolean keyPressed(KeyEvent e) {
		if (e.key() == 256 && ui.back()) return true;
		return super.keyPressed(e);
	}
}
