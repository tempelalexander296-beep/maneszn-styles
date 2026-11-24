package com.maneszn.styles.name;

import com.maneszn.styles.cosmetic.Cosmetic;
import com.maneszn.styles.cosmetic.CosmeticManager;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class NameEffects {
   private static final String FLIP_FROM = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
   private static final String FLIP_TO = "ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz∀ꓭƆꓷƎℲ⅁HIſꓘ˥WNOԀΌꓤS⊥∩ΛMX⅄Z0ƖᄅƐㄣϛ9ㄥ86‾";
   private static final String RANDOM_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789#$%&@";

   private NameEffects() {
   }

   public static Component apply(Component name, String plain, Cosmetic color, String effect, UUID uuid) {
      double t = CosmeticManager.ticks();
      boolean simple = name.getSiblings().isEmpty() && plain != null && !plain.isEmpty() && name.getString().equals(plain);
      Component core;
      if (!simple
         || !"typewriter".equals(effect)
            && !"glitch".equals(effect)
            && !"iterate".equals(effect)
            && !"flip".equals(effect)
            && !"skew".equals(effect)
            && !"grow".equals(effect)) {
         core = NameDecorator.colorize(name, plain, color);
      } else {
         core = perChar(name.getStyle(), plain, color, effect, t, uuid);
      }

      String effectId = effect == null ? "none" : effect;
      String l;
      String r;
      int deco;
      switch (effectId) {
         case "sparkle": {
            boolean a = (long)(t / 6.0) % 2L == 0L;
            l = a ? "✦ " : "✧ ";
            r = a ? " ✧" : " ✦";
            deco = (long)(t / 6.0) % 2L == 0L ? 16777215 : 16770698;
            break;
         }
         case "hearts":
            l = "♥ ";
            r = " ♥";
            double pulse = 0.5 + 0.5 * Math.sin(t * 0.25);
            deco = mix(16731528, 16761558, pulse);
            break;
         case "stars": {
            boolean a = (long)(t / 8.0) % 2L == 0L;
            l = a ? "★ " : "☆ ";
            r = a ? " ☆" : " ★";
            deco = 16767053;
            break;
         }
         case "crown":
            l = "♔ ";
            r = "";
            deco = 16763213;
            break;
         default:
            return core;
      }

      MutableComponent out = Component.empty();
      int dc = deco;
      out.append(Component.literal(l).withStyle(s -> s.withColor(dc)));
      out.append(core);
      if (!r.isEmpty()) {
         out.append(Component.literal(r).withStyle(s -> s.withColor(dc)));
      }

      return out;
   }

   private static Component perChar(Style base, String plain, Cosmetic color, String effect, double t, UUID uuid) {
      if ("flip".equals(effect)) {
         long cycle = (long)(t % 100.0);
         if (cycle >= 60L && cycle < 85L) {
            StringBuilder b = new StringBuilder();
            int[] cp = plain.codePoints().toArray();

            for (int i = cp.length - 1; i >= 0; i--) {
               String ch = new String(Character.toChars(cp[i]));
               int k = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_".indexOf(ch);
               b.append(
                  k >= 0 && k < "ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz∀ꓭƆꓷƎℲ⅁HIſꓘ˥WNOԀΌꓤS⊥∩ΛMX⅄Z0ƖᄅƐㄣϛ9ㄥ86‾".length()
                     ? String.valueOf("ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz∀ꓭƆꓷƎℲ⅁HIſꓘ˥WNOԀΌꓤS⊥∩ΛMX⅄Z0ƖᄅƐㄣϛ9ㄥ86‾".charAt(k))
                     : ch
               );
            }

            plain = b.toString();
         }
      }

      int[] cps = plain.codePoints().toArray();
      int n = cps.length;
      int shown = n;
      boolean cursor = false;
      if ("typewriter".equals(effect)) {
         int cycle = n * 3 + 30;
         int k = (int)(t % cycle);
         shown = Math.min(n, k / 3);
         cursor = shown < n || (long)(t / 6.0) % 2L == 0L;
      }

      long seed = (long)(t / 3.0) * 31L + (uuid == null ? 0L : uuid.getLeastSignificantBits());
      boolean glitchNow = "glitch".equals(effect) && Math.floorMod(seed, 7) == 0;
      int glitchIndex = Math.floorMod(seed * 13L, Math.max(1, n));
      List<String> cols = color == null ? List.of() : color.colors();
      MutableComponent out = Component.empty();
      int settled = n;
      if ("iterate".equals(effect)) {
         double cyc = t % 100.0;
         settled = cyc < 50.0 ? (int)(cyc / 50.0 * (n + 1)) : n;
      }

      for (int i = 0; i < shown; i++) {
         Style st = base;
         if (!cols.isEmpty()) {
            st = base.withColor(NameDecorator.colorAt(cols, n <= 1 ? 0.0 : (double)i / (n - 1), i));
         }

         if (glitchNow && i == glitchIndex) {
            st = st.withObfuscated(true).withColor(16731624);
         }

         if ("skew".equals(effect) && Math.sin(t * 0.25 - i * 0.6) > 0.2) {
            st = st.withItalic(true);
         }

         if ("grow".equals(effect) && Math.sin(t * 0.22 - i * 0.55) > 0.3) {
            st = st.withBold(true);
         }

         String ch = new String(Character.toChars(cps[i]));
         if (i >= settled) {
            int k = (int)Math.floorMod((long)(t / 2.0) * 7L + i * 13L, (long)"ABCDEFGHJKLMNPQRSTUVWXYZ0123456789#$%&@".length());
            ch = String.valueOf("ABCDEFGHJKLMNPQRSTUVWXYZ0123456789#$%&@".charAt(k));
         }

         out.append(Component.literal(ch).setStyle(st));
      }

      if (cursor) {
         out.append(Component.literal("_").setStyle(base.withColor(11691775)));
      }

      return out;
   }

   public static double[] motion(UUID uuid) {
      Cosmetic tag = CosmeticManager.get(uuid, Cosmetic.Type.NAMETAG);
      if (tag != null && tag.motion() != null) {
         double t = CosmeticManager.ticks() + (uuid == null ? 0L : uuid.getMostSignificantBits() & 63L);
         String var4 = tag.motion();

         return switch (var4) {
            case "bounce" -> new double[]{0.0, Math.abs(Math.sin(t * 0.22)) * 0.28};
            case "float" -> new double[]{0.0, Math.sin(t * 0.08) * 0.12 + 0.06};
            case "wiggle" -> new double[]{Math.sin(t * 0.6) * 0.04, Math.abs(Math.sin(t * 0.9)) * 0.06};
            case "shake" -> new double[]{(hash(t, 1) - 0.5) * 0.05, (hash(t, 2) - 0.5) * 0.05};
            case "wave" -> new double[]{Math.sin(t * 0.12) * 0.12, Math.sin(t * 0.24) * 0.06};
            default -> null;
         };
      } else {
         return null;
      }
   }

   private static double hash(double t, int salt) {
      double n = Math.sin(Math.floor(t) * 12.9898 + salt * 78.233) * 43758.5453;
      return n - Math.floor(n);
   }

   private static int mix(int a, int b, double f) {
      int r = (int)Math.round((a >> 16 & 0xFF) * (1.0 - f) + (b >> 16 & 0xFF) * f);
      int g = (int)Math.round((a >> 8 & 0xFF) * (1.0 - f) + (b >> 8 & 0xFF) * f);
      int bl = (int)Math.round((a & 0xFF) * (1.0 - f) + (b & 0xFF) * f);
      return r << 16 | g << 8 | bl;
   }
}
