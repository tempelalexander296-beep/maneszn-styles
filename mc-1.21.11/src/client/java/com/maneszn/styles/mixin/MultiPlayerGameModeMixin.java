package com.maneszn.styles.mixin;

import com.maneszn.styles.tools.DuraGuard;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MultiPlayerGameMode.class})
public abstract class MultiPlayerGameModeMixin {
   @Inject(
      method = {"startDestroyBlock"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void maneszn$guardStart(BlockPos pos, Direction dir, CallbackInfoReturnable<Boolean> cir) {
      if (DuraGuard.blockMining()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"continueDestroyBlock"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void maneszn$guardContinue(BlockPos pos, Direction dir, CallbackInfoReturnable<Boolean> cir) {
      if (DuraGuard.blockMining()) {
         ((MultiPlayerGameMode)(Object)this).stopDestroyBlock();
         cir.setReturnValue(false);
      }
   }
}
