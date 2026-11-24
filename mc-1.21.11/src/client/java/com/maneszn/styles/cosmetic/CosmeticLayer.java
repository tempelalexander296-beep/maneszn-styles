package com.maneszn.styles.cosmetic;

import com.maneszn.styles.compat.Hooks;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.perf.FpsBoost;
import com.maneszn.styles.render.CosmeticScenes;
import com.maneszn.styles.render.MiniPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

public class CosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
   private static final int FULL_BRIGHT = 15728880;
   private static final Cosmetic.Type[] MODEL_TYPES = new Cosmetic.Type[]{
      Cosmetic.Type.HAT,
      Cosmetic.Type.FACE,
      Cosmetic.Type.BACK,
      Cosmetic.Type.HIP,
      Cosmetic.Type.SHOES,
      Cosmetic.Type.BELT,
      Cosmetic.Type.JEWELRY,
      Cosmetic.Type.WINGS
   };

   public CosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
      super(parent);
   }

   public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, float yRot, float xRot) {
      Hooks.layer = true;
      if (!state.isInvisible) {
         if (!FpsBoost.skipCosmetics(state.distanceToCameraSq)) {
            UUID uuid = CosmeticManager.uuidOf(state.id);
            if (uuid != null) {
               Map<String, String> loadout = CosmeticManager.loadout(uuid);
               if (!loadout.isEmpty()) {
                  double ticks = CosmeticManager.ticks();
                  PlayerModel model = this.getParentModel();
                  CosmeticRegistry reg = CosmeticRegistry.get();
                  renderSkinEffect(poseStack, collector, light, state, model, reg.of(loadout, Cosmetic.Type.SKINFX), ticks);
                  renderCape(poseStack, collector, light, state, model, uuid, ticks, CosmeticManager.tint(loadout, Cosmetic.Type.CAPE));

                  for (Cosmetic.Type type : MODEL_TYPES) {
                     Cosmetic c = reg.of(loadout, type);
                     CosmeticModels.Baked b = CosmeticModels.get(c);
                     if (b != null) {
                        b.animate(ticks);
                        int l = b.glow ? 15728880 : light;
                        int tint = CosmeticManager.tint(loadout, type);
                        if (b.hasHead) {
                           poseStack.pushPose();
                           model.head.translateAndRotate(poseStack);
                           submit(collector, b.head, poseStack, b, l, tint);
                           poseStack.popPose();
                        }

                        if (b.hasBody) {
                           poseStack.pushPose();
                           model.body.translateAndRotate(poseStack);
                           submit(collector, b.body, poseStack, b, l, tint);
                           poseStack.popPose();
                        }

                        if (b.hasLegR) {
                           poseStack.pushPose();
                           model.rightLeg.translateAndRotate(poseStack);
                           submit(collector, b.legR, poseStack, b, l, tint);
                           poseStack.popPose();
                        }

                        if (b.hasLegL) {
                           poseStack.pushPose();
                           model.leftLeg.translateAndRotate(poseStack);
                           submit(collector, b.legL, poseStack, b, l, tint);
                           poseStack.popPose();
                        }
                     }
                  }

                  if (ModConfig.get().showAuras) {
                     CosmeticModels.Baked aura = CosmeticModels.get(reg.of(loadout, Cosmetic.Type.AURA));
                     if (aura != null && aura.hasBuddy) {
                        aura.animate(ticks);
                        submit(collector, aura.buddy, poseStack, aura, aura.glow ? 15728880 : light, CosmeticManager.tint(loadout, Cosmetic.Type.AURA));
                     }
                  }

                  this.renderBuddies(poseStack, collector, light, model, loadout, uuid, ticks);
                  renderPet(poseStack, collector, light, state, model, loadout, reg.of(loadout, Cosmetic.Type.PET), uuid, ticks);
               }
            }
         }
      }
   }

   private static void submit(SubmitNodeCollector collector, ModelPart part, PoseStack poseStack, CosmeticModels.Baked b, int light) {
      submit(collector, part, poseStack, b, light, -1);
   }

   private static void submit(SubmitNodeCollector collector, ModelPart part, PoseStack poseStack, CosmeticModels.Baked b, int light, int tint) {
      collector.submitModelPart(part, poseStack, b.renderType, light, OverlayTexture.NO_OVERLAY, null, false, false, tint, null, 0);
   }

   private static void renderSkinEffect(
      PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, PlayerModel model, Cosmetic fx, double ticks
   ) {
      if (fx != null && state.skin != null && state.skin.body() != null) {
         String kind = fx.text() == null ? "glint" : fx.text();
         if (kind.startsWith("tex:") && fx.texture() != null) {
            boolean slimT = state.skin.model() != null && state.skin.model().toString().toLowerCase(Locale.ROOT).contains("slim");
            CosmeticModels.Mini over = CosmeticModels.skinCopy(state.id, slimT);
            copyPose(over, "head", model.head);
            copyPose(over, "body", model.body);
            copyPose(over, "arm_r", model.rightArm);
            copyPose(over, "arm_l", model.leftArm);
            copyPose(over, "leg_r", model.rightLeg);
            copyPose(over, "leg_l", model.leftLeg);
            Identifier glintTex = CosmeticManager.texture(fx.framePath(fx.frameAt((long)ticks)));
            collector.submitModelPart(
               over.root(), poseStack, CosmeticModels.skinType(glintTex), 15728880, OverlayTexture.NO_OVERLAY, null, false, false, -1, null, 0
            );
         } else {
            boolean glint = kind.startsWith("glint");
            boolean glow = kind.endsWith("glow");
            Identifier tex = state.skin.body().texturePath();
            boolean slim = state.skin.model() != null && state.skin.model().toString().toLowerCase(Locale.ROOT).contains("slim");
            CosmeticModels.Mini copy = CosmeticModels.skinCopy(state.id, slim);
            copyPose(copy, "head", model.head);
            copyPose(copy, "body", model.body);
            copyPose(copy, "arm_r", model.rightArm);
            copyPose(copy, "arm_l", model.leftArm);
            copyPose(copy, "leg_r", model.rightLeg);
            copyPose(copy, "leg_l", model.leftLeg);
            collector.submitModelPart(
               copy.root(), poseStack, CosmeticModels.skinType(tex), glow ? 15728880 : light, OverlayTexture.NO_OVERLAY, null, false, glint, -1, null, 0
            );
         }
      }
   }

   private static void copyPose(CosmeticModels.Mini copy, String key, ModelPart from) {
      ModelPart to = copy.parts().get(key);
      if (to != null && from != null) {
         to.x = from.x;
         to.y = from.y;
         to.z = from.z;
         to.xRot = from.xRot;
         to.yRot = from.yRot;
         to.zRot = from.zRot;
         to.visible = from.visible;
      }
   }

   private static void renderCape(
      PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state, PlayerModel model, UUID uuid, double ticks, int tint
   ) {
      if (ModConfig.get().capePhysics && state.showCape) {
         Identifier tex = CosmeticManager.capeTexture(uuid);
         if (tex != null) {
            boolean chest = state.chestEquipment != null && !state.chestEquipment.isEmpty();
            if (!chest || !state.chestEquipment.is(Items.ELYTRA)) {
               poseStack.pushPose();
               model.body.translateAndRotate(poseStack);
               if (chest) {
                  poseStack.translate(0.0F, -0.053125F, 0.06875F);
               }

               if (state.isCrouching) {
                  poseStack.translate(0.0F, 0.115625F, 0.0875F);
               }

               float flap = 6.0F + state.capeLean / 2.0F + state.capeFlap;
               float speed = Math.max(0.0F, Math.min(1.0F, state.capeLean / 90.0F));
               CapeRenderer.submit(poseStack, collector, tex, light, flap, state.capeLean2, speed, ticks, tint);
               poseStack.popPose();
            }
         }
      }
   }

   private void renderBuddies(
      PoseStack poseStack, SubmitNodeCollector collector, int light, PlayerModel model, Map<String, String> loadout, UUID uuid, double ticks
   ) {
      List<CosmeticManager.BuddyAt> buddies = CosmeticManager.buddies(loadout);
      if (!buddies.isEmpty()) {
         Cosmetic hatC = CosmeticRegistry.get().of(loadout, Cosmetic.Type.HAT);
         boolean hatOn = hatC != null && !hatC.glow();
         boolean walking = isWalking(uuid);

         for (CosmeticManager.BuddyAt b : buddies) {
            if (b.buddy().skin()) {
               CosmeticScenes.BuddySpot spot = b.spot();
               MiniPlayer.Pose pose = MiniPlayer.Pose.of(spot);
               SkinFetcher.Skin skin = SkinFetcher.get(CosmeticManager.skinNameOr(b.skinName()));
               if (skin != null) {
                  float[] off = CosmeticScenes.buddyOffset(spot, hatOn);
                  float scale = CosmeticScenes.buddyScale(spot, true);
                  poseStack.pushPose();
                  switch (spot) {
                     case HEAD:
                        model.head.translateAndRotate(poseStack);
                        break;
                     case ARM:
                        model.rightArm.translateAndRotate(poseStack);
                     case HAND:
                        break;
                     default:
                        model.body.translateAndRotate(poseStack);
                  }

                  poseStack.translate(off[0] / 16.0F, off[1] / 16.0F, off[2] / 16.0F);
                  poseStack.scale(scale, scale, scale);
                  float sw = MiniPlayer.swing(pose, ticks);
                  if (sw != 0.0F) {
                     poseStack.mulPose(Axis.ZP.rotation(sw));
                  }

                  poseStack.translate(0.0F, MiniPlayer.shiftY(pose) / 16.0F, 0.0F);
                  boolean walk = walking && pose == MiniPlayer.Pose.HOLD;
                  CosmeticModels.Mini mini = CosmeticModels.mini(skin.slim(), spot, walk);
                  mini.animate(ticks, pose, walk);
                  collector.submitModelPart(
                     mini.root(), poseStack, CosmeticModels.skinType(skin.texture()), light, OverlayTexture.NO_OVERLAY, null, false, false, -1, null, 0
                  );
                  poseStack.popPose();
               }
            }
         }
      }
   }

   private static void renderPet(
      PoseStack poseStack,
      SubmitNodeCollector collector,
      int light,
      AvatarRenderState state,
      PlayerModel model,
      Map<String, String> loadout,
      Cosmetic pet,
      UUID uuid,
      double ticks
   ) {
      CosmeticModels.Baked bk = CosmeticModels.get(pet);
      if (bk != null && bk.hasBuddy) {
         int tint = CosmeticManager.tint(loadout, Cosmetic.Type.PET);
         if (!"head".equals(loadout.get("pet_pos"))) {
            PetTracker.Pet p = PetTracker.update(uuid, state.x, state.y, state.z, state.bodyRot);
            poseStack.pushPose();
            poseStack.translate(0.0F, 1.501F, 0.0F);
            poseStack.scale(1.0666667F, 1.0666667F, 1.0666667F);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-(180.0F - state.bodyRot)));
            double hop = Math.abs(Math.sin(p.phase)) * 0.12 * p.speed;
            poseStack.translate(p.x - state.x, p.y - state.y + hop, p.z - state.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - p.yaw));
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            float s = 1.4F;
            poseStack.scale(s, s, s);
            bk.animate(ticks);
            submit(collector, bk.buddy, poseStack, bk, bk.glow ? 15728880 : light, tint);
            poseStack.popPose();
         } else {
            Cosmetic hat = CosmeticRegistry.get().of(loadout, Cosmetic.Type.HAT);
            boolean hatOn = hat != null && !hat.glow();
            poseStack.pushPose();
            model.head.translateAndRotate(poseStack);
            poseStack.translate(0.0F, (hatOn ? -12.2F : -8.05F) / 16.0F, 0.03125F);
            poseStack.scale(0.8F, 0.8F, 0.8F);
            bk.animate(ticks);
            submit(collector, bk.buddy, poseStack, bk, bk.glow ? 15728880 : light, tint);
            poseStack.popPose();
         }
      }
   }

   private static boolean isWalking(UUID uuid) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return false;
      } else {
         for (AbstractClientPlayer p : mc.level.players()) {
            if (p.getUUID().equals(uuid)) {
               return p.getDeltaMovement().horizontalDistanceSqr() > 9.0E-4;
            }
         }

         return false;
      }
   }
}
