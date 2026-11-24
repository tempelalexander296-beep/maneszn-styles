package com.maneszn.styles.mixin;

import com.maneszn.styles.view.Zoom;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MouseHandler.class})
public abstract class MouseHandlerMixin {
   @Inject(
      method = {"onScroll"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void maneszn$zoomScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
      if (yOffset != 0.0 && Zoom.scroll(yOffset)) {
         ci.cancel();
      }
   }
}
