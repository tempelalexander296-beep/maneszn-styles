package com.maneszn.styles.mixinopt;

import com.maneszn.styles.compat.Hooks;
import com.maneszn.styles.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({LightTexture.class})
public abstract class LightTextureMixin {
   @Redirect(
      method = {"updateLightTexture"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"
      ),
      require = 0
   )
   private Object maneszn$gamma(OptionInstance<?> option) {
      Object value = option.get();
      if (ModConfig.get().fullbright && option == Minecraft.getInstance().options.gamma()) {
         Hooks.fullbright = true;
         return 15.0;
      } else {
         return value;
      }
   }
}
