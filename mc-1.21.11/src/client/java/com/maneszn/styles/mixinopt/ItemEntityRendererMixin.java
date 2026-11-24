package com.maneszn.styles.mixinopt;

import com.llamalad7.mixinextras.sugar.Local;
import com.maneszn.styles.compat.Hooks;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.render.ItemScales;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemEntityRenderer.class})
public abstract class ItemEntityRendererMixin {
   private static final Map<ItemEntityRenderState, Float> maneszn$SCALES = new WeakHashMap<>();
   private static final RandomSource maneszn$RANDOM = RandomSource.create();

   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V"},
      at = {@At("TAIL")},
      require = 0
   )
   private void maneszn$remember(CallbackInfo ci, @Local(argsOnly = true) ItemEntity entity, @Local(argsOnly = true) ItemEntityRenderState state) {
      maneszn$SCALES.put(state, ItemScales.dropped(entity.getItem()));
   }

   @Inject(
      method = {"submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void maneszn$dropped(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
      Float f = maneszn$SCALES.get(state);
      float s = f != null ? f : 1.0F;
      String mode = ModConfig.get().droppedItemStyle;
      boolean custom = mode != null && !"vanilla".equals(mode) && !state.item.isEmpty();
      if (!custom) {
         if (Math.abs(s - 1.0F) > 0.001F) {
            Hooks.itemScale = true;
            poseStack.scale(s, s, s);
         }
      } else {
         Hooks.itemScale = true;
         AABB box = state.item.getModelBoundingBox();
         float cx = (float)((box.minX + box.maxX) / 2.0);
         float cy = (float)((box.minY + box.maxY) / 2.0);
         float cz = (float)((box.minZ + box.maxZ) / 2.0);
         float lift = (float)(-box.minY) + 0.0625F;
         float yaw = state.bobOffset * 57.2958F % 360.0F;
         poseStack.pushPose();
         poseStack.scale(s, s, s);
         switch (mode) {
            case "2d":
               float bob = (float)Math.sin(state.ageInTicks / 10.0F + state.bobOffset) * 0.05F + 0.05F;
               poseStack.translate(0.0F, lift + cy + bob, 0.0F);
               if (camera != null && camera.orientation != null) {
                  poseStack.mulPose(camera.orientation);
               }

               poseStack.translate(-cx, -cy, -cz);
               state.item.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
               break;
            case "flat":
               float half = (float)(box.getZsize() / 2.0);
               poseStack.translate(0.0F, half + 0.01F, 0.0F);
               poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
               poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
               poseStack.translate(-cx, -cy, -cz);
               ItemEntityRenderer.submitMultipleFromCount(poseStack, collector, state.lightCoords, state, maneszn$RANDOM, box);
               break;
            default:
               poseStack.translate(0.0F, lift, 0.0F);
               poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
               ItemEntityRenderer.submitMultipleFromCount(poseStack, collector, state.lightCoords, state, maneszn$RANDOM, box);
         }

         poseStack.popPose();
         ci.cancel();
      }
   }
}
