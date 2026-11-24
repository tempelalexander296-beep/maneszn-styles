package com.maneszn.styles.preset;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.ModPaths;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.cosmetic.Cosmetic;
import com.maneszn.styles.cosmetic.CosmeticRegistry;
import com.maneszn.styles.net.ApiClient;
import com.maneszn.styles.style.StyleRegistry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public final class PresetManager {
   public static final String CODE_PREFIX = "MSZ1-";
   private static final PresetManager INSTANCE = new PresetManager();
   private final List<PresetManager.Preset> presets = new ArrayList<>();

   public static PresetManager get() {
      return INSTANCE;
   }

   public synchronized void reload() {
      this.presets.clear();
      Path dir = ModPaths.presetsDir();
      if (Files.isDirectory(dir)) {
         try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.filter(px -> px.toString().endsWith(".json")).toList()) {
               try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                  PresetManager.Preset p = (PresetManager.Preset)ModConfig.GSON.fromJson(r, PresetManager.Preset.class);
                  if (p != null && p.name != null) {
                     if (p.styles == null) {
                        p.styles = new LinkedHashMap<>();
                     }

                     if (p.cosmetics == null) {
                        p.cosmetics = new LinkedHashMap<>();
                     }

                     this.presets.add(p);
                  }
               } catch (Exception var11) {
                  ManesznStyles.LOGGER.warn("Preset {} fehlerhaft: {}", f.getFileName(), var11.toString());
               }
            }
         } catch (IOException var13) {
            ManesznStyles.LOGGER.error("presets-Ordner nicht lesbar", var13);
         }

         this.presets.sort(Comparator.comparing(px -> px.name.toLowerCase()));
      }
   }

   public synchronized List<PresetManager.Preset> presets() {
      return List.copyOf(this.presets);
   }

   public synchronized PresetManager.Preset saveCurrent(String name) throws IOException {
      PresetManager.Preset p = new PresetManager.Preset();
      p.name = name.trim();
      p.created = LocalDate.now().toString();
      p.styles.putAll(ModConfig.get().active);
      p.cosmetics.putAll(ModConfig.get().cosmetics);
      Files.writeString(file(p.name), ModConfig.GSON.toJson(p), StandardCharsets.UTF_8);
      this.reload();
      return p;
   }

   public synchronized void delete(PresetManager.Preset p) throws IOException {
      Files.deleteIfExists(file(p.name));
      this.reload();
   }

   public int apply(PresetManager.Preset p) {
      ModConfig cfg = ModConfig.get();
      cfg.active.clear();
      int skipped = 0;

      for (Entry<String, String> e : p.styles.entrySet()) {
         String cat = e.getKey();
         String style = e.getValue();
         if (!"vanilla".equals(style)) {
            if (StyleRegistry.get().style(cat, style) != null) {
               cfg.active.put(cat, style);
            } else {
               skipped++;
            }
         }
      }

      if (p.cosmetics != null && !p.cosmetics.isEmpty()) {
         cfg.cosmetics.clear();

         for (Entry<String, String> ex : p.cosmetics.entrySet()) {
            if (!ex.getKey().startsWith("buddy_") && !ex.getKey().startsWith("skin_")) {
               try {
                  Cosmetic.Type t = Cosmetic.Type.of(ex.getKey());
                  if (CosmeticRegistry.get().get(t, ex.getValue()) != null) {
                     cfg.cosmetics.put(t.id(), ex.getValue());
                  } else {
                     skipped++;
                  }
               } catch (IllegalArgumentException var8) {
                  skipped++;
               }
            } else {
               cfg.cosmetics.put(ex.getKey(), ex.getValue());
            }
         }

         ApiClient.get().markCosmeticsDirty();
      }

      ModConfig.save();
      return skipped;
   }

   private static Path file(String name) {
      return ModPaths.presetsDir().resolve(StyleRegistry.sanitize(name) + ".json");
   }

   public static String toCode(PresetManager.Preset p) {
      JsonObject o = new JsonObject();
      o.addProperty("n", p.name);
      JsonObject s = new JsonObject();
      p.styles.forEach(s::addProperty);
      o.add("s", s);
      if (!p.cosmetics.isEmpty()) {
         JsonObject c = new JsonObject();
         p.cosmetics.forEach(c::addProperty);
         o.add("c", c);
      }

      byte[] raw = o.toString().getBytes(StandardCharsets.UTF_8);
      Deflater d = new Deflater(9);
      d.setInput(raw);
      d.finish();
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      byte[] buf = new byte[512];

      while (!d.finished()) {
         bos.write(buf, 0, d.deflate(buf));
      }

      d.end();
      return "MSZ1-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bos.toByteArray());
   }

   public static PresetManager.Preset fromCode(String code) throws IOException {
      String c = code.trim();
      if (!c.startsWith("MSZ1-")) {
         throw new IOException("Kein Maneszn-Code");
      } else {
         try {
            byte[] data = Base64.getUrlDecoder().decode(c.substring("MSZ1-".length()));
            Inflater inf = new Inflater();
            inf.setInput(data);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[512];

            while (!inf.finished()) {
               int n = inf.inflate(buf);
               if (n == 0 && (inf.needsInput() || inf.needsDictionary())) {
                  break;
               }

               bos.write(buf, 0, n);
               if (bos.size() > 64000) {
                  throw new IOException("Code zu groß");
               }
            }

            inf.end();
            JsonObject o = JsonParser.parseString(bos.toString(StandardCharsets.UTF_8)).getAsJsonObject();
            PresetManager.Preset p = new PresetManager.Preset();
            p.name = o.has("n") ? o.get("n").getAsString() : "Geteilt";
            p.created = LocalDate.now().toString();
            if (o.has("s")) {
               for (Entry<String, JsonElement> e : o.getAsJsonObject("s").entrySet()) {
                  p.styles.put(e.getKey(), e.getValue().getAsString());
               }
            }

            if (o.has("c")) {
               for (Entry<String, JsonElement> e : o.getAsJsonObject("c").entrySet()) {
                  p.cosmetics.put(e.getKey(), e.getValue().getAsString());
               }
            }

            return p;
         } catch (RuntimeException | DataFormatException var10) {
            throw new IOException("Code ungültig", var10);
         }
      }
   }

   public static final class Preset {
      public String name;
      public String created;
      public Map<String, String> styles = new LinkedHashMap<>();
      public Map<String, String> cosmetics = new LinkedHashMap<>();
   }
}
