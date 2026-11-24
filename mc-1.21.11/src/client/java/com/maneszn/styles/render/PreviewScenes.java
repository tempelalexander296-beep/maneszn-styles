package com.maneszn.styles.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.maneszn.styles.image.ImageProcessor;
import com.maneszn.styles.style.Category;
import com.maneszn.styles.style.Style;
import com.maneszn.styles.style.Target;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;

public final class PreviewScenes {
   private PreviewScenes() {
   }

   public static Optional<ModelPreview.Scene> forCategory(Category c, ModelPreview.Lookup l) {
      String mode = c.preview3d() == null ? defaultMode(c) : c.preview3d();

      try {
         switch (mode) {
            case "cube":
               ImageProcessor.Img side = tex(l, c, c.faceTarget("side"));
               ImageProcessor.Img up = tex(l, c, c.faceTarget("up"));
               ImageProcessor.Img down = tex(l, c, c.faceTarget("down"));
               if (side == null && up == null) {
                  return Optional.empty();
               }

               if (side == null) {
                  side = up;
               }

               if (up == null) {
                  up = side;
               }

               if (down == null) {
                  down = up;
               }

               return Optional.of(ModelPreview.cube(up, down, side, side, side, side));
            case "crystal":
               Target ent = null;

               for (Target t : c.targets()) {
                  if (t.path().contains("textures/entity/")) {
                     ent = t;
                  }
               }

               ImageProcessor.Img e = ent == null ? null : tex(l, c, ent);
               if (e != null) {
                  return Optional.of(ModelPreview.endCrystal(e));
               }

               return itemScene(c, l);
            case "armor":
               Map<String, String> layers = c.extras() == null ? Map.of() : c.extras().armorLayers();
               ImageProcessor.Img outer = tex(l, c, layers.containsKey("outer") ? c.target(layers.get("outer")) : null);
               ImageProcessor.Img inner = tex(l, c, layers.containsKey("inner") ? c.target(layers.get("inner")) : null);
               if (outer == null && inner == null) {
                  return itemScene(c, l);
               }

               return Optional.of(ModelPreview.armor(outer, inner));
            case "item":
               return itemScene(c, l);
            default:
               return Optional.empty();
         }
      } catch (RuntimeException var8) {
         return Optional.empty();
      }
   }

   private static Optional<ModelPreview.Scene> itemScene(Category c, ModelPreview.Lookup l) {
      Target t = c.previewTargetOrFirst();
      if (t != null && t.path().contains("textures/item/")) {
         String id = t.pathInNamespace().substring("textures/item/".length()).replace(".png", "");
         return ModelPreview.item(l, id).filter(s -> !s.isEmpty());
      } else {
         return Optional.empty();
      }
   }

   private static String defaultMode(Category c) {
      return switch (c.kind()) {
         case BLOCK -> "cube";
         case ITEM -> "item";
         default -> "flat";
      };
   }

   private static ImageProcessor.Img tex(ModelPreview.Lookup l, Category c, Target t) {
      return t == null ? null : l.texture(t.namespace(), t.pathInNamespace());
   }

   public static ImageProcessor.Img flat(Category c, Style s, ModelPreview.Lookup l) {
      ImageProcessor.Img img = null;
      if (s != null) {
         Optional<Path> f = s.previewFile(c);
         if (f.isPresent()) {
            img = read(f.get());
         }
      } else {
         Target t = c.previewTargetOrFirst();
         if (t != null) {
            img = l.texture(t.namespace(), t.pathInNamespace());
         }
      }

      if (img == null) {
         return null;
      } else {
         boolean ownPreview = s != null && s.source() != Style.Source.PACK && Files.isRegularFile(s.root().resolve("preview.png"));
         int[] crop = c.extras() == null ? null : c.extras().previewCrop();
         Target tx = c.previewTargetOrFirst();
         if (crop != null && tx != null && !ownPreview) {
            double k = (double)img.w() / tx.width();
            int x = (int)(crop[0] * k);
            int y = (int)(crop[1] * k);
            int w = Math.min(img.w() - x, (int)Math.ceil(crop[2] * k));
            int h = Math.min(img.h() - y, (int)Math.ceil(crop[3] * k));
            if (w > 0 && h > 0) {
               int[] px = new int[w * h];

               for (int yy = 0; yy < h; yy++) {
                  System.arraycopy(img.px(), (y + yy) * img.w() + x, px, yy * w, w);
               }

               return new ImageProcessor.Img(w, h, px);
            }
         }

         return ModelPreview.firstFrame(img);
      }
   }

   public static ModelPreview.Lookup styleLookup(Style s, Category c, ModelPreview.Lookup fallback) {
      return new ModelPreview.Lookup() {
         @Override
         public JsonObject json(String ns, String path) {
            if (s != null) {
               Path p = s.root().resolve("assets/" + ns + "/" + path);
               if (Files.isRegularFile(p)) {
                  try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                     JsonElement e = JsonParser.parseReader(r);
                     if (e.isJsonObject()) {
                        return e.getAsJsonObject();
                     }

                     return fallback == null ? null : fallback.json(ns, path);
                  } catch (Exception var9) {
                  }
               }
            }

            return fallback == null ? null : fallback.json(ns, path);
         }

         @Override
         public ImageProcessor.Img texture(String ns, String path) {
            if (s != null) {
               ImageProcessor.Img img = PreviewScenes.read(s.root().resolve("assets/" + ns + "/" + path));
               if (img != null) {
                  return img;
               }

               for (Target t : c.targets()) {
                  if (t.namespace().equals(ns) && t.pathInNamespace().equals(path)) {
                     Optional<Path> f = s.resolve(t);
                     if (f.isPresent()) {
                        img = PreviewScenes.read(f.get());
                        if (img != null) {
                           return img;
                        }
                     }
                  }
               }
            }

            return fallback == null ? null : fallback.texture(ns, path);
         }
      };
   }

   public static ImageProcessor.Img read(Path p) {
      try {
         if (!Files.isRegularFile(p)) {
            return null;
         } else {
            ImageProcessor.Img var2;
            try (InputStream in = Files.newInputStream(p)) {
               var2 = decode(in);
            }

            return var2;
         }
      } catch (Exception var6) {
         return null;
      }
   }

   public static ImageProcessor.Img decode(InputStream in) throws IOException {
      BufferedImage b = ImageIO.read(in);
      return b == null ? null : ImageProcessor.Img.of(b);
   }
}
