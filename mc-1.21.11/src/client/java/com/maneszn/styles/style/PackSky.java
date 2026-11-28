package com.maneszn.styles.style;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Findet den Himmel in einem Texturpack, damit er unter "Himmel" auswählbar ist:
 * OptiFine/MCPatcher ({@code optifine/sky/world0/skyN.properties} + Bild im 3×2-Raster) und
 * FabricSkyboxes/Nuit mit einem einzelnen 3×2-Bild. Gewählt wird die Haupt-Himmelsschicht
 * (blend=replace/alpha bevorzugt, sonst die größte), Sternen-/Wolken-Overlays werden übersprungen.
 */
public final class PackSky {
   private PackSky() {
   }

   public static Optional<Path> find(Path root) {
      try {
         Optional<Path> r = optifine(root, "assets/minecraft/optifine");
         if (r.isEmpty()) {
            r = optifine(root, "assets/minecraft/mcpatcher");
         }

         if (r.isEmpty()) {
            r = skyboxJson(root);
         }

         return r;
      } catch (Exception e) {
         return Optional.empty();
      }
   }

   private static Optional<Path> optifine(Path root, String base) throws Exception {
      Path dir = root.resolve(base + "/sky/world0");
      if (!Files.isDirectory(dir)) {
         return Optional.empty();
      }

      Path best = null;
      long bestScore = -1L;
      List<Path> files;
      try (Stream<Path> s = Files.list(dir)) {
         files = s.sorted().toList();
      }

      for (Path p : files) {
         String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
         if (!n.endsWith(".properties")) {
            continue;
         }

         Properties pr = new Properties();
         try (InputStream in = Files.newInputStream(p)) {
            pr.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
         }

         String src = pr.getProperty("source", "").trim();
         Path img = src.isEmpty() ? dir.resolve(p.getFileName().toString().replaceAll("(?i)\\.properties$", ".png")) : resolveRef(root, base, dir, src);
         long score = score(img, pr.getProperty("blend", "").trim().toLowerCase(Locale.ROOT));
         if (score > bestScore) {
            bestScore = score;
            best = img;
         }
      }

      if (best == null) {
         // ohne .properties: einfach das größte 3×2-Bild im Ordner
         for (Path p : files) {
            if (p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
               long score = score(p, "");
               if (score > bestScore) {
                  bestScore = score;
                  best = p;
               }
            }
         }
      }

      return Optional.ofNullable(best);
   }

   private static Optional<Path> skyboxJson(Path root) throws Exception {
      List<Path> jsons = new ArrayList<>();
      for (String ns : new String[]{"fabricskyboxes", "nuit"}) {
         Path dir = root.resolve("assets/" + ns + "/sky");
         if (Files.isDirectory(dir)) {
            try (Stream<Path> s = Files.list(dir)) {
               s.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().forEach(jsons::add);
            }
         }
      }

      Path best = null;
      long bestScore = -1L;
      for (Path j : jsons) {
         JsonObject o = JsonParser.parseString(Files.readString(j, StandardCharsets.UTF_8)).getAsJsonObject();
         String tex = findTexture(o);
         if (tex != null) {
            Path img = resolveRef(root, "assets/minecraft", j.getParent(), tex);
            long score = score(img, "replace");
            if (score > bestScore) {
               bestScore = score;
               best = img;
            }
         }
      }

      return Optional.ofNullable(best);
   }

   private static String findTexture(JsonObject o) {
      for (String k : new String[]{"texture", "sprite"}) {
         if (o.has(k) && o.get(k).isJsonPrimitive()) {
            return o.get(k).getAsString();
         }
      }

      for (String k : new String[]{"properties", "textures"}) {
         if (o.has(k) && o.get(k).isJsonObject()) {
            String t = findTexture(o.getAsJsonObject(k));
            if (t != null) {
               return t;
            }
         }
      }

      return null;
   }

   /** OptiFine-Pfade: "./x.png" (gleicher Ordner), "~/x.png" (optifine-Ordner), "ns:pfad", "pfad/x.png", "x.png". */
   static Path resolveRef(Path root, String base, Path dir, String ref) {
      String r = ref.replace('\\', '/');
      if (r.startsWith("./")) {
         return dir.resolve(r.substring(2));
      } else if (r.startsWith("~/")) {
         return root.resolve(base + "/" + r.substring(2));
      } else if (r.contains(":")) {
         int i = r.indexOf(':');
         return root.resolve("assets/" + r.substring(0, i) + "/" + r.substring(i + 1));
      } else if (r.startsWith("assets/")) {
         return root.resolve(r);
      } else {
         return r.contains("/") ? root.resolve("assets/minecraft/" + r) : dir.resolve(r);
      }
   }

   /** Nur Bilder im 3×2-Raster zählen; Haupthimmel (replace/alpha/ohne) vor Overlays (add/screen/…), dann nach Größe. */
   private static long score(Path img, String blend) {
      if (img == null || !Files.isRegularFile(img)) {
         return -1L;
      }

      long[] wh = pngSize(img);
      if (wh == null || wh[0] * 2L != wh[1] * 3L) {
         return -1L;
      }

      boolean main = blend.isEmpty() || blend.equals("replace") || blend.equals("alpha");
      return (main ? 1_000_000_000_000L : 0L) + wh[0] * wh[1];
   }

   static long[] pngSize(Path png) {
      try (InputStream in = Files.newInputStream(png)) {
         byte[] h = in.readNBytes(24);
         if (h.length < 24 || (h[1] & 255) != 80 || (h[2] & 255) != 78 || (h[3] & 255) != 71) {
            return null;
         }

         long w = (h[16] & 255L) << 24 | (h[17] & 255L) << 16 | (h[18] & 255L) << 8 | h[19] & 255L;
         long ht = (h[20] & 255L) << 24 | (h[21] & 255L) << 16 | (h[22] & 255L) << 8 | h[23] & 255L;
         return w > 0L && ht > 0L ? new long[]{w, ht} : null;
      } catch (Exception e) {
         return null;
      }
   }
}
