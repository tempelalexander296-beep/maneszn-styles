package com.maneszn.styles.view;

import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.compat.Hooks;
import com.maneszn.styles.config.ModConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class Freelook {
   private static boolean active;
   private static float yaw;
   private static float pitch;
   private static CameraType before;
   private static boolean toggledOn;
   private static boolean wasDown;

   private Freelook() {
   }

   public static boolean active() {
      return active;
   }

   public static float yaw() {
      return yaw;
   }

   public static float pitch() {
      return pitch;
   }

   public static void tick(Minecraft mc) {
      boolean down = ManesznStyles.freelookKey.isDown() && mc.screen == null;
      boolean want;
      if (ModConfig.get().freelookToggle) {
         if (down && !wasDown) {
            toggledOn = !toggledOn;
         }

         want = toggledOn;
      } else {
         want = down;
      }

      wasDown = down;
      if (mc.player == null) {
         want = false;
      }

      if (want && !active) {
         start(mc);
      } else if (!want && active) {
         stop(mc);
      }
   }

   private static void start(Minecraft mc) {
      active = true;
      yaw = mc.player.getYRot();
      pitch = mc.player.getXRot();
      before = mc.options.getCameraType();
      if (before.isFirstPerson()) {
         mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
      }
   }

   private static void stop(Minecraft mc) {
      active = false;
      toggledOn = false;
      if (before != null) {
         mc.options.setCameraType(before);
      }
   }

   public static void turn(double dx, double dy) {
      Hooks.freelook = true;
      yaw += (float)dx * 0.15F;
      pitch = Mth.clamp(pitch + (float)dy * 0.15F, -90.0F, 90.0F);
   }
}
