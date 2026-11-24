package com.maneszn.styles.cosmetic;

import com.maneszn.styles.compat.Hooks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class CapeRenderer {
   private static final int SEGMENTS = 16;

   private CapeRenderer() {
   }

   public static void submit(
      PoseStack poseStack, SubmitNodeCollector collector, Identifier texture, int light, float flap, float side, float speed, double ticks, int tint
   ) {
      Hooks.capePhysics = true;
      float[] ang = new float[16];
      double base = Math.toRadians(flap);
      double amp = 0.03 + 0.22 * Math.max(0.0F, Math.min(1.0F, speed));

      for (int i = 0; i < 16; i++) {
         double f = i / 15.0;
         double bend = base * (0.55 + 0.9 * f);
         double wave = Math.sin(ticks * (0.25 + 0.25 * speed) - i * 0.45) * amp * (0.3 + 0.7 * f);
         ang[i] = (float)(bend + wave);
      }

      float[] py = new float[17];
      float[] pz = new float[17];

      for (int i = 0; i < 16; i++) {
         py[i + 1] = py[i] + (float)Math.cos(ang[i]);
         pz[i + 1] = pz[i] + (float)Math.sin(ang[i]);
      }

      poseStack.pushPose();
      poseStack.translate(0.0F, 0.0F, 0.15F);
      if (side != 0.0F) {
         poseStack.mulPose(Axis.ZP.rotationDegrees(side / 2.0F));
      }

      collector.submitCustomGeometry(
         poseStack,
         RenderTypes.entityCutoutNoCull(texture),
         (pose, vc) -> {
            for (int ix = 0; ix < 16; ix++) {
               float y0 = py[ix];
               float y1 = py[ix + 1];
               float z0 = pz[ix];
               float z1 = pz[ix + 1];
               float ny = (float)(-Math.sin(ang[ix]));
               float nz = (float)Math.cos(ang[ix]);
               float oy0 = y0 + ny;
               float oz0 = z0 + nz;
               float oy1 = y1 + ny;
               float oz1 = z1 + nz;
               float v0 = (1 + ix) / 32.0F;
               float v1 = (2 + ix) / 32.0F;
               quad(vc, pose, 5.0F, oy0, oz0, -5.0F, oy0, oz0, -5.0F, oy1, oz1, 5.0F, oy1, oz1, 0.015625F, v0, 0.171875F, v1, 0.0F, ny, nz, light, tint);
               quad(vc, pose, -5.0F, y0, z0, 5.0F, y0, z0, 5.0F, y1, z1, -5.0F, y1, z1, 0.1875F, v0, 0.34375F, v1, 0.0F, -ny, -nz, light, tint);
               quad(vc, pose, 5.0F, y0, z0, 5.0F, oy0, oz0, 5.0F, oy1, oz1, 5.0F, y1, z1, 0.0F, v0, 0.015625F, v1, 1.0F, 0.0F, 0.0F, light, tint);
               quad(vc, pose, -5.0F, oy0, oz0, -5.0F, y0, z0, -5.0F, y1, z1, -5.0F, oy1, oz1, 0.171875F, v0, 0.1875F, v1, -1.0F, 0.0F, 0.0F, light, tint);
            }

            int n = 16;
            float ny = (float)(-Math.sin(ang[0]));
            float nz = (float)Math.cos(ang[0]);
            quad(
               vc, pose, -5.0F, 0.0F, 0.0F, 5.0F, 0.0F, 0.0F, 5.0F, ny, nz, -5.0F, ny, nz, 0.015625F, 0.0F, 0.171875F, 0.03125F, 0.0F, -1.0F, 0.0F, light, tint
            );
            float by = py[n];
            float bz = pz[n];
            float my = (float)(-Math.sin(ang[n - 1]));
            float mz = (float)Math.cos(ang[n - 1]);
            quad(
               vc,
               pose,
               -5.0F,
               by,
               bz,
               5.0F,
               by,
               bz,
               5.0F,
               by + my,
               bz + mz,
               -5.0F,
               by + my,
               bz + mz,
               0.171875F,
               0.0F,
               0.328125F,
               0.03125F,
               0.0F,
               1.0F,
               0.0F,
               light,
               tint
            );
         }
      );
      poseStack.popPose();
   }

   private static void quad(
      VertexConsumer vc,
      Pose pose,
      float x0,
      float y0,
      float z0,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float u0,
      float v0,
      float u1,
      float v1,
      float nx,
      float ny,
      float nz,
      int light,
      int tint
   ) {
      vertex(vc, pose, x0, y0, z0, u0, v0, nx, ny, nz, light, tint);
      vertex(vc, pose, x1, y1, z1, u1, v0, nx, ny, nz, light, tint);
      vertex(vc, pose, x2, y2, z2, u1, v1, nx, ny, nz, light, tint);
      vertex(vc, pose, x3, y3, z3, u0, v1, nx, ny, nz, light, tint);
   }

   private static void vertex(VertexConsumer vc, Pose pose, float x, float y, float z, float u, float v, float nx, float ny, float nz, int light, int tint) {
      vc.addVertex(pose, x / 16.0F, y / 16.0F, z / 16.0F)
         .setColor(tint)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(pose, nx, ny, nz);
   }
}
