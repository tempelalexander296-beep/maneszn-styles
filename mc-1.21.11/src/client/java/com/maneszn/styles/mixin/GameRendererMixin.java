package com.maneszn.styles.mixin;

import com.maneszn.styles.view.Zoom;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GameRenderer.class})
public abstract class GameRendererMixin {
   @Inject(
      method = {"getFov"},
      at = {@At("RETURN")},
      cancellable = true,
      require = 0
   )
   private void maneszn$zoom(CallbackInfoReturnable<Float> cir) {
      cir.setReturnValue(Zoom.apply((Float)cir.getReturnValue()));
   }
}
