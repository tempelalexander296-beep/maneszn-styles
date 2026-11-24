package com.maneszn.styles.style;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.ModPaths;
import com.maneszn.styles.config.ModConfig;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;

public final class StyleRegistry {
   private static final StyleRegistry INSTANCE = new StyleRegistry();
   private final List<Group> groups = new ArrayList<>();
   private final Map<String, Category> categories = new LinkedHashMap<>();
   private final Map<String, List<Style>> styles = new LinkedHashMap<>();
   private final List<FileSystem> openZips = new ArrayList<>();
   private int scannedPacks;

   public static StyleRegistry get() {
      return INSTANCE;
   }

   public synchronized void reload() {
      this.groups.clear();
      this.categories.clear();
      this.styles.clear();
      this.closeZips();
      Optional<Path> builtin = this.builtinRoot();
      if (builtin.isEmpty()) {
         ManesznStyles.LOGGER.error("builtin-Ordner nicht gefunden – keine eingebauten Styles");
      } else {
         this.loadCategories(builtin.get().resolve("categories.json"));

         for (Category c : this.categories.values()) {
            this.styles.put(c.id(), new ArrayList<>());
         }

         this.loadBuiltinStyles(builtin.get());
         this.loadCustomStyles();
         this.scannedPacks = 0;
         if (ModConfig.get().scanResourcePacks) {
            this.loadPackStyles();
         }
      }
   }

   private Optional<Path> builtinRoot() {
      return FabricLoader.getInstance().getModContainer("maneszn_styles").flatMap(c -> c.findPath("assets/maneszn_styles/builtin"));
   }

   private void loadCategories(Path file) {
      try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
         JsonObject root = JsonParser.parseReader(r).getAsJsonObject();

         for (JsonElement ge : root.getAsJsonArray("groups")) {
            JsonObject g = ge.getAsJsonObject();
            this.groups.add(new Group(g.get("id").getAsString(), names(g.getAsJsonObject("name"))));
         }

         for (JsonElement ce : root.getAsJsonArray("categories")) {
            JsonObject c = ce.getAsJsonObject();
            List<Target> targets = new ArrayList<>();

            for (JsonElement te : c.getAsJsonArray("targets")) {
               JsonObject t = te.getAsJsonObject();
               JsonArray size = t.has("size") ? t.getAsJsonArray("size") : null;
               int w = size == null ? 16 : size.get(0).getAsInt();
               int h = size == null ? 16 : size.get(1).getAsInt();
               boolean shared = !t.has("shared") || t.get("shared").getAsBoolean();
               Category.Kind tKind = t.has("kind") ? Category.Kind.valueOf(t.get("kind").getAsString().toUpperCase(Locale.ROOT)) : null;
               List<String> alt = new ArrayList<>();
               if (t.has("alt")) {
                  for (JsonElement a : t.getAsJsonArray("alt")) {
                     alt.add(a.getAsString());
                  }
               }

               targets.add(new Target(t.get("name").getAsString(), t.get("path").getAsString(), w, h, shared, tKind, List.copyOf(alt)));
            }

            Category.Kind kind = Category.Kind.valueOf(c.has("kind") ? c.get("kind").getAsString().toUpperCase(Locale.ROOT) : "ITEM");
            String preview = c.has("preview") ? c.get("preview").getAsString() : targets.get(0).name();
            String preview3d = c.has("preview3d") ? c.get("preview3d").getAsString() : null;
            Map<String, String> faces = c.has("faces") ? names(c.getAsJsonObject("faces")) : Map.of();
            int[] crop = null;
            if (c.has("preview_crop")) {
               JsonArray a = c.getAsJsonArray("preview_crop");
               crop = new int[]{a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt(), a.get(3).getAsInt()};
            }

            List<String> folders = new ArrayList<>();
            if (c.has("include_folders")) {
               for (JsonElement f : c.getAsJsonArray("include_folders")) {
                  folders.add(f.getAsString());
               }
            }

            Category.Extras extras = new Category.Extras(
               crop,
               c.has("extra_files") ? names(c.getAsJsonObject("extra_files")) : Map.of(),
               List.copyOf(folders),
               c.has("armor_layers") ? names(c.getAsJsonObject("armor_layers")) : Map.of(),
               c.has("note") ? names(c.getAsJsonObject("note")) : Map.of()
            );
            Category cat = new Category(
               c.get("id").getAsString(),
               c.get("group").getAsString(),
               names(c.getAsJsonObject("name")),
               kind,
               preview,
               List.copyOf(targets),
               preview3d,
               faces,
               extras
            );
            this.categories.put(cat.id(), cat);
         }
      } catch (Exception var21) {
         ManesznStyles.LOGGER.error("categories.json fehlerhaft", var21);
      }
   }

   private static Map<String, String> names(JsonObject o) {
      Map<String, String> m = new LinkedHashMap<>();
      if (o != null) {
         for (Entry<String, JsonElement> e : o.entrySet()) {
            m.put(e.getKey(), e.getValue().getAsString());
         }
      }

      return m;
   }

   private void loadBuiltinStyles(Path builtin) {
      Path file = builtin.resolve("styles.json");

      try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
         JsonObject root = JsonParser.parseReader(r).getAsJsonObject();

         for (JsonElement se : root.getAsJsonArray("styles")) {
            JsonObject s = se.getAsJsonObject();
            String cat = s.get("category").getAsString();
            String id = s.get("id").getAsString();
            Path dir = builtin.resolve(cat).resolve(id);
            this.addStyle(this.parseStyle(s, cat, id, Style.Source.BUILTIN, null, dir));
         }
      } catch (Exception var13) {
         ManesznStyles.LOGGER.error("styles.json fehlerhaft", var13);
      }
   }

   private void loadCustomStyles() {
      Path custom = ModPaths.customDir();
      if (Files.isDirectory(custom)) {
         try (Stream<Path> cats = Files.list(custom)) {
            for (Path catDir : cats.filter(x$0 -> Files.isDirectory(x$0)).toList()) {
               String cat = catDir.getFileName().toString();
               if (this.categories.containsKey(cat)) {
                  List<Style> found = new ArrayList<>();

                  try (Stream<Path> ids = Files.list(catDir)) {
                     for (Path dir : ids.filter(x$0 -> Files.isDirectory(x$0)).toList()) {
                        Path meta = dir.resolve("style.json");
                        if (Files.isRegularFile(meta)) {
                           try (Reader r = Files.newBufferedReader(meta, StandardCharsets.UTF_8)) {
                              JsonObject s = JsonParser.parseReader(r).getAsJsonObject();
                              found.add(this.parseStyle(s, cat, dir.getFileName().toString(), Style.Source.CUSTOM, null, dir));
                           } catch (Exception var18) {
                              ManesznStyles.LOGGER.warn("Eigener Style {} fehlerhaft: {}", dir, var18.toString());
                           }
                        }
                     }
                  }

                  found.sort(Comparator.comparing(Style::date, Comparator.nullsLast(Comparator.reverseOrder())));
                  found.forEach(this::addStyle);
               }
            }
         } catch (IOException var21) {
            ManesznStyles.LOGGER.error("custom-Ordner konnte nicht gelesen werden", var21);
         }
      }
   }

   private void loadPackStyles() {
      Path dir = ModPaths.resourcePacksDir();
      if (Files.isDirectory(dir)) {
         try (Stream<Path> entries = Files.list(dir)) {
            for (Path p : entries.sorted().toList()) {
               try {
                  String fileName = p.getFileName().toString();
                  Path root;
                  String packName;
                  if (Files.isDirectory(p) && Files.isRegularFile(p.resolve("pack.mcmeta"))) {
                     root = p;
                     packName = fileName;
                  } else {
                     if (!fileName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
                        continue;
                     }

                     FileSystem fs = FileSystems.newFileSystem(p, (ClassLoader)null);
                     this.openZips.add(fs);
                     root = fs.getPath("/");
                     if (!Files.isRegularFile(root.resolve("pack.mcmeta"))) {
                        Path nested = null;

                        try (Stream<Path> sub = Files.list(root)) {
                           for (Path d : sub.toList()) {
                              if (Files.isDirectory(d) && !d.getFileName().toString().startsWith("__MACOSX") && Files.isRegularFile(d.resolve("pack.mcmeta"))) {
                                 nested = d;
                                 break;
                              }
                           }
                        }

                        if (nested == null) {
                           continue;
                        }

                        root = nested;
                     }

                     packName = fileName.substring(0, fileName.length() - 4);
                  }

                  packName = packName.replaceAll("§.", "");
                  LocalDate date = LocalDate.ofInstant(Instant.ofEpochMilli(Files.getLastModifiedTime(p).toMillis()), ZoneId.systemDefault());
                  String id = "pack_" + sanitize(packName);
                  boolean any = false;

                  for (Category c : this.categories.values()) {
                     Style s = new Style(id, packName, c.id(), packName, date, Style.Source.PACK, packName, root, List.of());
                     if (s.providesAnything(c)) {
                        this.addStyle(s);
                        any = true;
                     }
                  }

                  if (any) {
                     this.scannedPacks++;
                  }
               } catch (Exception var18) {
                  ManesznStyles.LOGGER.warn("Pack {} übersprungen: {}", p.getFileName(), var18.toString());
               }
            }
         } catch (IOException var20) {
            ManesznStyles.LOGGER.warn("resourcepacks-Ordner nicht lesbar: {}", var20.toString());
         }
      }
   }

   private Style parseStyle(JsonObject s, String cat, String id, Style.Source src, String pack, Path dir) {
      String name = s.has("name") ? s.get("name").getAsString() : id;
      String author = s.has("author") ? s.get("author").getAsString() : "Maneszn";
      LocalDate date = null;
      if (s.has("date")) {
         try {
            date = LocalDate.parse(s.get("date").getAsString());
         } catch (Exception var13) {
         }
      }

      List<String> applyTo = new ArrayList<>();
      if (s.has("apply_to")) {
         for (JsonElement e : s.getAsJsonArray("apply_to")) {
            applyTo.add(e.getAsString());
         }
      }

      return new Style(id, name, cat, author, date, src, pack, dir, applyTo);
   }

   private void addStyle(Style s) {
      List<Style> list = this.styles.get(s.categoryId());
      if (list == null) {
         ManesznStyles.LOGGER.warn("Style {} gehört zu unbekannter Kategorie", s.key());
      } else if (!"vanilla".equals(s.id())) {
         list.removeIf(o -> o.id().equals(s.id()));
         list.add(s);
      }
   }

   private void closeZips() {
      for (FileSystem fs : this.openZips) {
         try {
            fs.close();
         } catch (IOException var4) {
         }
      }

      this.openZips.clear();
   }

   public static String sanitize(String s) {
      String r = s.toLowerCase(Locale.ROOT)
         .replace("ä", "ae")
         .replace("ö", "oe")
         .replace("ü", "ue")
         .replace("ß", "ss")
         .replaceAll("[^a-z0-9_]+", "_")
         .replaceAll("^_+|_+$", "");
      return r.isEmpty() ? "style" : r;
   }

   public synchronized List<Group> groups() {
      return List.copyOf(this.groups);
   }

   public synchronized List<Category> categories() {
      return List.copyOf(this.categories.values());
   }

   public synchronized Category category(String id) {
      return this.categories.get(id);
   }

   public synchronized List<Style> styles(String categoryId) {
      List<Style> l = this.styles.get(categoryId);
      return l == null ? List.of() : List.copyOf(l);
   }

   public synchronized Style style(String categoryId, String styleId) {
      for (Style s : this.styles(categoryId)) {
         if (s.id().equals(styleId)) {
            return s;
         }
      }

      return null;
   }

   public synchronized List<Category> visibleCategories() {
      List<Category> out = new ArrayList<>();

      for (Category c : this.categories.values()) {
         if (!this.styles(c.id()).isEmpty()) {
            out.add(c);
         }
      }

      return out;
   }

   public synchronized int styleCount() {
      int n = 0;

      for (List<Style> l : this.styles.values()) {
         n += l.size();
      }

      return n;
   }

   public synchronized int scannedPackCount() {
      return this.scannedPacks;
   }
}
