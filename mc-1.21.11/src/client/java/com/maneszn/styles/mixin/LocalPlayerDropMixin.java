package com.maneszn.styles.mixin;

import com.maneszn.styles.tools.DropLock;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Drop-Schutz: Q / Strg+Q mit einem gesperrten Item in der Hand tut nichts. */
@Mixin({LocalPlayer.class})
public abstract class LocalPlayerDropMixin {
   @Inject(method = {"drop(Z)Z"}, at = {@At("HEAD")}, cancellable = true)
   private void maneszn$dropLock(boolean all, CallbackInfoReturnable<Boolean> cir) {
      if (DropLock.block(((LocalPlayer)(Object)this).getMainHandItem())) {
         cir.setReturnValue(false);
      }
   }
}
