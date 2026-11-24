package com.maneszn.styles.style;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class Style {
   public static final String VANILLA = "vanilla";
   private final String id;
   private final String name;
   private final String categoryId;
   private final String author;
   private final LocalDate date;
   private final Style.Source source;
   private final String packName;
   private final Path root;
   private final List<String> applyTo;

   public Style(String id, String name, String categoryId, String author, LocalDate date, Style.Source source, String packName, Path root, List<String> applyTo) {
      this.id = id;
      this.name = name;
      this.categoryId = categoryId;
      this.author = author;
      this.date = date;
      this.source = source;
      this.packName = packName;
      this.root = root;
      this.applyTo = applyTo == null ? List.of() : List.copyOf(applyTo);
   }

   public String id() {
      return this.id;
   }

   public String name() {
      return this.name;
   }

   public String categoryId() {
      return this.categoryId;
   }

   public String author() {
      return this.author;
   }

   public LocalDate date() {
      return this.date;
   }

   public Style.Source source() {
      return this.source;
   }

   public String packName() {
      return this.packName;
   }

   public Path root() {
      return this.root;
   }

   public String key() {
      return this.categoryId + "/" + this.id;
   }

   public boolean isNew() {
      return this.date != null && !this.date.isBefore(LocalDate.now().minusDays(14L));
   }

   public String sourceLabel(String lang) {
      return switch (this.source) {
         case BUILTIN -> this.author != null && !this.author.isBlank() ? this.author : "Maneszn";
         case CUSTOM -> lang.startsWith("de") ? "Eigenes Bild" : "Own image";
         case PACK -> this.packName;
      };
   }

   public Optional<Path> resolve(Target t) {
      try {
         if (this.source == Style.Source.PACK) {
            Path p = this.root.resolve(t.packPath());
            if (Files.isRegularFile(p)) {
               return Optional.of(p);
            }

            for (String alt : t.altPackPaths()) {
               Path a = this.root.resolve(alt);
               if (Files.isRegularFile(a)) {
                  return Optional.of(a);
               }
            }

            return Optional.empty();
         }

         String path = t.path();
         int dot = path.lastIndexOf(46);
         String ext = dot > path.lastIndexOf(47) && dot >= 0 ? path.substring(dot) : ".png";
         Path own = this.root.resolve(t.name() + ext);
         if (Files.isRegularFile(own)) {
            return Optional.of(own);
         }

         if (ext.equals(".png") && t.shared() && (this.applyTo.isEmpty() || this.applyTo.contains(t.name()))) {
            Path shared = this.root.resolve("texture.png");
            if (Files.isRegularFile(shared)) {
               return Optional.of(shared);
            }
         }
      } catch (Exception var7) {
      }

      return Optional.empty();
   }

   public Optional<Path> previewFile(Category c) {
      if (this.source != Style.Source.PACK) {
         Path p = this.root.resolve("preview.png");
         if (Files.isRegularFile(p)) {
            return Optional.of(p);
         }
      }

      Target t = c.previewTargetOrFirst();
      Optional<Path> r = t == null ? Optional.empty() : this.resolve(t);
      if (r.isPresent()) {
         return r;
      } else {
         for (Target other : c.targets()) {
            r = this.resolve(other);
            if (r.isPresent()) {
               return r;
            }
         }

         return Optional.empty();
      }
   }

   public Optional<Path> assetsOverride() {
      if (this.source == Style.Source.PACK) {
         return Optional.empty();
      } else {
         Path p = this.root.resolve("assets");
         return Files.isDirectory(p) ? Optional.of(p) : Optional.empty();
      }
   }

   public boolean providesAnything(Category c) throws IOException {
      for (Target t : c.targets()) {
         if (this.resolve(t).isPresent()) {
            return true;
         }
      }

      return this.assetsOverride().isPresent();
   }

   public static enum Source {
      BUILTIN,
      CUSTOM,
      PACK;
   }
}
