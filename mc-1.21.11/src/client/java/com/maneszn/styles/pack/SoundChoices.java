package com.maneszn.styles.pack;

import com.maneszn.styles.config.ModConfig;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class SoundChoices {
   public static final List<SoundChoices.Slot> SLOTS = List.of(
      new SoundChoices.Slot(
         "hit", List.of("entity.player.attack.strong", "entity.player.attack.weak", "entity.player.attack.knockback", "entity.player.attack.nodamage")
      ),
      new SoundChoices.Slot("crit", List.of("entity.player.attack.crit")),
      new SoundChoices.Slot("sweep", List.of("entity.player.attack.sweep")),
      new SoundChoices.Slot("bow", List.of("entity.arrow.hit_player")),
      new SoundChoices.Slot("xp", List.of("entity.experience_orb.pickup")),
      new SoundChoices.Slot("levelup", List.of("entity.player.levelup"))
   );
   public static final List<SoundChoices.Choice> CHOICES = List.of(
      new SoundChoices.Choice("orb", List.of("random/orb"), "entity.experience_orb.pickup"),
      new SoundChoices.Choice("ding", List.of("random/successful_hit"), "entity.arrow.hit_player"),
      new SoundChoices.Choice("pling", List.of("note/pling"), "block.note_block.pling"),
      new SoundChoices.Choice("bell", List.of("note/bell"), "block.note_block.bell"),
      new SoundChoices.Choice("harp", List.of("note/harp"), "block.note_block.harp"),
      new SoundChoices.Choice("pop", List.of("random/pop"), "entity.item.pickup"),
      new SoundChoices.Choice("click", List.of("random/click"), "ui.button.click"),
      new SoundChoices.Choice("glass", List.of("random/glass1", "random/glass2", "random/glass3"), "block.glass.break"),
      new SoundChoices.Choice("anvil", List.of("random/anvil_land"), "block.anvil.land"),
      new SoundChoices.Choice("levelup", List.of("random/levelup"), "entity.player.levelup"),
      new SoundChoices.Choice("meow", List.of("mob/cat/meow1", "mob/cat/meow2"), "entity.cat.ambient"),
      new SoundChoices.Choice("mute", List.of(), null)
   );

   private SoundChoices() {
   }

   public static void preview(SoundChoices.Choice c) {
      if (c != null && c.preview() != null) {
         SoundEvent ev = SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace(c.preview()));
         Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, 1.0F));
      }
   }

   public static SoundChoices.Choice choice(String id) {
      if (id == null) {
         return null;
      } else {
         for (SoundChoices.Choice c : CHOICES) {
            if (c.id().equals(id)) {
               return c;
            }
         }

         return null;
      }
   }

   public static String next(String current) {
      if (current == null) {
         return CHOICES.get(0).id();
      } else {
         for (int i = 0; i < CHOICES.size(); i++) {
            if (CHOICES.get(i).id().equals(current)) {
               return i + 1 < CHOICES.size() ? CHOICES.get(i + 1).id() : null;
            }
         }

         return null;
      }
   }

   public static String soundsJson() {
      Map<String, String> sel = ModConfig.get().sounds;
      StringBuilder b = new StringBuilder("{\n");
      boolean any = false;

      for (SoundChoices.Slot s : SLOTS) {
         SoundChoices.Choice c = choice(sel.get(s.id()));
         if (c != null) {
            for (String ev : s.events()) {
               if (any) {
                  b.append(",\n");
               }

               any = true;
               b.append("  \"").append(ev).append("\": { \"replace\": true, \"sounds\": [");

               for (int i = 0; i < c.files().size(); i++) {
                  if (i > 0) {
                     b.append(", ");
                  }

                  b.append('"').append(c.files().get(i)).append('"');
               }

               b.append("] }");
            }
         }
      }

      b.append("\n}\n");
      return any ? b.toString() : null;
   }

   public record Choice(String id, List<String> files, String preview) {
   }

   public record Slot(String id, List<String> events) {
   }
}
