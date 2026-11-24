package com.maneszn.styles.render;

import com.maneszn.styles.image.ImageProcessor;
import java.util.ArrayList;
import java.util.List;

public final class MiniPlayer {
   public static final float SCALE = 0.32F;

   private MiniPlayer() {
   }

   public static List<MiniPlayer.Box> boxes(boolean slim) {
      List<MiniPlayer.Box> b = new ArrayList<>();
      float a = slim ? 3.0F : 4.0F;
      add(b, "head", 0, 0, -4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      add(b, "head", 32, 0, -4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, 0.5F, 0.0F, 0.0F, 0.0F);
      add(b, "body", 16, 16, -4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.0F);
      add(b, "body", 16, 32, -4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, 0.25F, 0.0F, 0.0F, 0.0F);
      add(b, "arm_r", 40, 16, -4.0F - a, 0.0F, -2.0F, a, 12.0F, 4.0F, 0.0F, -5.0F, 2.0F, 0.0F);
      add(b, "arm_r", 40, 32, -4.0F - a, 0.0F, -2.0F, a, 12.0F, 4.0F, 0.25F, -5.0F, 2.0F, 0.0F);
      add(b, "arm_l", 32, 48, 4.0F, 0.0F, -2.0F, a, 12.0F, 4.0F, 0.0F, 5.0F, 2.0F, 0.0F);
      add(b, "arm_l", 48, 48, 4.0F, 0.0F, -2.0F, a, 12.0F, 4.0F, 0.25F, 5.0F, 2.0F, 0.0F);
      add(b, "leg_r", 0, 16, -4.0F, 12.0F, -2.0F, 4.0F, 12.0F, 4.0F, 0.0F, -2.0F, 12.0F, 0.0F);
      add(b, "leg_r", 0, 32, -4.0F, 12.0F, -2.0F, 4.0F, 12.0F, 4.0F, 0.25F, -2.0F, 12.0F, 0.0F);
      add(b, "leg_l", 16, 48, 0.0F, 12.0F, -2.0F, 4.0F, 12.0F, 4.0F, 0.0F, 2.0F, 12.0F, 0.0F);
      add(b, "leg_l", 0, 48, 0.0F, 12.0F, -2.0F, 4.0F, 12.0F, 4.0F, 0.25F, 2.0F, 12.0F, 0.0F);
      return b;
   }

   private static void add(
      List<MiniPlayer.Box> b, String part, int u, int v, float x, float y, float z, float w, float h, float d, float inflate, float px, float py, float pz
   ) {
      b.add(new MiniPlayer.Box(part, u, v, x, y - 24.0F, z, w, h, d, inflate, px, py - 24.0F, pz));
   }

   public static float[] rotation(String part, double ticks) {
      return rotation(part, ticks, MiniPlayer.Pose.STAND, false);
   }

   public static float[] rotation(String part, double ticks, boolean holdsHand, boolean walking) {
      return rotation(part, ticks, holdsHand ? MiniPlayer.Pose.HOLD : MiniPlayer.Pose.STAND, walking);
   }

   public static float[] rotation(String part, double ticks, MiniPlayer.Pose pose, boolean walking) {
      switch (pose) {
         case SIT:
            double dangle = Math.sin(ticks * 0.12) * 0.22;

            return switch (part) {
               case "leg_r" -> new float[]{(float)(-1.45 + dangle), 0.12F, 0.0F};
               case "leg_l" -> new float[]{(float)(-1.45 - dangle), -0.12F, 0.0F};
               case "arm_r" -> {
                  double cycle = ticks % 240.0;
                  yield cycle < 40.0 ? new float[]{-2.6F, 0.0F, (float)(Math.sin(ticks * 0.6) * 0.35)} : new float[]{-0.55F, 0.0F, 0.12F};
               }
               case "arm_l" -> new float[]{-0.55F, 0.0F, -0.12F};
               case "head" -> new float[]{0.0F, (float)(Math.sin(ticks * 0.03) * 0.35), 0.0F};
               default -> new float[]{0.0F, 0.0F, 0.0F};
            };
         case HANG:
            double kick = Math.sin(ticks * 0.1) * 0.3;

            return switch (part) {
               case "arm_r" -> new float[]{(float) Math.PI, 0.0F, -0.08F};
               case "arm_l" -> new float[]{(float) Math.PI, 0.0F, 0.08F};
               case "leg_r" -> new float[]{(float)kick, 0.0F, 0.05F};
               case "leg_l" -> new float[]{(float)(-kick), 0.0F, -0.05F};
               case "head" -> new float[]{-0.15F, (float)(Math.sin(ticks * 0.025) * 0.3), 0.0F};
               default -> new float[]{0.0F, 0.0F, 0.0F};
            };
         case HOLD:
            double sw = walking ? Math.sin(ticks * 0.6) * 0.7 : 0.0;

            return switch (part) {
               case "arm_l" -> new float[]{-0.25F, 0.0F, -0.75F};
               case "arm_r" -> new float[]{(float)(-sw * 0.8), 0.0F, 0.08F};
               case "leg_r" -> new float[]{(float)sw, 0.0F, 0.0F};
               case "leg_l" -> new float[]{(float)(-sw), 0.0F, 0.0F};
               case "head" -> new float[]{-0.25F, (float)(-0.35 + Math.sin(ticks * 0.03) * 0.2), 0.0F};
               default -> new float[]{0.0F, 0.0F, 0.0F};
            };
         default:
            return switch (part) {
               case "arm_r" -> {
                  double cycle = ticks % 200.0;
                  yield cycle < 40.0
                     ? new float[]{-2.6F, 0.0F, (float)(Math.sin(ticks * 0.6) * 0.35)}
                     : new float[]{(float)(Math.sin(ticks * 0.08) * 0.25), 0.0F, 0.08F};
               }
               case "arm_l" -> new float[]{(float)(-Math.sin(ticks * 0.08) * 0.25), 0.0F, -0.08F};
               case "head" -> new float[]{0.0F, (float)(Math.sin(ticks * 0.03) * 0.3), 0.0F};
               default -> new float[]{0.0F, 0.0F, 0.0F};
            };
      }
   }

   public static float shiftY(MiniPlayer.Pose pose) {
      return switch (pose) {
         case SIT -> 12.0F;
         case HANG -> 33.0F;
         default -> 0.0F;
      };
   }

   public static float swing(MiniPlayer.Pose pose, double ticks) {
      return pose == MiniPlayer.Pose.HANG ? (float)(Math.sin(ticks * 0.07) * 0.18) : 0.0F;
   }

   public static boolean isSlim(ImageProcessor.Img skin) {
      if (skin != null && skin.h() >= 64) {
         double k = skin.w() / 64.0;
         int x = (int)(50.0 * k + k / 2.0);
         int y = (int)(17.0 * k + k / 2.0);
         return skin.px()[y * skin.w() + x] >>> 24 == 0;
      } else {
         return false;
      }
   }

   public static ImageProcessor.Img upgrade(ImageProcessor.Img old) {
      if (old != null && old.h() * 2 == old.w()) {
         int s = old.w() / 64;
         int[] px = new int[old.w() * old.w()];
         System.arraycopy(old.px(), 0, px, 0, old.px().length);
         ImageProcessor.Img n = new ImageProcessor.Img(old.w(), old.w(), px);
         copy(old, n, 0, 16, 16, 48, 16, 16, s);
         copy(old, n, 40, 16, 32, 48, 16, 16, s);
         return n;
      } else {
         return old;
      }
   }

   private static void copy(ImageProcessor.Img src, ImageProcessor.Img dst, int sx, int sy, int dx, int dy, int w, int h, int s) {
      for (int y = 0; y < h * s; y++) {
         for (int x = 0; x < w * s; x++) {
            dst.px()[(dy * s + y) * dst.w() + dx * s + x] = src.px()[(sy * s + y) * src.w() + sx * s + x];
         }
      }
   }

   public record Box(String part, int u, int v, float x, float y, float z, float w, float h, float d, float inflate, float px, float py, float pz) {
   }

   public static enum Pose {
      STAND,
      SIT,
      HANG,
      HOLD;

      public static MiniPlayer.Pose of(CosmeticScenes.BuddySpot spot) {
         if (spot == null) {
            return STAND;
         } else {
            return switch (spot) {
               case HEAD, LEFT_SHOULDER, RIGHT_SHOULDER -> SIT;
               case ARM -> HANG;
               case HAND -> HOLD;
            };
         }
      }
   }
}
