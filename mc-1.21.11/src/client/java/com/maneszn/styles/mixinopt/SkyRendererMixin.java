package com.maneszn.styles.mixinopt;

import com.maneszn.styles.render.CustomSky;
import net.minecraft.client.renderer.SkyRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SkyRenderer.class})
public abstract class SkyRendererMixin {
   @Inject(
      method = {"renderSkyDisc"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void maneszn$sky(int skyColor, CallbackInfo ci) {
      if (CustomSky.active()) {
         try {
            CustomSky.render(skyColor);
            ci.cancel();
         } catch (Throwable var4) {
         }
      }
   }

   @Inject(
      method = {"renderSunriseAndSunset"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void maneszn$noSunrise(CallbackInfo ci) {
      if (CustomSky.active()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderDarkDisc"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void maneszn$noDarkDisc(CallbackInfo ci) {
      if (CustomSky.active()) {
         ci.cancel();
      }
   }
}
