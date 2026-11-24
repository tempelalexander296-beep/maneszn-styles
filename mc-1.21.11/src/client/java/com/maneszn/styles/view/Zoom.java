package com.maneszn.styles.view;

import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.compat.Hooks;
import com.maneszn.styles.config.ModConfig;
import net.minecraft.client.Minecraft;

public final class Zoom {
   private static double current = 1.0;
   private static double level = -1.0;
   private static long lastNanos;
   public static volatile float lastFov = 70.0F;

   private Zoom() {
   }

   public static boolean held() {
      Minecraft mc = Minecraft.getInstance();
      return ManesznStyles.zoomKey.isDown() && mc.screen == null && mc.player != null;
   }

   public static double factor() {
      if (level < 0.0) {
         level = Math.max(1.5, ModConfig.get().zoomLevel);
      }

      double target = held() ? level : 1.0;
      long now = System.nanoTime();
      double dt = lastNanos == 0L ? 0.016 : Math.min(0.1, (now - lastNanos) / 1.0E9);
      lastNanos = now;
      double k = ModConfig.get().zoomSmooth ? 1.0 - Math.exp(-dt * 14.0) : 1.0;
      current = current + (target - current) * k;
      if (Math.abs(current - target) < 0.001) {
         current = target;
      }

      return current;
   }

   public static float apply(float fov) {
      Hooks.zoom = true;
      double f = factor();
      float out = f == 1.0 ? fov : (float)(fov / f);
      lastFov = out;
      return out;
   }

   public static double sensitivity() {
      return current > 1.01 ? 1.0 / current : 1.0;
   }

   public static boolean scroll(double amount) {
      if (!held()) {
         return false;
      } else {
         level = Math.max(1.5, Math.min(40.0, level * (amount > 0.0 ? 1.25 : 0.8)));
         ModConfig.get().zoomLevel = level;
         return true;
      }
   }
}
