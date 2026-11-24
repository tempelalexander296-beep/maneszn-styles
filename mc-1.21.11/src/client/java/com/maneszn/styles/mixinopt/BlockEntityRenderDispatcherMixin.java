package com.maneszn.styles.mixinopt;

import com.maneszn.styles.perf.PieChart;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BlockEntityRenderDispatcher.class})
public abstract class BlockEntityRenderDispatcherMixin {
   private static final Map<BlockEntityType<?>, String> maneszn$NAMES = new IdentityHashMap<>();
   private static final ThreadLocal<Integer> maneszn$DEPTH = ThreadLocal.withInitial(() -> 0);

   @Inject(
      method = {"tryExtractRenderState"},
      at = {@At("HEAD")},
      require = 0
   )
   private void maneszn$push(BlockEntity be, float partialTick, CrumblingOverlay crumbling, CallbackInfoReturnable<?> cir) {
      if (PieChart.collecting()) {
         String name = maneszn$NAMES.computeIfAbsent(be.getType(), t -> {
            Identifier id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey((BlockEntityType<?>)t);
            return id == null ? "unknown" : id.getPath();
         });
         Profiler.get().push(name);
         maneszn$DEPTH.set(maneszn$DEPTH.get() + 1);
      }
   }

   @Inject(
      method = {"tryExtractRenderState"},
      at = {@At("RETURN")},
      require = 0
   )
   private void maneszn$pop(BlockEntity be, float partialTick, CrumblingOverlay crumbling, CallbackInfoReturnable<?> cir) {
      int d = maneszn$DEPTH.get();
      if (d > 0) {
         maneszn$DEPTH.set(d - 1);
         Profiler.get().pop();
      }
   }
}
