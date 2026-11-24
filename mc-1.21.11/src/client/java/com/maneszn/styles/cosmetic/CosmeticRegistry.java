package com.maneszn.styles.cosmetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.maneszn.styles.ManesznStyles;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.fabricmc.loader.api.FabricLoader;

public final class CosmeticRegistry {
   private static final CosmeticRegistry INSTANCE = new CosmeticRegistry();
   private final Map<Cosmetic.Type, Map<String, Cosmetic>> byType = new EnumMap<>(Cosmetic.Type.class);
   private final Map<String, Cosmetic> custom = new LinkedHashMap<>();

   private CosmeticRegistry() {
   }

   public static CosmeticRegistry get() {
      return INSTANCE;
   }

   public synchronized void addCustom(Cosmetic c) {
      this.custom.put(c.key(), c);
      this.byType.computeIfAbsent(c.type(), t -> new LinkedHashMap<>()).put(c.id(), c);
   }

   public synchronized void removeCustom(String capeId) {
      this.custom.remove(Cosmetic.Type.CAPE.id() + ":" + capeId);
      Map<String, Cosmetic> m = this.byType.get(Cosmetic.Type.CAPE);
      if (m != null) {
         m.remove(capeId);
      }
   }

   public synchronized void reload() {
      this.byType.clear();

      for (Cosmetic.Type t : Cosmetic.Type.values()) {
         this.byType.put(t, new LinkedHashMap<>());
      }

      for (Cosmetic c : this.custom.values()) {
         this.byType.get(c.type()).put(c.id(), c);
      }

      Optional<Path> file = modPath("assets/maneszn_styles/builtin/cosmetics.json");
      if (file.isEmpty()) {
         ManesznStyles.LOGGER.error("cosmetics.json nicht gefunden – keine Cosmetics");
      } else {
         try (Reader r = Files.newBufferedReader(file.get(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();

            for (JsonElement e : root.getAsJsonArray("cosmetics")) {
               try {
                  Cosmetic c = parse(e.getAsJsonObject());
                  this.byType.get(c.type()).put(c.id(), c);
               } catch (RuntimeException var8) {
                  ManesznStyles.LOGGER.warn("Cosmetic übersprungen: {}", var8.toString());
               }
            }
         } catch (Exception var10) {
            ManesznStyles.LOGGER.error("cosmetics.json fehlerhaft", var10);
         }
      }
   }

   public static Optional<Path> modPath(String path) {
      return FabricLoader.getInstance().getModContainer("maneszn_styles").flatMap(c -> c.findPath(path));
   }

   public static Cosmetic parse(JsonObject o) {
      Map<String, String> names = new LinkedHashMap<>();
      if (o.has("names")) {
         for (Entry<String, JsonElement> n : o.getAsJsonObject("names").entrySet()) {
            names.put(n.getKey(), n.getValue().getAsString());
         }
      }

      List<Cosmetic.Part> parts = new ArrayList<>();
      if (o.has("parts")) {
         for (JsonElement pe : o.getAsJsonArray("parts")) {
            JsonObject p = pe.getAsJsonObject();
            List<Cosmetic.Cube> cubes = new ArrayList<>();

            for (JsonElement ce : p.getAsJsonArray("cubes")) {
               JsonObject c = ce.getAsJsonObject();
               JsonArray uv = c.getAsJsonArray("uv");
               cubes.add(
                  new Cosmetic.Cube(
                     floats(c.getAsJsonArray("from")),
                     floats(c.getAsJsonArray("size")),
                     c.has("inflate") ? c.get("inflate").getAsFloat() : 0.0F,
                     new int[]{uv.get(0).getAsInt(), uv.get(1).getAsInt()}
                  )
               );
            }

            parts.add(
               new Cosmetic.Part(
                  str(p, "attach", "head"), floats(p.getAsJsonArray("pivot")), floats(p.getAsJsonArray("rot")), str(p, "anim", "none"), List.copyOf(cubes)
               )
            );
         }
      }

      List<String> colors = new ArrayList<>();
      if (o.has("colors")) {
         for (JsonElement c : o.getAsJsonArray("colors")) {
            colors.add(c.getAsString());
         }
      }

      int[] texSize = new int[]{64, 32};
      if (o.has("tex_size")) {
         JsonArray a = o.getAsJsonArray("tex_size");
         texSize = new int[]{a.get(0).getAsInt(), a.get(1).getAsInt()};
      }

      return new Cosmetic(
         o.get("id").getAsString(),
         Cosmetic.Type.of(o.get("type").getAsString()),
         names,
         str(o, "texture", null),
         o.has("frames") ? o.get("frames").getAsInt() : 1,
         o.has("frametime") ? o.get("frametime").getAsInt() : 1,
         texSize,
         o.has("glow") && o.get("glow").getAsBoolean(),
         o.has("translucent") && o.get("translucent").getAsBoolean(),
         List.copyOf(parts),
         str(o, "particle", null),
         str(o, "mode", "around"),
         o.has("rate") ? o.get("rate").getAsInt() : 4,
         str(o, "icon", null),
         List.copyOf(colors),
         o.has("skin") && o.get("skin").getAsBoolean(),
         str(o, "motion", "none"),
         str(o, "text", "none"),
         str(o, "spot", null),
         str(o, "requires", null)
      );
   }

   private static String str(JsonObject o, String k, String def) {
      return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : def;
   }

   private static float[] floats(JsonArray a) {
      float[] f = new float[3];
      if (a != null) {
         for (int i = 0; i < Math.min(3, a.size()); i++) {
            f[i] = a.get(i).getAsFloat();
         }
      }

      return f;
   }

   public synchronized List<Cosmetic> all() {
      List<Cosmetic> out = new ArrayList<>();

      for (Map<String, Cosmetic> m : this.byType.values()) {
         out.addAll(m.values());
      }

      return out;
   }

   public synchronized List<Cosmetic> list(Cosmetic.Type type) {
      Map<String, Cosmetic> m = this.byType.get(type);
      return m == null ? List.of() : List.copyOf(m.values());
   }

   public synchronized Cosmetic get(Cosmetic.Type type, String id) {
      if (id == null) {
         return null;
      } else {
         Map<String, Cosmetic> m = this.byType.get(type);
         return m == null ? null : m.get(id);
      }
   }

   public Cosmetic of(Map<String, String> loadout, Cosmetic.Type type) {
      return loadout == null ? null : this.get(type, loadout.get(type.id()));
   }

   public synchronized int count() {
      int n = 0;

      for (Map<String, Cosmetic> m : this.byType.values()) {
         n += m.size();
      }

      return n;
   }
}
