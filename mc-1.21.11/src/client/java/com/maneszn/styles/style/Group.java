package com.maneszn.styles.style;

import java.util.Map;

public record Group(String id, Map<String, String> names) {
   public String displayName(String lang) {
      String n = this.names.get(lang);
      if (n == null) {
         n = this.names.get("en");
      }

      return n == null ? this.id : n;
   }
}
