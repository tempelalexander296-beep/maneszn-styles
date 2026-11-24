package com.maneszn.styles.gui;

import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.image.ImageProcessor;
import com.maneszn.styles.render.ModelPreview;
import com.maneszn.styles.render.PreviewScenes;
import com.maneszn.styles.style.Category;
import com.maneszn.styles.style.Style;
import com.maneszn.styles.style.Target;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;

public final class TextureCache {
   private static final Map<String, Optional<TextureCache.Tex>> CACHE = new HashMap<>();
   private static int counter;
   private static final int STILL_SIZE = 128;
   private static final int SPIN_SIZE = 160;
   public static final int SPIN_FRAMES = 36;
   private static final Map<String, Optional<ModelPreview.Scene>> SCENES = new HashMap<>();
   private static String spinKey;

   private TextureCache() {
   }

   public static Optional<TextureCache.Tex> preview(Category c, Style style) {
      String key = style == null ? "vanilla3d:" + c.id() : "style:" + style.key();
      Optional<TextureCache.Tex> cached = CACHE.get(key);
      if (cached != null) {
         return cached;
      } else {
         Optional<TextureCache.Tex> result = scene(c, style).map(sc -> ModelPreview.render(sc, 128, 0.0, false)).flatMap(img -> register(key, png(img)));
         if (result.isEmpty()) {
            ImageProcessor.Img flat = null;

            try {
               flat = PreviewScenes.flat(c, style, VanillaLookup.INSTANCE);
            } catch (Exception var7) {
            }

            if (flat != null) {
               result = register(key, png(flat));
            }
         }

         CACHE.put(key, result);
         return result;
      }
   }

   public static Optional<TextureCache.Tex> icon(Category c, Style style) {
      String key = style == null ? "icon:vanilla:" + c.id() : "icon:" + style.key();
      Optional<TextureCache.Tex> cached = CACHE.get(key);
      if (cached != null) {
         return cached;
      } else {
         Optional<TextureCache.Tex> result = scene(c, style).map(sc -> ModelPreview.render(sc, 32, 0.0, false)).flatMap(img -> register(key, png(img)));
         if (result.isEmpty()) {
            result = preview(c, style);
         }

         CACHE.put(key, result);
         return result;
      }
   }

   public static boolean has3d(Category c, Style style) {
      return scene(c, style).isPresent();
   }

   public static Optional<TextureCache.Tex> spin(Category c, Style style, int frame) {
      String base = "spin:" + (style == null ? "vanilla:" + c.id() : style.key());
      if (!base.equals(spinKey)) {
         for (String k : new ArrayList<>(CACHE.keySet())) {
            if (k.startsWith("spin:")) {
               release(k);
            }
         }

         spinKey = base;
      }

      int f = Math.floorMod(frame, 36);
      String key = base + ":" + f;
      Optional<TextureCache.Tex> cached = CACHE.get(key);
      if (cached != null) {
         return cached;
      } else {
         Optional<TextureCache.Tex> t = scene(c, style).map(sc -> ModelPreview.render(sc, 160, f * 10.0, true)).flatMap(img -> register(key, png(img)));
         CACHE.put(key, t);
         return t;
      }
   }

   private static Optional<ModelPreview.Scene> scene(Category c, Style style) {
      String key = style == null ? "vanilla:" + c.id() : style.key();
      return SCENES.computeIfAbsent(key, k -> {
         try {
            return PreviewScenes.forCategory(c, PreviewScenes.styleLookup(style, c, VanillaLookup.INSTANCE));
         } catch (Exception var4) {
            ManesznStyles.LOGGER.warn("3D-Vorschau für {} fehlgeschlagen: {}", k, var4.toString());
            return Optional.empty();
         }
      });
   }

   private static byte[] png(ImageProcessor.Img img) {
      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         ImageIO.write(img.toBuffered(), "png", bos);
         return bos.toByteArray();
      } catch (Exception var2) {
         return new byte[0];
      }
   }

   public static Optional<TextureCache.Tex> vanilla(Target t) {
      if (t == null) {
         return Optional.empty();
      } else {
         String key = "vanilla:" + t.path();
         return CACHE.computeIfAbsent(
            key,
            k -> {
               try {
                  IoSupplier<InputStream> sup = Minecraft.getInstance()
                     .getVanillaPackResources()
                     .getResource(PackType.CLIENT_RESOURCES, Identifier.parse(t.path()));
                  if (sup == null) {
                     return Optional.empty();
                  } else {
                     Optional var4;
                     try (InputStream in = sup.get()) {
                        var4 = register(k, in.readAllBytes());
                     }

                     return var4;
                  }
               } catch (Exception var8) {
                  return Optional.empty();
               }
            }
         );
      }
   }

   public static Optional<TextureCache.Tex> file(String key, Path p) {
      return CACHE.computeIfAbsent("file:" + key, k -> load(k, p));
   }

   public static Optional<TextureCache.Tex> bytes(String key, byte[] png) {
      String k = "bytes:" + key;
      release(k);
      Optional<TextureCache.Tex> t = register(k, png);
      CACHE.put(k, t);
      return t;
   }

   public static Optional<TextureCache.Tex> image(String key, ImageProcessor.Img img) {
      String k = "img:" + key;
      Optional<TextureCache.Tex> cached = CACHE.get(k);
      if (cached != null) {
         return cached;
      } else {
         Optional<TextureCache.Tex> t = img == null ? Optional.empty() : register(k, png(img));
         CACHE.put(k, t);
         return t;
      }
   }

   public static Optional<TextureCache.Tex> replaceImage(String key, ImageProcessor.Img img) {
      String k = "img:" + key;
      release(k);
      Optional<TextureCache.Tex> t = img == null ? Optional.empty() : register(k, png(img));
      CACHE.put(k, t);
      return t;
   }

   private static Optional<TextureCache.Tex> load(String key, Path p) {
      try {
         return register(key, Files.readAllBytes(p));
      } catch (Exception var3) {
         ManesznStyles.LOGGER.warn("Vorschau {} nicht ladbar: {}", p, var3.toString());
         return Optional.empty();
      }
   }

   private static Optional<TextureCache.Tex> register(String key, byte[] png) {
      try {
         NativeImage img = NativeImage.read(new ByteArrayInputStream(png));
         int w = img.getWidth();
         int h = img.getHeight();
         Identifier id = ManesznStyles.id("preview/t" + counter++);
         DynamicTexture tex = new DynamicTexture(() -> "maneszn_preview " + key, img);
         Minecraft.getInstance().getTextureManager().register(id, tex);
         return Optional.of(new TextureCache.Tex(id, w, h));
      } catch (Exception var7) {
         ManesznStyles.LOGGER.warn("Textur {} fehlerhaft: {}", key, var7.toString());
         return Optional.empty();
      }
   }

   private static void release(String key) {
      Optional<TextureCache.Tex> old = CACHE.remove(key);
      if (old != null && old.isPresent()) {
         Minecraft.getInstance().getTextureManager().release(old.get().id());
      }
   }

   public static void clear() {
      for (Optional<TextureCache.Tex> t : CACHE.values()) {
         t.ifPresent(tex -> Minecraft.getInstance().getTextureManager().release(tex.id()));
      }

      CACHE.clear();
      SCENES.clear();
      spinKey = null;
      VanillaLookup.INSTANCE.clear();
      CosmeticPreviews.clear();
   }

   public record Tex(Identifier id, int width, int height) {
      public int frameHeight() {
         return this.height > this.width && this.height % this.width == 0 ? this.width : this.height;
      }
   }
}
