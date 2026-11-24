package com.maneszn.styles.emote;

import com.maneszn.styles.net.ApiClient;
import java.util.Locale;
import java.util.Map;

public record Emote(String id, Map<String, String> names, String icon, int duration, Emote.Animator animator, String requires) {
   public Emote(String id, Map<String, String> names, String icon, int duration, Emote.Animator animator) {
      this(id, names, icon, duration, animator, null);
   }

   public boolean unlocked() {
      return this.requires == null || ApiClient.get().entitled(this.requires);
   }

   public String displayName(String lang) {
      return this.names.getOrDefault(lang.toLowerCase(Locale.ROOT), this.names.getOrDefault("en", this.id));
   }

   public boolean loops() {
      return this.duration <= 0;
   }

   public EmotePose pose(double t) {
      EmotePose p = new EmotePose();
      this.animator.pose(p, t);
      return p.restRest();
   }

   public interface Animator {
      void pose(EmotePose var1, double var2);
   }
}
