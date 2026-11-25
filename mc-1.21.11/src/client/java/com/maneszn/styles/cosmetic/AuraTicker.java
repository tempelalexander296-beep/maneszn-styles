package com.maneszn.styles.cosmetic;

import com.maneszn.styles.compat.Hooks;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.name.NameDecorator;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

public final class AuraTicker {
   private static final Random RNG = new Random();
   private static long tick;

   private AuraTicker() {
   }

   public static void tick(Minecraft mc) {
      tick++;
      ClientLevel level = mc.level;
      if (level != null && mc.player != null && !mc.isPaused() && ModConfig.get().showAuras) {
         for (AbstractClientPlayer p : level.players()) {
            if (!p.isInvisible() && !p.isSpectator()) {
               double maxD = ModConfig.get().fpsBoost ? 16.0 : 48.0;
               if (!(p.distanceToSqr(mc.player) > maxD * maxD)) {
                  if ((tick + p.getId()) % 7L == 0L && !(p == mc.player && mc.options.getCameraType().isFirstPerson())) {
                     rarityShimmer(p);
                  }

                  Cosmetic aura = CosmeticManager.get(p.getUUID(), Cosmetic.Type.AURA);
                  if (aura != null && aura.particle() != null) {
                     int rate = Math.max(1, aura.rate()) * (ModConfig.get().fpsBoost ? 2 : 1);
                     if ((tick + p.getId()) % rate == 0L) {
                        boolean firstPerson = p == mc.player && mc.options.getCameraType().isFirstPerson();
                        spawn(level, p, aura, firstPerson);
                     }
                  }
               }
            }
         }
      }
   }

   private static void spawn(ClientLevel level, AbstractClientPlayer p, Cosmetic aura, boolean firstPerson) {
      ParticleOptions particle = particle(aura.particle());
      if (particle != null) {
         double x = p.getX();
         double y = p.getY();
         double z = p.getZ();
         double h = p.getBbHeight();
         String var17 = aura.mode();
         switch (var17) {
            case "ring": {
               double ang = tick * 0.35 + RNG.nextDouble() * 0.4;
               double r = 0.75;
               emit(particle, x + Math.cos(ang) * r, y + 0.08, z + Math.sin(ang) * r, 0.0, 0.01, 0.0);
               break;
            }
            case "above":
               if (firstPerson) {
                  return;
               }

               emit(particle, x + (RNG.nextDouble() - 0.5) * 0.9, y + h + 0.45, z + (RNG.nextDouble() - 0.5) * 0.9, 0.0, -0.02, 0.0);
               break;
            case "helix":
               for (int strand = 0; strand < 2; strand++) {
                  double angx = tick * 0.3 + strand * Math.PI;
                  double hyx = tick * 0.045 % h;
                  if (!firstPerson || !(hyx > 1.0)) {
                     emit(particle, x + Math.cos(angx) * 0.7, y + hyx, z + Math.sin(angx) * 0.7, 0.0, 0.0, 0.0);
                  }
               }
               break;
            default: {
               double ang = RNG.nextDouble() * Math.PI * 2.0;
               double r = 0.45 + RNG.nextDouble() * 0.35;
               double hy = RNG.nextDouble() * (firstPerson ? 0.9 : h);
               emit(particle, x + Math.cos(ang) * r, y + 0.1 + hy, z + Math.sin(ang) * r, 0.0, 0.015, 0.0);
            }
         }
      }
   }

   /** v24: exclusive (VIP / Legend / Admin / Owner) cosmetics sparkle subtly so their rarity is visible. */
   private static void rarityShimmer(AbstractClientPlayer p) {
      int tier = 0;
      for (String id : CosmeticManager.loadout(p.getUUID()).values()) {
         if (id == null) continue;
         if (id.startsWith("owner_")) tier = Math.max(tier, 4);
         else if (id.startsWith("admin_")) tier = Math.max(tier, 3);
         else if (id.startsWith("legend_")) tier = Math.max(tier, 2);
         else if (id.startsWith("vip_")) tier = Math.max(tier, 1);
      }
      if (tier == 0) return;
      int color = switch (tier) { case 4 -> 0xC77DFF; case 3 -> 0xFF4D5E; case 2 -> 0xFFD36B; default -> 0xFFE9A8; };
      double ang = RNG.nextDouble() * Math.PI * 2.0;
      double r = 0.35 + RNG.nextDouble() * 0.35;
      double hy = 0.6 + RNG.nextDouble() * (p.getBbHeight() - 0.4);
      emit(new DustParticleOptions(color, 0.55F), p.getX() + Math.cos(ang) * r, p.getY() + hy, p.getZ() + Math.sin(ang) * r, 0.0, 0.01, 0.0);
      if (tier >= 3 && RNG.nextInt(3) == 0) emit(ParticleTypes.END_ROD, p.getX() + Math.cos(ang + 1) * r, p.getY() + hy, p.getZ() + Math.sin(ang + 1) * r, 0.0, 0.005, 0.0);
   }

   private static void emit(ParticleOptions p, double x, double y, double z, double dx, double dy, double dz) {
      Hooks.aura = true;
      Minecraft.getInstance().particleEngine.createParticle(p, x, y, z, dx, dy, dz);
   }

   static ParticleOptions particle(String spec) {
      if (spec.startsWith("dust:")) {
         String c = spec.substring(5);
         int rgb = "rainbow".equals(c) ? NameDecorator.hsv(tick / 80.0, 0.75, 1.0) : Integer.parseInt(c.replace("#", ""), 16);
         return new DustParticleOptions(rgb, 1.0F);
      } else {
         return switch (spec) {
            case "heart" -> ParticleTypes.HEART;
            case "flame" -> ParticleTypes.FLAME;
            case "soul_fire_flame" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "end_rod" -> ParticleTypes.END_ROD;
            case "snowflake" -> ParticleTypes.SNOWFLAKE;
            case "note" -> ParticleTypes.NOTE;
            case "enchant" -> ParticleTypes.ENCHANT;
            case "totem_of_undying" -> ParticleTypes.TOTEM_OF_UNDYING;
            case "electric_spark" -> ParticleTypes.ELECTRIC_SPARK;
            case "glow" -> ParticleTypes.GLOW;
            case "happy_villager" -> ParticleTypes.HAPPY_VILLAGER;
            case "portal" -> ParticleTypes.PORTAL;
            default -> null;
         };
      }
   }
}
