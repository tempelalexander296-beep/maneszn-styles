package com.maneszn.styles.gui;

import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.cosmetic.Cosmetic;
import com.maneszn.styles.cosmetic.CosmeticManager;
import com.maneszn.styles.cosmetic.CosmeticRegistry;
import com.maneszn.styles.cosmetic.CustomCapes;
import com.maneszn.styles.cosmetic.SkinFetcher;
import com.maneszn.styles.image.ImageProcessor;
import com.maneszn.styles.render.CosmeticScenes;
import com.maneszn.styles.render.ModelPreview;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

public final class CosmeticPreviews {
   private static final Map<String, Optional<ImageProcessor.Img>> IMAGES = new HashMap<>();
   private static final CosmeticScenes.Textures TEX = CosmeticPreviews::load;
   private static String inspectorSig;
   private static Optional<TextureCache.Tex> inspectorTex = Optional.empty();
   private static final Map<String, Optional<TextureCache.Tex>> OUTFITS = new HashMap<>();

   private CosmeticPreviews() {
   }

   static ImageProcessor.Img load(String path) {
      return path != null && path.startsWith("dyn/") ? CustomCapes.preview(path) : IMAGES.computeIfAbsent(path, p -> {
         Optional<Path> f = CosmeticRegistry.modPath("assets/maneszn_styles/" + p);
         if (f.isEmpty()) {
            return Optional.empty();
         } else {
            try {
               Optional<ImageProcessor.Img> var4;
               try (InputStream in = Files.newInputStream(f.get())) {
                  BufferedImage b = ImageIO.read(in);
                  var4 = b == null ? Optional.empty() : Optional.of(ImageProcessor.Img.of(b));
               }

               return var4;
            } catch (Exception var7) {
               return Optional.empty();
            }
         }
      }).orElse(null);
   }

   static String look(Map<String, String> loadout) {
      Map<Cosmetic.Type, Integer> t = new EnumMap<>(Cosmetic.Type.class);
      StringBuilder sig = new StringBuilder();
      if (loadout != null) {
         for (Cosmetic.Type type : Cosmetic.Type.values()) {
            int c = CosmeticManager.tint(loadout, type);
            if (c != -1) {
               t.put(type, c);
               sig.append(type.id()).append(Integer.toHexString(c));
            }
         }
      }

      CosmeticScenes.tints = t;
      CosmeticScenes.petOnHead = loadout != null && "head".equals(loadout.get("pet_pos"));
      return sig + (CosmeticScenes.petOnHead ? "h" : "");
   }

   public static Optional<TextureCache.Tex> card(Cosmetic c) {
      if (c == null) {
         return Optional.empty();
      } else {
         look(null);
         String key = "cos_card:" + c.key();
         switch (c.type()) {
            case AURA:
               return TextureCache.image(key, c.icon() == null ? null : load(c.icon() + ".png"));
            case NAMECOLOR:
            case NAMETAG:
            case FRAME:
            case LOGO:
               return Optional.empty();
            case BUDDY:
            case PET:
               ImageProcessor.Img skinImg = null;
               if (c.skin()) {
                  String name = CosmeticManager.skinNameOr(CosmeticManager.extra(CosmeticManager.spotOf(c).skinKey()));
                  SkinFetcher.Skin sk = SkinFetcher.get(name);
                  if (sk == null) {
                     return Optional.empty();
                  }

                  skinImg = sk.image();
                  key = key + ":" + name;
               }

               ImageProcessor.Img img;
               try {
                  img = ModelPreview.render(CosmeticScenes.buddyOnly(c, skinImg, TEX, 0.0), 96, 205.0, false);
               } catch (Exception var8) {
                  img = null;
               }

               return TextureCache.image(key, img);
            default:
               ImageProcessor.Img img2;
               try {
                  Map<Cosmetic.Type, Cosmetic> worn = new EnumMap<>(Cosmetic.Type.class);
                  worn.put(c.type(), c);
                  ModelPreview.Scene scene = CosmeticScenes.figure(worn, List.of(), TEX, 0.0);
                  boolean front = c.type() == Cosmetic.Type.HAT
                     || c.type() == Cosmetic.Type.FACE
                     || c.type() == Cosmetic.Type.SKINFX
                     || c.type() == Cosmetic.Type.AURA
                     || c.type() == Cosmetic.Type.SHOES;
                  double yaw = c.type() == Cosmetic.Type.HIP ? 250.0 : (front ? 205.0 : 25.0);
                  img2 = ModelPreview.render(scene, 96, yaw, false);
               } catch (Exception var9) {
                  ManesznStyles.LOGGER.warn("Cosmetic-Vorschau {} fehlgeschlagen: {}", key, var9.toString());
                  img2 = null;
               }

               return TextureCache.image(key, img2);
         }
      }
   }

   static String selfName() {
      User u = Minecraft.getInstance().getUser();
      return u == null ? null : u.getName();
   }

   static String skinName(CosmeticScenes.BuddySpot spot) {
      String n = ModConfig.get().cosmetics.get(spot.skinKey());
      return n != null && !n.isBlank() ? n : selfName();
   }

   public static Optional<TextureCache.Tex> figure(
      Cosmetic cape, Cosmetic hat, Cosmetic wings, Cosmetic face, Cosmetic back, List<CosmeticManager.BuddyAt> buddies, double yawDeg, double ticks
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

      return figure(worn, buddies, yawDeg, ticks);
   }

   public static Optional<TextureCache.Tex> figure(Map<Cosmetic.Type, Cosmetic> worn, List<CosmeticManager.BuddyAt> buddies, double yawDeg, double ticks) {
      return figure(worn, buddies, yawDeg, ticks, ModConfig.get().cosmetics);
   }

   public static Optional<TextureCache.Tex> figure(
      Map<Cosmetic.Type, Cosmetic> worn, List<CosmeticManager.BuddyAt> buddies, double yawDeg, double ticks, Map<String, String> loadout
   ) {
      String lookSig = look(loadout);
      int yawFrame = Math.floorMod(Math.round(yawDeg / 6.0), 60);
      long anim = (long)(ticks / 2.0);
      List<CosmeticScenes.BuddyEntry> entries = new ArrayList<>();
      StringBuilder bsig = new StringBuilder();

      for (CosmeticManager.BuddyAt b : buddies) {
         ImageProcessor.Img skin = null;
         if (b.buddy().skin()) {
            String name = CosmeticManager.skinNameOr(b.skinName());
            SkinFetcher.Skin sk = SkinFetcher.get(name);
            skin = sk == null ? null : sk.image();
            bsig.append(name).append(skin != null);
         }

         entries.add(new CosmeticScenes.BuddyEntry(b.spot(), b.buddy(), skin));
         bsig.append(b.spot()).append(key(b.buddy())).append(';');
      }

      StringBuilder wsig = new StringBuilder();

      for (Entry<Cosmetic.Type, Cosmetic> e : worn.entrySet()) {
         wsig.append(key(e.getValue())).append('|');
      }

      String sig = wsig + "|" + bsig + "|" + yawFrame + "|" + anim + "|" + lookSig;
      if (sig.equals(inspectorSig)) {
         return inspectorTex;
      } else {
         inspectorSig = sig;

         ImageProcessor.Img img;
         try {
            img = ModelPreview.render(CosmeticScenes.figure(worn, entries, TEX, ticks), 160, yawFrame * 6.0, true);
         } catch (Exception var18) {
            img = null;
         }

         inspectorTex = TextureCache.replaceImage("cos_inspector", img);
         return inspectorTex;
      }
   }

   public static Optional<TextureCache.Tex> loadoutFigure(Map<String, String> loadout, double yawDeg, double ticks) {
      Map<Cosmetic.Type, Cosmetic> worn = new EnumMap<>(Cosmetic.Type.class);
      CosmeticRegistry reg = CosmeticRegistry.get();

      for (Cosmetic.Type t : Cosmetic.Type.values()) {
         Cosmetic c = reg.of(loadout, t);
         if (c != null && t != Cosmetic.Type.BUDDY) {
            worn.put(t, c);
         }
      }

      return figure(worn, CosmeticManager.buddies(loadout), yawDeg, ticks, loadout);
   }

   public static Optional<TextureCache.Tex> outfit(ModConfig.Outfit o) {
      String lookSig = look(o.cosmetics);
      String sig = "outfit:" + o.cosmetics.toString().hashCode() + lookSig;
      Optional<TextureCache.Tex> have = OUTFITS.get(sig);
      if (have != null && have.isPresent()) {
         return have;
      } else {
         Map<Cosmetic.Type, Cosmetic> worn = new EnumMap<>(Cosmetic.Type.class);
         CosmeticRegistry reg = CosmeticRegistry.get();

         for (Cosmetic.Type t : Cosmetic.Type.values()) {
            Cosmetic c = reg.of(o.cosmetics, t);
            if (c != null && t != Cosmetic.Type.BUDDY) {
               worn.put(t, c);
            }
         }

         List<CosmeticScenes.BuddyEntry> entries = new ArrayList<>();
         boolean waiting = false;

         for (CosmeticManager.BuddyAt b : CosmeticManager.buddies(o.cosmetics)) {
            SkinFetcher.Skin sk = SkinFetcher.get(CosmeticManager.skinNameOr(b.skinName()));
            if (sk == null) {
               waiting = true;
            }

            entries.add(new CosmeticScenes.BuddyEntry(b.spot(), b.buddy(), sk == null ? null : sk.image()));
         }

         ImageProcessor.Img img;
         try {
            img = ModelPreview.render(CosmeticScenes.figure(worn, entries, TEX, 0.0), 96, 160.0, false);
         } catch (Exception var11) {
            img = null;
         }

         Optional<TextureCache.Tex> tex = TextureCache.image(sig + (waiting ? ":w" : ""), img);
         if (!waiting) {
            OUTFITS.put(sig, tex);
         }

         return tex;
      }
   }

   public static void clearOutfits() {
      OUTFITS.clear();
   }

   private static String key(Cosmetic c) {
      return c == null ? "-" : c.key();
   }

   public static void clear() {
      IMAGES.clear();
      OUTFITS.clear();
      inspectorSig = null;
   }
}
