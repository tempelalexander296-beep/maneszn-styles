package com.maneszn.styles.image;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.maneszn.styles.ModPaths;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.style.Category;
import com.maneszn.styles.style.StyleRegistry;
import com.maneszn.styles.style.Target;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import javax.imageio.ImageIO;

public final class StyleImporter {
   private StyleImporter() {
   }

   public static StyleImporter.Result importImage(Path image, Category category, Target target, String name, String author) throws IOException {
      BufferedImage src;
      try (InputStream in = Files.newInputStream(image)) {
         src = ImageIO.read(in);
      }

      if (src == null) {
         throw new IOException("Kein lesbares Bild: " + image.getFileName());
      } else {
         Target ref = target != null ? target : category.previewTargetOrFirst();
         Category.Kind kind = ref.kindOr(category.kind());
         ImageProcessor.Img out = ImageProcessor.process(src, kind, ref.width(), ref.height(), ModConfig.get().importResolution);
         String id = uniqueId(category.id(), StyleRegistry.sanitize(name));
         Path dir = ModPaths.customDir().resolve(category.id()).resolve(id);
         Files.createDirectories(dir);
         String fileName = target != null ? target.name() + ".png" : "texture.png";
         ImageIO.write(out.toBuffered(), "png", dir.resolve(fileName).toFile());
         if (kind == Category.Kind.SKY) {
            ImageProcessor.Img north = ImageProcessor.resizeArea(ImageProcessor.skyboxNorth(out), 128, 128);
            ImageIO.write(north.toBuffered(), "png", dir.resolve("preview.png").toFile());
         }

         JsonObject meta = new JsonObject();
         meta.addProperty("name", name);
         meta.addProperty("author", author);
         meta.addProperty("date", LocalDate.now().toString());
         meta.addProperty("source_file", image.getFileName().toString());
         if (target != null) {
            JsonArray a = new JsonArray();
            a.add(target.name());
            meta.add("apply_to", a);
         }

         Files.writeString(dir.resolve("style.json"), ModConfig.GSON.toJson(meta), StandardCharsets.UTF_8);
         return new StyleImporter.Result(id, dir, out.w(), out.h());
      }
   }

   private static String uniqueId(String categoryId, String base) {
      String id = base;
      int n = 2;

      while (Files.exists(ModPaths.customDir().resolve(categoryId).resolve(id)) || StyleRegistry.get().style(categoryId, id) != null || id.equals("vanilla")) {
         id = base + "_" + n++;
      }

      return id;
   }

   public record Result(String styleId, Path dir, int width, int height) {
   }
}
