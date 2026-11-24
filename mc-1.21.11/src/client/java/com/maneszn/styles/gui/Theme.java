package com.maneszn.styles.gui;

import com.maneszn.styles.config.ModConfig;

public enum Theme {
   LILA("lila", -5745161, "Lila", "Purple"),
   BLAU("blau", -12877066, "Blau", "Blue"),
   PINK("pink", -1292135, "Pink", "Pink"),
   GRUEN("gruen", -14498466, "Grün", "Green"),
   ROT("rot", -1096636, "Rot", "Red"),
   GOLD("gold", -1395960, "Gold", "Gold"),
   WEIHNACHTEN("weihnachten", -1689274, -13779348, "Weihnachten", "Christmas"),
   HALLOWEEN("halloween", -34278, -6599169, "Halloween", "Halloween"),
   OSTERN("ostern", -546600, -7347280, "Ostern", "Easter");

   public static final int BG = -234092014;
   public static final int PANEL = -15462116;
   public static final int PANEL_2 = -15001818;
   public static final int CARD = -14607314;
   public static final int CARD_HOVER = -14015424;
   public static final int LINE = -13752510;
   public static final int TEXT = -1185282;
   public static final int MUTED = -6646866;
   public static final int DIM = -9739136;
   public static final int GREEN = -14498466;
   public static final int RED = -1096636;
   public static final int STAR = -340971;
   public final String id;
   public final int accent;
   public final int accent2;
   private final String de;
   private final String en;

   private Theme(String id, int accent, String de, String en) {
      this(id, accent, 0, de, en);
   }

   private Theme(String id, int accent, int accent2, String de, String en) {
      this.id = id;
      this.accent = accent;
      this.accent2 = accent2;
      this.de = de;
      this.en = en;
   }

   public boolean holiday() {
      return this.accent2 != 0;
   }

   public String label(String lang) {
      return lang.startsWith("de") ? this.de : this.en;
   }

   public int accentDim() {
      return mix(this.accent, -15462116, 0.72F);
   }

   public int accentAlpha(int alpha) {
      return alpha << 24 | this.accent & 16777215;
   }

   public static Theme current() {
      String t = ModConfig.get().theme;

      for (Theme th : values()) {
         if (th.id.equals(t)) {
            return th;
         }
      }

      return LILA;
   }

   public static int mix(int a, int b, float f) {
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int aa = a >>> 24;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      int ba = b >>> 24;
      int r = (int)(ar + (br - ar) * f);
      int g = (int)(ag + (bg - ag) * f);
      int bl = (int)(ab + (bb - ab) * f);
      int al = (int)(aa + (ba - aa) * f);
      return al << 24 | r << 16 | g << 8 | bl;
   }
}
