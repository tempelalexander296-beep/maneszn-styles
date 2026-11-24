package com.maneszn.styles.mixinopt;

import com.maneszn.styles.compat.Hooks;
import com.maneszn.styles.config.ModConfig;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({LevelRenderer.class})
public abstract class LevelRendererMixin {
   @ModifyVariable(
      method = {"renderHitOutline"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0,
      require = 0
   )
   private int maneszn$outlineColor(int color) {
      int c = ModConfig.get().blockOutlineColor;
      if (c == 0) {
         return color;
      } else {
         Hooks.outline = true;
         return c;
      }
   }
}
