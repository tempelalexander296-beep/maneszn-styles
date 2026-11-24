package com.maneszn.styles.style;

import java.util.ArrayList;
import java.util.List;

public record Target(String name, String path, int width, int height, boolean shared, Category.Kind kind, List<String> alt) {
   public List<String> altPackPaths() {
      List<String> out = new ArrayList<>();
      if (this.alt != null) {
         for (String a : this.alt) {
            int i = a.indexOf(58);
            out.add("assets/" + (i < 0 ? "minecraft" : a.substring(0, i)) + "/" + (i < 0 ? a : a.substring(i + 1)));
         }
      }

      return out;
   }

   public Category.Kind kindOr(Category.Kind fallback) {
      return this.kind == null ? fallback : this.kind;
   }

   public String namespace() {
      int i = this.path.indexOf(58);
      return i < 0 ? "minecraft" : this.path.substring(0, i);
   }

   public String pathInNamespace() {
      int i = this.path.indexOf(58);
      return i < 0 ? this.path : this.path.substring(i + 1);
   }

   public String packPath() {
      return "assets/" + this.namespace() + "/" + this.pathInNamespace();
   }
}
