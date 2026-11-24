package com.maneszn.styles.render;

import com.maneszn.styles.config.ModConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class ItemScales {
   private ItemScales() {
   }

   public static String idOf(ItemStack stack) {
      return stack != null && !stack.isEmpty() ? BuiltInRegistries.ITEM.getKey(stack.getItem()).toString() : null;
   }

   public static double perItem(ItemStack stack) {
      String id = idOf(stack);
      if (id == null) {
         return 1.0;
      } else {
         Double d = ModConfig.get().itemScales.get(id);
         return d == null ? 1.0 : d;
      }
   }

   public static float held(ItemStack stack) {
      return (float)ModConfig.get().heldItemScale;
   }

   public static float dropped(ItemStack stack) {
      return (float)perItem(stack);
   }
}
