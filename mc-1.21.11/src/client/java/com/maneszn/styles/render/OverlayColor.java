package com.maneszn.styles.render;

import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.compat.Hooks;
import com.maneszn.styles.config.ModConfig;
import com.mojang.blaze3d.platform.NativeImage;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

public final class OverlayColor {
   private static final int VANILLA = -1275133952;
   private static int applied = Integer.MIN_VALUE;
   private static boolean broken;

   private OverlayColor() {
   }

   public static void tick(Minecraft mc) {
      int want = ModConfig.get().hitColor;
      if (want != applied && !broken && mc.gameRenderer != null) {
         try {
            OverlayTexture ot = mc.gameRenderer.overlayTexture();
            DynamicTexture tex = null;

            for (Field f : OverlayTexture.class.getDeclaredFields()) {
               if (DynamicTexture.class.isAssignableFrom(f.getType())) {
                  f.setAccessible(true);
                  tex = (DynamicTexture)f.get(ot);
                  break;
               }
            }

            if (tex == null || tex.getPixels() == null) {
               broken = true;
               return;
            }

            NativeImage img = tex.getPixels();
            int color = want == 0 ? -1275133952 : -1291845632 | want & 16777215;

            for (int y = 0; y < 8; y++) {
               for (int x = 0; x < 16; x++) {
                  img.setPixel(x, y, color);
               }
            }

            tex.upload();
            applied = want;
            Hooks.overlay = true;
         } catch (Throwable var8) {
            broken = true;
            ManesznStyles.LOGGER.warn("Trefferfarbe nicht änderbar: {}", var8.toString());
         }
      }
   }
}
