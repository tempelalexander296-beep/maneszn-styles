package com.maneszn.styles.render;

import com.maneszn.styles.cosmetic.Cosmetic;
import com.maneszn.styles.image.ImageProcessor;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class CosmeticScenes {
   private static final ImageProcessor.Img SKIN = new ImageProcessor.Img(1, 1, new int[]{-12898224});
   private static final ImageProcessor.Img SKIN_DARK = new ImageProcessor.Img(1, 1, new int[]{-14015936});
   private static final ImageProcessor.Img EYE = new ImageProcessor.Img(1, 1, new int[]{-3562753});
   public static Map<Cosmetic.Type, Integer> tints = Map.of();
   public static boolean petOnHead;
   private static final Map<String, ImageProcessor.Img> TINTED = new HashMap<>();
   public static final float PET_SCALE = 1.4F;

   private CosmeticScenes() {
   }

   public static ModelPreview.Scene figure(Cosmetic cape, Cosmetic hat, Cosmetic wings, CosmeticScenes.Textures tex, double ticks) {
      return figure(cape, hat, wings, null, null, List.of(), tex, ticks);
   }

   public static ModelPreview.Scene figure(
      Cosmetic cape,
      Cosmetic hat,
      Cosmetic wings,
      Cosmetic face,
      Cosmetic back,
      Cosmetic buddy,
      CosmeticScenes.BuddySpot spot,
      ImageProcessor.Img buddySkin,
      CosmeticScenes.Textures tex,
      double ticks
   ) {
      return figure(cape, hat, wings, face, back, buddy == null ? List.of() : List.of(new CosmeticScenes.BuddyEntry(spot, buddy, buddySkin)), tex, ticks);
   }

   public static ModelPreview.Scene figure(
      Cosmetic cape,
      Cosmetic hat,
      Cosmetic wings,
      Cosmetic face,
      Cosmetic back,
      List<CosmeticScenes.BuddyEntry> buddies,
      CosmeticScenes.Textures tex,
      double ticks
   ) {
      Map<Cosmetic.Type, Cosmetic> worn = new EnumMap<>(Cosmetic.Type.class);
      if (cape != null) {
         worn.put(Cosmetic.Type.CAPE, cape);
      }

      if (hat != null) {
         worn.put(Cosmetic.Type.HAT, hat);
      }

      if (wings != null) {
         worn.put(Cosmetic.Type.WINGS, wings);
      }

      if (face != null) {
         worn.put(Cosmetic.Type.FACE, face);
      }

      if (back != null) {
         worn.put(Cosmetic.Type.BACK, back);
      }

      return figure(worn, buddies, tex, ticks);
   }

   public static ModelPreview.Scene figure(
      Map<Cosmetic.Type, Cosmetic> worn, List<CosmeticScenes.BuddyEntry> buddies, CosmeticScenes.Textures tex, double ticks
   ) {
      Cosmetic cape = worn.get(Cosmetic.Type.CAPE);
      Cosmetic hat = worn.get(Cosmetic.Type.HAT);
      Cosmetic wings = worn.get(Cosmetic.Type.WINGS);
      Cosmetic face = worn.get(Cosmetic.Type.FACE);
      Cosmetic back = worn.get(Cosmetic.Type.BACK);
      Cosmetic hip = worn.get(Cosmetic.Type.HIP);
      Cosmetic aura = worn.get(Cosmetic.Type.AURA);
      Cosmetic pet = worn.get(Cosmetic.Type.PET);
      Cosmetic fx = worn.get(Cosmetic.Type.SKINFX);
      ImageProcessor.Img skin = SKIN;
      ImageProcessor.Img skinDark = SKIN_DARK;
      if (fx != null) {
         boolean glint = fx.text() != null && fx.text().startsWith("glint");
         boolean glow = fx.text() != null && fx.text().endsWith("glow");
         double sh = 0.5 + 0.5 * Math.sin(ticks * 0.15);
         int base = glow ? -9807216 : -12898224;
         int glintCol = -4616961;
         if (fx.text() != null && fx.text().startsWith("tex:")) {
            glint = true;

            try {
               glintCol = 0xFF000000 | Integer.parseInt(fx.text().substring(5), 16);
            } catch (RuntimeException var31) {
            }
         }

         int c = glint ? mixRgb(base, glintCol, 0.25 + 0.35 * sh) : base;
         skin = new ImageProcessor.Img(1, 1, new int[]{c});
         skinDark = new ImageProcessor.Img(1, 1, new int[]{mixRgb(c, -16777216, 0.25)});
      }

      ModelPreview.Scene s = new ModelPreview.Scene();
      s.noCull = true;
      colorBox(s, skin, -4.0, -8.0, -4.0, 8.0, 8.0, 8.0);
      colorBox(s, skinDark, -4.0, 0.0, -2.0, 8.0, 12.0, 4.0);
      boolean holdsHand = false;

      for (CosmeticScenes.BuddyEntry b : buddies) {
         holdsHand |= b.buddy() != null && b.spot() == CosmeticScenes.BuddySpot.HAND;
      }

      int armStart = s.faces.size();
      colorBox(s, skin, -8.0, 0.0, -2.0, 4.0, 12.0, 4.0);
      if (holdsHand) {
         for (int i = armStart; i < s.faces.size(); i++) {
            for (double[] p : s.faces.get(i).pos()) {
               p[0] += 5.0;
               p[1] -= 2.0;
               ModelPreview.rotateAxis(p, 2, 0.3);
               p[0] -= 5.0;
               p[1] += 2.0;
            }
         }
      }

      colorBox(s, skin, 4.0, 0.0, -2.0, 4.0, 12.0, 4.0);
      colorBox(s, skinDark, -4.0, 12.0, -2.0, 4.0, 12.0, 4.0);
      colorBox(s, skinDark, 0.0, 12.0, -2.0, 4.0, 12.0, 4.0);
      colorBox(s, EYE, -3.0, -4.0, -4.15, 2.0, 1.0, 0.1);
      colorBox(s, EYE, 1.0, -4.0, -4.15, 2.0, 1.0, 0.1);
      if (cape != null) {
         addCape(s, cape, tex, ticks);
      }

      if (hat != null) {
         addModel(s, hat, tex, ticks);
      }

      if (face != null) {
         addModel(s, face, tex, ticks);
      }

      if (back != null) {
         addModel(s, back, tex, ticks);
      }

      if (wings != null) {
         addModel(s, wings, tex, ticks);
      }

      if (hip != null) {
         addModel(s, hip, tex, ticks);
      }

      for (Cosmetic.Type extra : new Cosmetic.Type[]{Cosmetic.Type.SHOES, Cosmetic.Type.BELT, Cosmetic.Type.JEWELRY}) {
         Cosmetic c = worn.get(extra);
         if (c != null) {
            addModel(s, c, tex, ticks);
         }
      }

      if (aura != null && !aura.parts().isEmpty()) {
         addModel(s, aura, tex, ticks);
      }

      if (pet != null) {
         addPet(s, pet, tex, ticks);
      }

      for (CosmeticScenes.BuddyEntry b : buddies) {
         if (b.buddy() != null) {
            addBuddy(s, b.buddy(), b.spot(), hat != null && !hat.glow(), b.skin(), tex, ticks);
         }
      }

      double minY = 1.0E9;
      double maxY = -1.0E9;

      for (ModelPreview.Face f : s.faces) {
         for (double[] p : f.pos()) {
            p[0] = -p[0];
            p[1] = -p[1];
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
         }
      }

      double cy = (minY + maxY) / 2.0;

      for (ModelPreview.Face f : s.faces) {
         for (double[] p : f.pos()) {
            p[1] -= cy;
         }
      }

      return s;
   }

   private static void addCape(ModelPreview.Scene s, Cosmetic cape, CosmeticScenes.Textures tex, double ticks) {
      ImageProcessor.Img img = tinted(tex.apply(cape.framePath(cape.frameAt((long)ticks))), Cosmetic.Type.CAPE);
      if (img != null) {
         double ku = img.w() / 64.0;
         double kv = img.h() / 32.0;
         int start = s.faces.size();
         cube(s, img, ku, kv, 0, 0, -5.0, 0.0, -1.0, 10.0, 16.0, 1.0, 0.0, true);

         for (int i = start; i < s.faces.size(); i++) {
            for (double[] p : s.faces.get(i).pos()) {
               ModelPreview.rotateAxis(p, 1, Math.PI);
               ModelPreview.rotateAxis(p, 0, 0.16);
               p[2] += 2.05;
            }
         }
      }
   }

   public static ModelPreview.Scene buddyOnly(Cosmetic buddy, ImageProcessor.Img skin, CosmeticScenes.Textures tex, double ticks) {
      ModelPreview.Scene s = new ModelPreview.Scene();
      s.noCull = true;
      addBuddyRaw(s, buddy, skin, tex, ticks);
      double minY = 1.0E9;
      double maxY = -1.0E9;

      for (ModelPreview.Face f : s.faces) {
         for (double[] p : f.pos()) {
            p[0] = -p[0];
            p[1] = -p[1];
            minY = Math.min(minY, p[1]);
            maxY = Math.max(maxY, p[1]);
         }
      }

      double cy = (minY + maxY) / 2.0;

      for (ModelPreview.Face f : s.faces) {
         for (double[] p : f.pos()) {
            p[1] -= cy;
         }
      }

      return s;
   }

   private static void addBuddyRaw(ModelPreview.Scene s, Cosmetic buddy, ImageProcessor.Img skin, CosmeticScenes.Textures tex, double ticks) {
      CosmeticScenes.BuddySpot spot = buddy.spot() == null ? CosmeticScenes.BuddySpot.LEFT_SHOULDER : CosmeticScenes.BuddySpot.of(buddy.spot());
      addBuddyRaw(s, buddy, spot, MiniPlayer.Pose.of(spot), skin, tex, ticks);
   }

   private static void addBuddyRaw(
      ModelPreview.Scene s, Cosmetic buddy, CosmeticScenes.BuddySpot spot, ImageProcessor.Img skin, CosmeticScenes.Textures tex, double ticks
   ) {
      addBuddyRaw(s, buddy, spot, MiniPlayer.Pose.of(spot), skin, tex, ticks);
   }

   private static void addBuddyRaw(
      ModelPreview.Scene s,
      Cosmetic buddy,
      CosmeticScenes.BuddySpot spot,
      MiniPlayer.Pose pose,
      ImageProcessor.Img skin,
      CosmeticScenes.Textures tex,
      double ticks
   ) {
      int start = s.faces.size();
      float scale = buddyScale(spot, buddy.skin());
      if (buddy.skin()) {
         if (skin == null) {
            return;
         }

         for (MiniPlayer.Box b : MiniPlayer.boxes(MiniPlayer.isSlim(skin))) {
            int before = s.faces.size();
            cube(s, skin, skin.w() / 64.0, skin.h() / 64.0, b.u(), b.v(), b.x(), b.y(), b.z(), b.w(), b.h(), b.d(), b.inflate(), true);
            float[] r = MiniPlayer.rotation(b.part(), ticks, pose, false);

            for (int i = before; i < s.faces.size(); i++) {
               for (double[] p : s.faces.get(i).pos()) {
                  p[0] -= b.px();
                  p[1] -= b.py();
                  p[2] -= b.pz();
                  ModelPreview.rotateAxis(p, 0, r[0]);
                  ModelPreview.rotateAxis(p, 1, r[1]);
                  ModelPreview.rotateAxis(p, 2, r[2]);
                  p[0] += b.px();
                  p[1] += b.py();
                  p[2] += b.pz();
               }
            }
         }

         float shift = MiniPlayer.shiftY(pose);
         float sw = MiniPlayer.swing(pose, ticks);

         for (int i = start; i < s.faces.size(); i++) {
            for (double[] p : s.faces.get(i).pos()) {
               p[1] += shift;
               if (sw != 0.0F) {
                  ModelPreview.rotateAxis(p, 2, sw);
               }
            }
         }
      } else {
         addModel(s, buddy, tex, ticks);
      }

      if (scale != 1.0F) {
         for (int i = start; i < s.faces.size(); i++) {
            for (double[] px : s.faces.get(i).pos()) {
               for (int a = 0; a < 3; a++) {
                  px[a] *= scale;
               }
            }
         }
      }
   }

   private static void addBuddy(
      ModelPreview.Scene s, Cosmetic buddy, CosmeticScenes.BuddySpot spot, boolean hatOn, ImageProcessor.Img skin, CosmeticScenes.Textures tex, double ticks
   ) {
      int start = s.faces.size();
      addBuddyRaw(s, buddy, spot, skin, tex, ticks);
      float[] off = buddyOffset(spot, hatOn);

      for (int i = start; i < s.faces.size(); i++) {
         for (double[] p : s.faces.get(i).pos()) {
            ModelPreview.rotateAxis(p, 1, off[3]);
            p[0] += off[0];
            p[1] += off[1];
            p[2] += off[2];
            if (spot == CosmeticScenes.BuddySpot.ARM) {
               p[0] += -5.0;
               p[1] += 2.0;
            }
         }
      }
   }

   private static void addPet(ModelPreview.Scene s, Cosmetic pet, CosmeticScenes.Textures tex, double ticks) {
      int start = s.faces.size();
      addModel(s, pet, tex, ticks);
      if (petOnHead) {
         for (int i = start; i < s.faces.size(); i++) {
            for (double[] p : s.faces.get(i).pos()) {
               for (int a = 0; a < 3; a++) {
                  p[a] *= 0.8;
               }

               p[1] += -8.05;
               p[2] += 0.5;
            }
         }
      } else {
         for (int i = start; i < s.faces.size(); i++) {
            for (double[] p : s.faces.get(i).pos()) {
               for (int a = 0; a < 3; a++) {
                  p[a] *= 1.4F;
               }

               ModelPreview.rotateAxis(p, 1, 0.35);
               p[0] += 11.0;
               p[1] += 24.0;
               p[2] += 5.0;
            }
         }
      }
   }

   static ImageProcessor.Img tinted(ImageProcessor.Img img, Cosmetic.Type type) {
      Integer t = tints.get(type);
      if (img != null && t != null && t != -1) {
         String key = System.identityHashCode(img) + ":" + Integer.toHexString(t);
         if (TINTED.size() > 256) {
            TINTED.clear();
         }

         return TINTED.computeIfAbsent(key, k -> {
            int[] px = (int[])img.px().clone();
            int tr = t >> 16 & 0xFF;
            int tg = t >> 8 & 0xFF;
            int tb = t & 0xFF;

            for (int i = 0; i < px.length; i++) {
               int c = px[i];
               int r = (c >> 16 & 0xFF) * tr / 255;
               int g = (c >> 8 & 0xFF) * tg / 255;
               int b = (c & 0xFF) * tb / 255;
               px[i] = c & 0xFF000000 | r << 16 | g << 8 | b;
            }

            return new ImageProcessor.Img(img.w(), img.h(), px);
         });
      } else {
         return img;
      }
   }

   static int mixRgb(int a, int b, double f) {
      int r = (int)Math.round((a >> 16 & 0xFF) * (1.0 - f) + (b >> 16 & 0xFF) * f);
      int g = (int)Math.round((a >> 8 & 0xFF) * (1.0 - f) + (b >> 8 & 0xFF) * f);
      int bl = (int)Math.round((a & 0xFF) * (1.0 - f) + (b & 0xFF) * f);
      return 0xFF000000 | r << 16 | g << 8 | bl;
   }

   private static void addModel(ModelPreview.Scene s, Cosmetic c, CosmeticScenes.Textures tex, double ticks) {
      ImageProcessor.Img img = tinted(tex.apply(c.texture() + ".png"), c.type());
      if (img != null) {
         double ku = (double)img.w() / c.texSize()[0];
         double kv = (double)img.h() / c.texSize()[1];

         for (Cosmetic.Part part : c.parts()) {
            float[] rot = animated(part, ticks);
            float[] pv = (float[])animatedPivot(part, ticks).clone();
            if ("leg_r".equals(part.attach())) {
               pv[0]--;
               pv[1] += 12.0F;
            } else if ("leg_l".equals(part.attach())) {
               pv[0]++;
               pv[1] += 12.0F;
            }

            int start = s.faces.size();

            for (Cosmetic.Cube cb : part.cubes()) {
               cube(
                  s,
                  img,
                  ku,
                  kv,
                  cb.uv()[0],
                  cb.uv()[1],
                  cb.from()[0],
                  cb.from()[1],
                  cb.from()[2],
                  cb.size()[0],
                  cb.size()[1],
                  cb.size()[2],
                  cb.inflate(),
                  !c.glow()
               );
            }

            for (int i = start; i < s.faces.size(); i++) {
               for (double[] p : s.faces.get(i).pos()) {
                  ModelPreview.rotateAxis(p, 0, rot[0]);
                  ModelPreview.rotateAxis(p, 1, rot[1]);
                  ModelPreview.rotateAxis(p, 2, rot[2]);
                  p[0] += pv[0];
                  p[1] += pv[1];
                  p[2] += pv[2];
               }
            }
         }
      }
   }

   public static float[] animated(Cosmetic.Part part, double ticks) {
      float[] r = (float[])part.rot().clone();
      String var6 = part.anim();
      switch (var6) {
         case "flap_left":
            r[1] += (float)(Math.sin(ticks * 0.16) * 0.22);
            break;
         case "flap_right":
            r[1] -= (float)(Math.sin(ticks * 0.16) * 0.22);
            break;
         case "flap_small_left":
            r[2] -= (float)(Math.max(0.0, Math.sin(ticks * 0.35)) * 0.6);
            break;
         case "flap_small_right":
            r[2] += (float)(Math.max(0.0, Math.sin(ticks * 0.35)) * 0.6);
            break;
         case "buzz_left":
            r[2] -= (float)(Math.sin(ticks * 2.2) * 0.6 + 0.3);
            break;
         case "buzz_right":
            r[2] += (float)(Math.sin(ticks * 2.2) * 0.6 + 0.3);
            break;
         case "spin":
         case "spin_bob":
         case "orbit":
            r[1] += (float)(ticks * 0.05 % (Math.PI * 2));
            break;
         case "spin_slow":
            r[1] += (float)(ticks * 0.02 % (Math.PI * 2));
            break;
         case "spin_fast":
            r[1] += (float)(ticks * 0.16 % (Math.PI * 2));
            break;
         case "spin_rev":
            r[1] -= (float)(ticks * 0.03 % (Math.PI * 2));
            break;
         case "look":
            r[1] += (float)(Math.sin(ticks * 0.03) * 0.35 + Math.sin(ticks * 0.011) * 0.2);
            break;
         case "wag":
            r[1] += (float)(Math.sin(ticks * 0.5) * 0.5);
            break;
         case "wag_slow":
            r[2] += (float)(Math.sin(ticks * 0.12) * 0.12);
      }

      return r;
   }

   public static float[] animatedPivot(Cosmetic.Part part, double ticks) {
      float[] p = (float[])part.pivot().clone();
      String var4 = part.anim();
      switch (var4) {
         case "spin_bob":
            p[1] += (float)(Math.sin(ticks * 0.08) * 0.6);
            break;
         case "orbit":
            p[1] += (float)(Math.sin(ticks * 0.09 + part.rot()[1] * 2.0F) * 1.2);
            break;
         case "breathe":
            p[1] += (float)(Math.sin(ticks * 0.1) * 0.15);
            break;
         case "hover":
            p[1] += (float)(Math.sin(ticks * 0.15) * 0.8 - 1.5);
            break;
         case "bounce":
            p[1] -= (float)(Math.abs(Math.sin(ticks * 0.15)) * 1.6);
      }

      return p;
   }

   public static float[] buddyOffset(CosmeticScenes.BuddySpot spot, boolean hatOn) {
      return switch (spot) {
         case HEAD -> new float[]{0.0F, hatOn ? -12.5F : -8.6F, 0.5F, 0.0F};
         case LEFT_SHOULDER -> new float[]{6.5F, -0.05F, 0.3F, 0.0F};
         case RIGHT_SHOULDER -> new float[]{-6.5F, -0.05F, 0.3F, 0.0F};
         case ARM -> new float[]{-1.0F, 4.5F, -2.6F, 0.0F};
         case HAND -> new float[]{-11.5F, 24.0F, 0.5F, 0.0F};
      };
   }

   public static float buddyScale(CosmeticScenes.BuddySpot spot, boolean mini) {
      if (spot == CosmeticScenes.BuddySpot.HAND) {
         return mini ? 0.5F : 1.25F;
      } else {
         return mini ? 0.32F : 1.0F;
      }
   }

   private static void colorBox(ModelPreview.Scene s, ImageProcessor.Img color, double x, double y, double z, double w, double h, double d) {
      cube(s, color, 0.0, 0.0, 0, 0, x, y, z, w, h, d, 0.0, true);
   }

   static void cube(
      ModelPreview.Scene s,
      ImageProcessor.Img img,
      double ku,
      double kv,
      int u,
      int v,
      double x,
      double y,
      double z,
      double w,
      double h,
      double d,
      double grow,
      boolean shade
   ) {
      double x0 = x - grow;
      double y0 = y - grow;
      double z0 = z - grow;
      double x1 = x + w + grow;
      double y1 = y + h + grow;
      double z1 = z + d + grow;
      double[][] vt = new double[][]{{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
      double iw = Math.ceil(w);
      double ih = Math.ceil(h);
      double id = Math.ceil(d);
      double f6 = u;
      double f7 = u + id;
      double f8 = u + id + iw;
      double f9 = u + id + iw + iw;
      double f10 = u + id + iw + id;
      double f11 = u + id + iw + id + iw;
      double f12 = v;
      double f13 = v + id;
      double f14 = v + id + ih;
      face(s, img, ku, kv, vt, new int[]{5, 4, 0, 1}, f7, f12, f8, f13, shade);
      face(s, img, ku, kv, vt, new int[]{2, 3, 7, 6}, f8, f13, f9, f12, shade);
      face(s, img, ku, kv, vt, new int[]{0, 4, 7, 3}, f6, f13, f7, f14, shade);
      face(s, img, ku, kv, vt, new int[]{1, 0, 3, 2}, f7, f13, f8, f14, shade);
      face(s, img, ku, kv, vt, new int[]{5, 1, 2, 6}, f8, f13, f10, f14, shade);
      face(s, img, ku, kv, vt, new int[]{4, 5, 6, 7}, f10, f13, f11, f14, shade);
   }

   private static void face(
      ModelPreview.Scene s, ImageProcessor.Img img, double ku, double kv, double[][] vt, int[] idx, double u1, double v1, double u2, double v2, boolean shade
   ) {
      double[][] pos = new double[4][];

      for (int i = 0; i < 4; i++) {
         pos[i] = (double[])vt[idx[i]].clone();
      }

      double[][] uv = new double[][]{{u2 * ku, v1 * kv}, {u1 * ku, v1 * kv}, {u1 * ku, v2 * kv}, {u2 * ku, v2 * kv}};
      if (img.w() == 1 && img.h() == 1) {
         uv = new double[4][2];
      }

      if (!(Math.abs(u2 - u1) < 1.0E-6) && !(Math.abs(v2 - v1) < 1.0E-6)) {
         s.faces.add(new ModelPreview.Face(pos, insetUv(uv), img, shade));
      }
   }

   private static double[][] insetUv(double[][] uv) {
      double cu = 0.0;
      double cv = 0.0;

      for (double[] p : uv) {
         cu += p[0] / 4.0;
         cv += p[1] / 4.0;
      }

      for (double[] p : uv) {
         p[0] += Math.signum(cu - p[0]) * 0.01;
         p[1] += Math.signum(cv - p[1]) * 0.01;
      }

      return uv;
   }

   public record BuddyEntry(CosmeticScenes.BuddySpot spot, Cosmetic buddy, ImageProcessor.Img skin) {
   }

   public static enum BuddySpot {
      HEAD,
      LEFT_SHOULDER,
      RIGHT_SHOULDER,
      ARM,
      HAND;

      public static CosmeticScenes.BuddySpot of(String s) {
         if (s == null) {
            return LEFT_SHOULDER;
         } else {
            return switch (s) {
               case "head", "kopf" -> HEAD;
               case "right_shoulder", "schulter_rechts" -> RIGHT_SHOULDER;
               case "arm" -> ARM;
               case "hand" -> HAND;
               default -> LEFT_SHOULDER;
            };
         }
      }

      public String id() {
         return switch (this) {
            case HEAD -> "head";
            case LEFT_SHOULDER -> "left_shoulder";
            case RIGHT_SHOULDER -> "right_shoulder";
            case ARM -> "arm";
            case HAND -> "hand";
         };
      }

      public String key() {
         return "buddy_" + this.id();
      }

      public String skinKey() {
         return "skin_" + this.id();
      }
   }

   public interface Textures extends Function<String, ImageProcessor.Img> {
   }
}
