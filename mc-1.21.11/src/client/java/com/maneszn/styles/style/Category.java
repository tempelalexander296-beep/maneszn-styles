package com.maneszn.styles.style;

import java.util.List;
import java.util.Map;

public record Category(
   String id,
   String group,
   Map<String, String> names,
   Category.Kind kind,
   String previewTarget,
   List<Target> targets,
   String preview3d,
   Map<String, String> faces,
   Category.Extras extras
) {
   public String note(String lang) {
      if (this.extras != null && !this.extras.notes().isEmpty()) {
         String n = this.extras.notes().get(lang);
         return n != null ? n : this.extras.notes().get("en");
      } else {
         return null;
      }
   }

   public Target faceTarget(String side) {
      String n = this.faces == null ? null : this.faces.get(side);
      return n == null ? this.previewTargetOrFirst() : this.target(n);
   }

   public String displayName(String lang) {
      String n = this.names.get(lang);
      if (n == null) {
         n = this.names.get("en");
      }

      if (n == null) {
         n = this.id;
      }

      return n;
   }

   public Target target(String name) {
      for (Target t : this.targets) {
         if (t.name().equals(name)) {
            return t;
         }
      }

      return this.targets.isEmpty() ? null : this.targets.get(0);
   }

   public Target previewTargetOrFirst() {
      return this.target(this.previewTarget);
   }

   public record Extras(
      int[] previewCrop, Map<String, String> extraFiles, List<String> includeFolders, Map<String, String> armorLayers, Map<String, String> notes
   ) {
      public static final Category.Extras NONE = new Category.Extras(null, Map.of(), List.of(), Map.of(), Map.of());
   }

   public static enum Kind {
      ITEM,
      BLOCK,
      ENTITY,
      GUI,
      LAYOUT,
      SKY,
      SOUND;
   }
}
