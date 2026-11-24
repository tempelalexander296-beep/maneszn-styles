package com.maneszn.styles.perf;

import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.config.ModConfig;
import java.util.Map;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.level.ParticleStatus;

public final class FpsBoost {
   private static final double COSMETIC_RANGE = 24.0;

   private FpsBoost() {
   }

   public static boolean enabled() {
      return ModConfig.get().fpsBoost;
   }

   public static boolean skipCosmetics(double distanceSq) {
      return enabled() && distanceSq > 576.0;
   }

   public static void tick(Minecraft mc) {
   }

   public static void set(boolean on) {
      ModConfig cfg = ModConfig.get();
      Minecraft mc = Minecraft.getInstance();
      Options o = mc.options;
      if (on && !cfg.fpsBoost) {
         Map<String, String> b = cfg.fpsBoostBackup;
         b.clear();
         b.put("clouds", o.cloudStatus().get().name());
         b.put("particles", o.particles().get().name());
         b.put("biomeBlend", String.valueOf(o.biomeBlendRadius().get()));
         b.put("shadows", String.valueOf(o.entityShadows().get()));
         b.put("ao", String.valueOf(o.ambientOcclusion().get()));
         b.put("transparency", String.valueOf(o.improvedTransparency().get()));
         b.put("fade", String.valueOf(o.chunkSectionFadeInTime().get()));
         b.put("entityDist", String.valueOf(o.entityDistanceScaling().get()));
         b.put("vsync", String.valueOf(o.enableVsync().get()));
         b.put("fps", String.valueOf(o.framerateLimit().get()));
         b.put("vignette", String.valueOf(o.vignette().get()));
         safe(() -> o.cloudStatus().set(CloudStatus.OFF));
         safe(() -> o.particles().set(ParticleStatus.DECREASED));
         safe(() -> o.biomeBlendRadius().set(0));
         safe(() -> o.entityShadows().set(false));
         safe(() -> o.ambientOcclusion().set(false));
         safe(() -> o.improvedTransparency().set(false));
         safe(() -> o.chunkSectionFadeInTime().set(0.0));
         safe(() -> o.entityDistanceScaling().set(0.75));
         safe(() -> o.enableVsync().set(false));
         safe(() -> o.framerateLimit().set(260));
         safe(() -> o.vignette().set(false));
         cfg.fpsBoost = true;
      } else if (!on && cfg.fpsBoost) {
         Map<String, String> b = cfg.fpsBoostBackup;
         safe(() -> o.cloudStatus().set(CloudStatus.valueOf(b.getOrDefault("clouds", "FANCY"))));
         safe(() -> o.particles().set(ParticleStatus.valueOf(b.getOrDefault("particles", "ALL"))));
         safe(() -> o.biomeBlendRadius().set(Integer.parseInt(b.getOrDefault("biomeBlend", "2"))));
         safe(() -> o.entityShadows().set(Boolean.parseBoolean(b.getOrDefault("shadows", "true"))));
         safe(() -> o.ambientOcclusion().set(Boolean.parseBoolean(b.getOrDefault("ao", "true"))));
         safe(() -> o.improvedTransparency().set(Boolean.parseBoolean(b.getOrDefault("transparency", "false"))));
         safe(() -> o.chunkSectionFadeInTime().set(Double.parseDouble(b.getOrDefault("fade", "0.75"))));
         safe(() -> o.entityDistanceScaling().set(Double.parseDouble(b.getOrDefault("entityDist", "1.0"))));
         safe(() -> o.enableVsync().set(Boolean.parseBoolean(b.getOrDefault("vsync", "true"))));
         safe(() -> o.framerateLimit().set(Integer.parseInt(b.getOrDefault("fps", "120"))));
         safe(() -> o.vignette().set(Boolean.parseBoolean(b.getOrDefault("vignette", "true"))));
         b.clear();
         cfg.fpsBoost = false;
      }

      safe(o::save);
      ModConfig.save();
   }

   private static void safe(Runnable r) {
      try {
         r.run();
      } catch (Throwable var2) {
         ManesznStyles.LOGGER.warn("FPS-Boost: Einstellung nicht gesetzt: {}", var2.toString());
      }
   }
}
