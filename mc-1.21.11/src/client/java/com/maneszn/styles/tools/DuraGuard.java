package com.maneszn.styles.tools;

import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.gui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

public final class DuraGuard {
   private static long lastMsg;

   private DuraGuard() {
   }

   public static boolean blockMining() {
      ModConfig cfg = ModConfig.get();
      Minecraft mc = Minecraft.getInstance();
      if (cfg.duraGuard && mc.player != null && !mc.player.isCreative()) {
         ItemStack st = mc.player.getMainHandItem();
         if (!st.isEmpty() && st.isDamageableItem() && st.getMaxDamage() > 0) {
            boolean tool = st.is(ItemTags.PICKAXES) || cfg.duraGuardAllTools && (st.is(ItemTags.AXES) || st.is(ItemTags.SHOVELS) || st.is(ItemTags.HOES));
            if (!tool) {
               return false;
            } else {
               int left = st.getMaxDamage() - st.getDamageValue();
               if (left * 100 > cfg.duraGuardPercent * st.getMaxDamage()) {
                  return false;
               } else {
                  long now = System.currentTimeMillis();
                  if (now - lastMsg > 1500L) {
                     lastMsg = now;
                     mc.player.displayClientMessage(Component.literal("§c⛏ " + Ui.tr("maneszn_styles.duraguard.blocked", left)), true);
                  }

                  return true;
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}
