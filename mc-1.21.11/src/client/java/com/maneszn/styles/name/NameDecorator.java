package com.maneszn.styles.name;

import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.cosmetic.Cosmetic;
import com.maneszn.styles.cosmetic.CosmeticManager;
import com.maneszn.styles.net.ApiClient;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription.Resource;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.contents.PlainTextContents.LiteralContents;

public final class NameDecorator {
   public static final String GLYPH = "\ue000";
   private static MutableComponent glyph;
   private static final FontDescription LOGO_FONT = new Resource(ManesznStyles.id("logo"));

   private NameDecorator() {
   }

   public static MutableComponent logo() {
      if (glyph == null) {
         glyph = Component.literal("\ue000")
            .withStyle(s -> s.withFont(new Resource(ManesznStyles.id("logo"))).withColor(16777215).withBold(false).withItalic(false));
      }

      return Component.empty().append(glyph).append(Component.literal(" "));
   }

   public static MutableComponent logo(UUID uuid) {
      Cosmetic lg = uuid == null ? null : CosmeticManager.get(uuid, Cosmetic.Type.LOGO);
      return lg == null ? logo() : logo(lg);
   }

   public static MutableComponent logo(Cosmetic lg) {
      return lg != null && lg.text() != null && !lg.text().isEmpty() && !"none".equals(lg.text())
         ? Component.empty()
            .append(
               Component.literal(lg.text())
                  .withStyle(s -> s.withFont(new Resource(ManesznStyles.id("logo"))).withColor(16777215).withBold(false).withItalic(false))
            )
            .append(Component.literal(" "))
         : logo();
   }

   private static boolean hasLogo(Component c) {
      return c.visit((style, text) -> !text.isEmpty() && LOGO_FONT.equals(style.getFont()) ? Optional.of(Boolean.TRUE) : Optional.empty(), Style.EMPTY)
         .isPresent();
   }

   public static Component decorate(UUID uuid, Component name, String plainName) {
      return decorate(uuid, name, plainName, false);
   }

   public static Component decorate(UUID uuid, Component name, String plainName, boolean nametag) {
      if (name != null && uuid != null) {
         ApiClient api = ApiClient.get();
         if (!api.isModUser(uuid)) {
            return name;
         } else if (hasLogo(name)) {
            return name;
         } else {
            Cosmetic color = CosmeticManager.get(uuid, Cosmetic.Type.NAMECOLOR);
            Cosmetic tag = nametag ? CosmeticManager.get(uuid, Cosmetic.Type.NAMETAG) : null;
            Component colored = tag != null ? NameEffects.apply(name, plainName, color, tag.text(), uuid) : colorize(name, plainName, color);
            Cosmetic frame = CosmeticManager.get(uuid, Cosmetic.Type.FRAME);
            if (frame != null) {
               colored = frame(colored, frame);
            }

            return (Component)(!ModConfig.get().showLogo ? colored : logo(uuid).append(colored));
         }
      } else {
         return name;
      }
   }

   public static Component frame(Component name, Cosmetic frame) {
      if (frame != null && frame.text() != null) {
         String[] lr = frame.text().split("\\|", -1);
         String l = lr.length > 0 ? lr[0] : "";
         String r = lr.length > 1 ? lr[1] : "";
         int col = frame.colors().isEmpty() ? 16777215 : colorAt(frame.colors(), 0.0, 0);
         MutableComponent out = Component.empty();
         if (!l.isEmpty()) {
            out.append(Component.literal(l + " ").withStyle(s -> s.withColor(col)));
         }

         out.append(name);
         if (!r.isEmpty()) {
            out.append(Component.literal(" " + r).withStyle(s -> s.withColor(col)));
         }

         return out;
      } else {
         return name;
      }
   }

   public static Component colorize(Component name, String plainName, Cosmetic color) {
      if (color != null && !color.colors().isEmpty()) {
         List<String> cols = color.colors();
         boolean simple = name.getSiblings().isEmpty() && plainName != null && !plainName.isEmpty() && name.getString().equals(plainName);
         boolean multi = cols.size() > 1 || "rainbow".equals(cols.get(0)) || cols.get(0).startsWith("@");
         if (multi && simple) {
            Style base = name.getStyle();
            MutableComponent out = Component.empty();
            int[] cps = plainName.codePoints().toArray();

            for (int i = 0; i < cps.length; i++) {
               int c = colorAt(cols, cps.length <= 1 ? 0.0 : (double)i / (cps.length - 1), i);
               out.append(Component.literal(new String(Character.toChars(cps[i]))).setStyle(base.withColor(c)));
            }

            return out;
         } else {
            int c = colorAt(cols, 0.0, 0);
            return Component.empty().withStyle(s -> s.withColor(c)).append(name);
         }
      } else {
         return name;
      }
   }

   public static int colorAt(List<String> cols, double t, int index) {
      String mode = cols.get(0);
      if (mode.startsWith("@")) {
         return animated(mode.substring(1), cols, t, index);
      } else if ("rainbow".equals(mode)) {
         float hue = (float)((CosmeticManager.ticks() / 60.0 + index * 0.07) % 1.0);
         return hsv(hue, 0.6, 1.0);
      } else if (cols.size() == 1) {
         return hex(cols.get(0));
      } else {
         double p = t * (cols.size() - 1);
         int i = Math.min(cols.size() - 2, (int)Math.floor(p));
         double f = p - i;
         int a = hex(cols.get(i));
         int b = hex(cols.get(i + 1));
         int r = (int)Math.round((a >> 16 & 0xFF) * (1.0 - f) + (b >> 16 & 0xFF) * f);
         int g = (int)Math.round((a >> 8 & 0xFF) * (1.0 - f) + (b >> 8 & 0xFF) * f);
         int bl = (int)Math.round((a & 0xFF) * (1.0 - f) + (b & 0xFF) * f);
         return r << 16 | g << 8 | bl;
      }
   }

   static int animated(String mode, List<String> cols, double t, int index) {
      double now = CosmeticManager.ticks();
      int base = cols.size() > 1 ? hex(cols.get(1)) : 11691775;
      switch (mode) {
         case "gradient":
            List<String> c = cols.subList(1, cols.size());
            if (c.isEmpty()) {
               return base;
            }

            double p = (t + now * 0.02) % 1.0;
            double q = p < 0.5 ? p * 2.0 : 2.0 - p * 2.0;
            return c.size() == 1 ? hex(c.get(0)) : colorAt(c, q, index);
         case "metallic":
            int tint = cols.size() > 1 ? base : 13160668;
            double shine = 0.5 + 0.5 * Math.sin(index * 0.7 - now * 0.18);
            return mixRgb(mixRgb(tint, 5922928, 0.45), 16777215, shine * 0.85);
         case "fade": {
            double k = 0.5 + 0.5 * Math.sin(now * 0.12);
            return mixRgb(mixRgb(base, 0, 0.65), base, k);
         }
         case "shimmer":
            double pos = now * 0.35 % 24.0 - 6.0;
            double d = Math.abs(index - pos);
            return mixRgb(base, 16777215, Math.max(0.0, 1.0 - d / 2.2));
         case "blink":
            int other = cols.size() > 2 ? hex(cols.get(2)) : 16777215;
            return (long)(now / 10.0) % 2L == 0L ? base : other;
         case "chromatic":
            float hue = (float)((now / 25.0 + index * 0.15) % 1.0);
            return hsv(hue, 0.95, 1.0);
         case "fire":
            double n = Math.sin(index * 12.9898 + Math.floor(now / 2.0) * 78.233) * 43758.5453;
            n -= Math.floor(n);
            return n < 0.33 ? 16726815 : (n < 0.66 ? 16751135 : 16769357);
         case "glow": {
            double k = 0.5 + 0.5 * Math.sin(now * 0.15);
            return mixRgb(base, 16777215, k * 0.6);
         }
         default:
            return base;
      }
   }

   static int mixRgb(int a, int b, double f) {
      f = Math.max(0.0, Math.min(1.0, f));
      int r = (int)Math.round((a >> 16 & 0xFF) * (1.0 - f) + (b >> 16 & 0xFF) * f);
      int g = (int)Math.round((a >> 8 & 0xFF) * (1.0 - f) + (b >> 8 & 0xFF) * f);
      int bl = (int)Math.round((a & 0xFF) * (1.0 - f) + (b & 0xFF) * f);
      return r << 16 | g << 8 | bl;
   }

   public static int hsv(double h, double s, double v) {
      double hh = (h % 1.0 + 1.0) % 1.0 * 6.0;
      int i = (int)Math.floor(hh);
      double f = hh - i;
      double p = v * (1.0 - s);
      double q = v * (1.0 - s * f);
      double t = v * (1.0 - s * (1.0 - f));
      double r;
      double g;
      double b;
      switch (i) {
         case 0:
            r = v;
            g = t;
            b = p;
            break;
         case 1:
            r = q;
            g = v;
            b = p;
            break;
         case 2:
            r = p;
            g = v;
            b = t;
            break;
         case 3:
            r = p;
            g = q;
            b = v;
            break;
         case 4:
            r = t;
            g = p;
            b = v;
            break;
         default:
            r = v;
            g = p;
            b = q;
      }

      return (int)Math.round(r * 255.0) << 16 | (int)Math.round(g * 255.0) << 8 | (int)Math.round(b * 255.0);
   }

   static int hex(String s) {
      try {
         return Integer.parseInt(s.startsWith("#") ? s.substring(1) : s, 16) & 16777215;
      } catch (NumberFormatException var2) {
         return 16777215;
      }
   }

   public static Component decorateChat(Component message) {
      if (message != null && ModConfig.get().showLogo) {
         Map<String, UUID> names = new HashMap<>(ApiClient.get().namesIndex());
         Minecraft mc = Minecraft.getInstance();
         if (mc.getUser() != null) {
            names.put(mc.getUser().getName().toLowerCase(Locale.ROOT), ApiClient.selfId());
         }

         if (names.isEmpty()) {
            return message;
         } else {
            String all = message.getString();
            if (hasLogo(message)) {
               return message;
            } else {
               boolean any = false;
               String lower = all.toLowerCase(Locale.ROOT);

               for (String n : names.keySet()) {
                  if (n.length() >= 3 && lower.contains(n)) {
                     any = true;
                     break;
                  }
               }

               if (!any) {
                  return message;
               } else {
                  try {
                     boolean[] done = new boolean[]{false};
                     return rewrite(message, names, done);
                  } catch (RuntimeException var8) {
                     return message;
                  }
               }
            }
         }
      } else {
         return message;
      }
   }

   private static Component rewrite(Component c, Map<String, UUID> names, boolean[] done) {
      if (done[0]) {
         return c;
      } else {
         ComponentContents contents = c.getContents();
         MutableComponent out;
         if (contents instanceof TranslatableContents tc) {
            Object[] args = tc.getArgs();
            Object[] na = new Object[args.length];

            for (int i = 0; i < args.length; i++) {
               Object a = args[i];
               if (a instanceof Component ac) {
                  na[i] = rewrite(ac, names, done);
               } else if (a instanceof String s && !done[0]) {
                  na[i] = literalWithName(s, names, done, Style.EMPTY);
               } else {
                  na[i] = a;
               }
            }

            out = Component.translatableWithFallback(tc.getKey(), tc.getFallback(), na);
         } else if (contents instanceof LiteralContents lit && !done[0]) {
            out = literalWithName(lit.text(), names, done, Style.EMPTY) instanceof MutableComponent m ? m : MutableComponent.create(contents);
         } else {
            out = MutableComponent.create(contents);
         }

         out.setStyle(c.getStyle());

         for (Component sib : c.getSiblings()) {
            out.append(rewrite(sib, names, done));
         }

         return out;
      }
   }

   private static Component literalWithName(String text, Map<String, UUID> names, boolean[] done, Style style) {
      String lower = text.toLowerCase(Locale.ROOT);
      int best = -1;
      String bestName = null;

      for (String n : names.keySet()) {
         if (n.length() >= 3) {
            int i = indexOfWord(lower, n);
            if (i >= 0 && (best < 0 || i < best)) {
               best = i;
               bestName = n;
            }
         }
      }

      if (best < 0) {
         return Component.literal(text).setStyle(style);
      } else {
         done[0] = true;
         UUID id = names.get(bestName);
         String original = text.substring(best, best + bestName.length());
         MutableComponent out = Component.empty();
         if (best > 0) {
            out.append(Component.literal(text.substring(0, best)));
         }

         out.append(logo(id));
         Cosmetic color = CosmeticManager.get(id, Cosmetic.Type.NAMECOLOR);
         out.append(colorize(Component.literal(original), original, color));
         if (best + bestName.length() < text.length()) {
            out.append(Component.literal(text.substring(best + bestName.length())));
         }

         return out;
      }
   }

   private static int indexOfWord(String hay, String needle) {
      int from = 0;

      while (true) {
         int i = hay.indexOf(needle, from);
         if (i < 0) {
            return -1;
         }

         boolean left = i == 0 || !isNameChar(hay.charAt(i - 1));
         int end = i + needle.length();
         boolean right = end >= hay.length() || !isNameChar(hay.charAt(end));
         if (left && right) {
            return i;
         }

         from = i + 1;
      }
   }

   private static boolean isNameChar(char ch) {
      return Character.isLetterOrDigit(ch) || ch == '_';
   }
}
