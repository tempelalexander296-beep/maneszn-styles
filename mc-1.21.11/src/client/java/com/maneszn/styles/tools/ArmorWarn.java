package com.maneszn.styles.tools;

import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.gui.Ui;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class ArmorWarn {
   private static final EquipmentSlot[] SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static final Map<EquipmentSlot, Boolean> warned = new EnumMap<>(EquipmentSlot.class);
   private static String text;
   private static long until;

   private ArmorWarn() {
   }

   public static void tick(Minecraft mc) {
      ModConfig cfg = ModConfig.get();
      if (cfg.armorWarn && mc.player != null) {
         for (EquipmentSlot s : SLOTS) {
            ItemStack st = mc.player.getItemBySlot(s);
            if (!st.isEmpty() && st.isDamageableItem() && st.getMaxDamage() > 0) {
               int left = st.getMaxDamage() - st.getDamageValue();
               int pct = left * 100 / st.getMaxDamage();
               boolean low = pct <= cfg.armorWarnPercent;
               if (low && !Boolean.TRUE.equals(warned.get(s))) {
                  warned.put(s, true);
                  text = Ui.tr("maneszn_styles.armorwarn.msg", st.getHoverName().getString(), left);
                  until = System.currentTimeMillis() + 3500L;
                  mc.player.playSound(SoundEvents.ITEM_BREAK.value(), 0.8F, 0.7F);
               } else if (!low) {
                  warned.put(s, false);
               }
            } else {
               warned.remove(s);
            }
         }
      }
   }

   public static void render(GuiGraphics g) {
      if (text != null && System.currentTimeMillis() <= until) {
         Minecraft mc = Minecraft.getInstance();
         if (!mc.options.hideGui) {
            boolean blink = System.currentTimeMillis() / 250L % 2L == 0L;
            int w = g.guiWidth();
            int y = g.guiHeight() / 2 - 40;
            g.pose().pushMatrix();
            g.pose().translate(w / 2.0F, y);
            g.pose().scale(1.6F, 1.6F);
            g.drawCenteredString(mc.font, "⚠ " + text, 0, 0, blink ? -48060 : -22016);
            g.pose().popMatrix();
         }
      }
   }
}
