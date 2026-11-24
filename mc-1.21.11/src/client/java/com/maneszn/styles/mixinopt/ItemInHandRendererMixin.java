package com.maneszn.styles.mixinopt;

import com.llamalad7.mixinextras.sugar.Local;
import com.maneszn.styles.compat.Hooks;
import com.maneszn.styles.render.ItemScales;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemInHandRenderer.class})
public abstract class ItemInHandRendererMixin {
   @Inject(
      method = {"renderItem"},
      at = {@At("HEAD")},
      require = 0
   )
   private void maneszn$heldScale(CallbackInfo ci, @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) PoseStack poseStack) {
      float s = ItemScales.held(stack);
      if (Math.abs(s - 1.0F) > 0.001F) {
         Hooks.itemScale = true;
         poseStack.scale(s, s, s);
      }
   }
}
