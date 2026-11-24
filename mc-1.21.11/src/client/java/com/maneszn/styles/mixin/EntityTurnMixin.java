package com.maneszn.styles.mixin;

import com.maneszn.styles.view.Freelook;
import com.maneszn.styles.view.Zoom;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class EntityTurnMixin {
   @Inject(
      method = {"turn"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void maneszn$freelook(double yRot, double xRot, CallbackInfo ci) {
      if (Freelook.active() && (Object)this == Minecraft.getInstance().player) {
         Freelook.turn(yRot, xRot);
         ci.cancel();
      }
   }

   @ModifyVariable(
      method = {"turn"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0,
      require = 0
   )
   private double maneszn$zoomYaw(double v) {
      return (Object)this == Minecraft.getInstance().player ? v * Zoom.sensitivity() : v;
   }

   @ModifyVariable(
      method = {"turn"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 1,
      require = 0
   )
   private double maneszn$zoomPitch(double v) {
      return (Object)this == Minecraft.getInstance().player ? v * Zoom.sensitivity() : v;
   }
}
