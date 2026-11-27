package com.maneszn.styles.tools;

import com.mojang.blaze3d.platform.InputConstants.Type;
import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.gui.Ui;
import com.maneszn.styles.mixin.AbstractContainerScreenAccessor;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Drop-Schutz: ausgewählte Items (nach Item-Art, z. B. "Netherite-Schwert") lassen sich nicht droppen –
 * weder mit Q in der Hand, noch mit Q/Strg+Q im Inventar, noch durch Klicken außerhalb des Fensters,
 * noch im Kreativ-Inventar oder über den Papierkorb-Slot. Auswählen: Item im Inventar mit der Maus
 * anvisieren (oder in die Hand nehmen) und die Taste drücken (Standard: N).
 */
public final class DropLock {
   public static KeyMapping lockKey;
   private static long lastMsg;

   private DropLock() {
   }

   public static void register() {
      lockKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.maneszn_styles.drop_lock", Type.KEYSYM, 78, ManesznStyles.KEY_CATEGORY));
      ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
         if (screen instanceof AbstractContainerScreen<?> acs) {
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, key) -> {
               if (!lockKey.matches(key)) {
                  return true;
               }

               Slot slot = ((AbstractContainerScreenAccessor)acs).maneszn$hoveredSlot();
               if (slot != null && slot.hasItem()) {
                  toggle(slot.getItem());
               } else if (!acs.getMenu().getCarried().isEmpty()) {
                  toggle(acs.getMenu().getCarried());
               }

               return false;
            });
            ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) -> renderSlots(acs, g));
         }
      });
      HudElementRegistry.addLast(ManesznStyles.id("drop_lock"), (g, delta) -> renderHotbar(g));
   }

   public static String id(ItemStack st) {
      return BuiltInRegistries.ITEM.getKey(st.getItem()).toString();
   }

   public static boolean isLocked(ItemStack st) {
      ModConfig cfg = ModConfig.get();
      return cfg.dropLock && st != null && !st.isEmpty() && !cfg.lockedItems.isEmpty() && cfg.lockedItems.contains(id(st));
   }

   public static void toggle(ItemStack st) {
      if (st == null || st.isEmpty()) {
         return;
      }

      ModConfig cfg = ModConfig.get();
      String id = id(st);
      boolean nowLocked = !cfg.lockedItems.contains(id);
      if (nowLocked) {
         cfg.lockedItems.add(id);
      } else {
         cfg.lockedItems.remove(id);
      }

      if (nowLocked && !cfg.dropLock) {
         cfg.dropLock = true;
      }

      ModConfig.save();
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         String name = st.getItem().getName(st).getString();
         mc.player.displayClientMessage(
            Component.literal(nowLocked ? "§6🔒 " + Ui.tr("maneszn_styles.droplock.locked", name) : "§7🔓 " + Ui.tr("maneszn_styles.droplock.unlocked", name)), true
         );
         mc.player.playSound(nowLocked ? SoundEvents.CHAIN_PLACE : SoundEvents.CHAIN_BREAK, 0.6F, 1.4F);
      }
   }

   /** Prüft einen Drop-Versuch. true = verhindern (und kurz Bescheid sagen). */
   public static boolean block(ItemStack st) {
      if (!isLocked(st)) {
         return false;
      }

      Minecraft mc = Minecraft.getInstance();
      long now = System.currentTimeMillis();
      if (mc.player != null && now - lastMsg > 1200L) {
         lastMsg = now;
         String key = lockKey == null ? "N" : lockKey.getTranslatedKeyMessage().getString();
         mc.player.displayClientMessage(Component.literal("§c🔒 " + Ui.tr("maneszn_styles.droplock.blocked", st.getItem().getName(st).getString(), key)), true);
         mc.player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.7F, 0.6F);
      }

      return true;
   }

   /** Taste in der Welt: Item in der Hand sperren/entsperren. */
   public static void tick(Minecraft mc) {
      if (lockKey == null) {
         return;
      }

      while (lockKey.consumeClick()) {
         if (mc.screen == null && mc.player != null) {
            ItemStack st = mc.player.getMainHandItem();
            if (st.isEmpty()) {
               st = mc.player.getOffhandItem();
            }

            toggle(st);
         }
      }
   }

   private static void renderSlots(AbstractContainerScreen<?> acs, GuiGraphics g) {
      if (!ModConfig.get().dropLock || ModConfig.get().lockedItems.isEmpty()) {
         return;
      }

      AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor)acs;
      for (Slot slot : acs.getMenu().slots) {
         if (slot.isActive() && isLocked(slot.getItem())) {
            lockIcon(g, a.maneszn$leftPos() + slot.x + 11, a.maneszn$topPos() + slot.y - 1);
         }
      }
   }

   private static void renderHotbar(GuiGraphics g) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.options.hideGui || mc.player.isSpectator() || !ModConfig.get().dropLock || ModConfig.get().lockedItems.isEmpty()) {
         return;
      }

      int w = g.guiWidth();
      int h = g.guiHeight();
      for (int i = 0; i < 9; i++) {
         if (isLocked(mc.player.getInventory().getItem(i))) {
            lockIcon(g, w / 2 - 90 + i * 20 + 2 + 11, h - 16 - 3 - 1);
         }
      }
   }

   /** Kleines Pixel-Schloss (5×6), oben rechts am Slot. */
   private static void lockIcon(GuiGraphics g, int x, int y) {
      int gold = 0xFFFFC83D;
      int dark = 0xFF3A2A06;
      g.fill(x, y + 2, x + 6, y + 7, dark);
      g.fill(x + 1, y, x + 5, y + 1, dark);
      g.fill(x, y + 1, x + 1, y + 3, dark);
      g.fill(x + 5, y + 1, x + 6, y + 3, dark);
      g.fill(x + 1, y + 1, x + 2, y + 3, 0xFFE8E8E8);
      g.fill(x + 4, y + 1, x + 5, y + 3, 0xFFE8E8E8);
      g.fill(x + 2, y + 1, x + 4, y + 2, 0xFFE8E8E8);
      g.fill(x + 1, y + 3, x + 5, y + 6, gold);
      g.fill(x + 3, y + 4, x + 4, y + 5, dark);
   }
}
