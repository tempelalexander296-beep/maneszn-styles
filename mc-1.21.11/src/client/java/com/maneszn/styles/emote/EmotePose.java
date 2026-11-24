package com.maneszn.styles.emote;

public final class EmotePose {
   public static final int HEAD = 0;
   public static final int BODY = 1;
   public static final int RIGHT_ARM = 2;
   public static final int LEFT_ARM = 3;
   public static final int RIGHT_LEG = 4;
   public static final int LEFT_LEG = 5;
   public final float[][] rot = new float[6][3];
   public final float[][] off = new float[6][3];
   public final boolean[] set = new boolean[6];

   public EmotePose rot(int part, double x, double y, double z) {
      this.rot[part][0] = (float)x;
      this.rot[part][1] = (float)y;
      this.rot[part][2] = (float)z;
      this.set[part] = true;
      return this;
   }

   public EmotePose off(int part, double x, double y, double z) {
      this.off[part][0] = this.off[part][0] + (float)x;
      this.off[part][1] = this.off[part][1] + (float)y;
      this.off[part][2] = this.off[part][2] + (float)z;
      return this;
   }

   public EmotePose offAll(double x, double y, double z) {
      for (int i = 0; i < 6; i++) {
         this.off(i, x, y, z);
      }

      return this;
   }

   public EmotePose offUpper(double x, double y, double z) {
      for (int i = 0; i <= 3; i++) {
         this.off(i, x, y, z);
      }

      return this;
   }

   public EmotePose restRest() {
      for (int i = 0; i < 6; i++) {
         if (!this.set[i] && i != 0) {
            this.rot(i, 0.0, 0.0, i == 2 ? 0.05 : (i == 3 ? -0.05 : 0.0));
         }
      }

      return this;
   }
}
