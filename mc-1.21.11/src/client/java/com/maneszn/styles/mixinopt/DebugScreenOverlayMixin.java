package com.maneszn.styles.mixinopt;

import com.maneszn.styles.perf.PieChart;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({DebugScreenOverlay.class})
public abstract class DebugScreenOverlayMixin {
   @Inject(
      method = {"showProfilerChart"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void maneszn$pie(CallbackInfoReturnable<Boolean> cir) {
      if (PieChart.isOpen()) {
         cir.setReturnValue(true);
      }
   }
}
