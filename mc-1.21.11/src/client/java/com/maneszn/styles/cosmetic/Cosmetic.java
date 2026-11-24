package com.maneszn.styles.cosmetic;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public record Cosmetic(
   String id,
   Cosmetic.Type type,
   Map<String, String> names,
   String texture,
   int frames,
   int frametime,
   int[] texSize,
   boolean glow,
   boolean translucent,
   List<Cosmetic.Part> parts,
   String particle,
   String mode,
   int rate,
   String icon,
   List<String> colors,
   boolean skin,
   String motion,
   String text,
   String spot,
   String requires
) {
   public String displayName(String lang) {
      return this.names.getOrDefault(lang, this.names.getOrDefault("en", this.id));
   }

   public String key() {
      return this.type.id() + ":" + this.id;
   }

   public String framePath(int frame) {
      return this.frames > 1 ? this.texture + "_" + Math.floorMod(frame, this.frames) + ".png" : this.texture + ".png";
   }

   public int frameAt(long ticks) {
      return this.frames > 1 ? (int)(ticks / Math.max(1, this.frametime) % this.frames) : 0;
   }

   public record Cube(float[] from, float[] size, float inflate, int[] uv) {
   }

   public record Part(String attach, float[] pivot, float[] rot, String anim, List<Cosmetic.Cube> cubes) {
   }

   public static enum Type {
      CAPE,
      HAT,
      FACE,
      BACK,
      HIP,
      SHOES,
      BELT,
      JEWELRY,
      WINGS,
      AURA,
      BUDDY,
      PET,
      SKINFX,
      KILLFX,
      NAMECOLOR,
      NAMETAG,
      NAMEBG,
      FRAME,
      LOGO;

      public String id() {
         return this.name().toLowerCase(Locale.ROOT);
      }

      public static Cosmetic.Type of(String s) {
         return valueOf(s.toUpperCase(Locale.ROOT));
      }

      public boolean modeled() {
         return this == HAT || this == FACE || this == BACK || this == HIP || this == SHOES || this == BELT || this == JEWELRY || this == WINGS;
      }
   }
}
