package com.maneszn.styles.mixin;

import com.maneszn.styles.compat.Hooks;
import com.maneszn.styles.name.NameEffects;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderer.class})
public abstract class EntityRendererMixin {
   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V"},
      at = {@At("RETURN")},
      require = 0
   )
   private void maneszn$nametagMotion(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
      if (entity instanceof AbstractClientPlayer player && state.nameTagAttachment != null) {
         Hooks.nametagMotion = true;
         double[] m = NameEffects.motion(player.getUUID());
         if (m != null) {
            state.nameTagAttachment = state.nameTagAttachment.add(m[0], m[1], 0.0);
         }
      }
   }
}
