package com.maneszn.styles.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({AbstractContainerScreen.class})
public interface AbstractContainerScreenAccessor {
   @Accessor("hoveredSlot")
   Slot maneszn$hoveredSlot();

   @Accessor("leftPos")
   int maneszn$leftPos();

   @Accessor("topPos")
   int maneszn$topPos();
}
