package com.maneszn.styles.mixin;

import com.maneszn.styles.tools.DropLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drop-Schutz im Kreativ-Inventar: kein Wegwerfen, kein Papierkorb, kein Überschreiben durch die Item-Liste. */
@Mixin({CreativeModeInventoryScreen.class})
public abstract class CreativeDropMixin {
   @Shadow
   private @Nullable Slot destroyItemSlot;

   @Shadow
   private boolean isCreativeSlot(@Nullable Slot slot) {
      throw new AssertionError();
   }

   @Inject(method = {"slotClicked"}, at = {@At("HEAD")}, cancellable = true)
   private void maneszn$dropLock(@Nullable Slot slot, int slotId, int button, ClickType type, CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return;
      }

      ItemStack carried = ((CreativeModeInventoryScreen)(Object)this).getMenu().getCarried();
      boolean throwing = type == ClickType.THROW || slotId == -999 && type == ClickType.PICKUP;
      ItemStack culprit = ItemStack.EMPTY;
      if (slot != null && slot == this.destroyItemSlot) {
         if (DropLock.isLocked(carried)) {
            culprit = carried;
         } else if (type == ClickType.QUICK_MOVE) {
            // Shift-Klick auf den Papierkorb leert das ganze Inventar
            for (int k = 0; k < mc.player.getInventory().getContainerSize() && culprit.isEmpty(); k++) {
               if (DropLock.isLocked(mc.player.getInventory().getItem(k))) {
                  culprit = mc.player.getInventory().getItem(k);
               }
            }
         }
      } else if (slot == null && type != ClickType.QUICK_CRAFT) {
         culprit = DropLock.isLocked(carried) ? carried : ItemStack.EMPTY;
      } else if (throwing && slot != null && carried.isEmpty()) {
         culprit = DropLock.isLocked(slot.getItem()) ? slot.getItem() : ItemStack.EMPTY;
      } else if (slot != null && this.isCreativeSlot(slot)) {
         culprit = DropLock.isLocked(carried) ? carried : ItemStack.EMPTY; // würde das Item in der Hand löschen
      }

      if (!culprit.isEmpty() && DropLock.block(culprit)) {
         ci.cancel();
      }
   }
}
