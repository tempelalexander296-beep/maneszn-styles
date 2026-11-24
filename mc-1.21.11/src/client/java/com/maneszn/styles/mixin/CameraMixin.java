package com.maneszn.styles.mixin;

import com.maneszn.styles.view.Freelook;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({Camera.class})
public abstract class CameraMixin {
   @Redirect(
      method = {"setup"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"
      ),
      require = 0
   )
   private float maneszn$yaw(Entity entity, float partialTick) {
      return Freelook.active() ? Freelook.yaw() : entity.getViewYRot(partialTick);
   }

   @Redirect(
      method = {"setup"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"
      ),
      require = 0
   )
   private float maneszn$pitch(Entity entity, float partialTick) {
      return Freelook.active() ? Freelook.pitch() : entity.getViewXRot(partialTick);
   }
}
