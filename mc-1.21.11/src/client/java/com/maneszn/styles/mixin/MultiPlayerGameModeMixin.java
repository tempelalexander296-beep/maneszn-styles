package com.maneszn.styles.mixin;

import com.maneszn.styles.tools.DropLock;
import com.maneszn.styles.tools.DuraGuard;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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

   /**
    * Drop-Schutz in jedem Inventar/jeder Truhe: Q bzw. Strg+Q über einem gesperrten Item, oder ein gesperrtes
    * Item am Mauszeiger außerhalb des Fensters ablegen → nichts passiert (es wird auch kein Paket gesendet).
    */
   @Inject(method = {"handleInventoryMouseClick"}, at = {@At("HEAD")}, cancellable = true)
   private void maneszn$dropLock(int containerId, int slotId, int button, ClickType type, Player player, CallbackInfo ci) {
      AbstractContainerMenu menu = player.containerMenu;
      if (menu == null) {
         return;
      }

      ItemStack culprit = ItemStack.EMPTY;
      if (slotId == -999 && (type == ClickType.PICKUP || type == ClickType.THROW)) {
         culprit = menu.getCarried();
      } else if (type == ClickType.THROW && slotId >= 0 && slotId < menu.slots.size() && menu.getCarried().isEmpty()) {
         culprit = menu.getSlot(slotId).getItem();
      }

      if (!culprit.isEmpty() && DropLock.block(culprit)) {
         ci.cancel();
      }
   }
}
