package com.maneszn.styles.emote;

import com.maneszn.styles.config.ModConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public final class EmoteMusic {
   private static final Map<String, EmoteMusic.Track> TRACKS = new HashMap<>();
   private static final Map<UUID, Long> LAST_TICK = new HashMap<>();
   private static final Map<String, SoundEvent> SOUNDS = new HashMap<>();

   private EmoteMusic() {
   }

   private static EmoteMusic.Track track(int length, List<EmoteMusic.Note>... parts) {
      List<EmoteMusic.Note> all = new ArrayList<>();

      for (List<EmoteMusic.Note> p : parts) {
         all.addAll(p);
      }

      return new EmoteMusic.Track(length, all);
   }

   private static List<EmoteMusic.Note> drums(String pattern) {
      List<EmoteMusic.Note> out = new ArrayList<>();

      for (int i = 0; i < pattern.length(); i++) {
         switch (pattern.charAt(i)) {
            case 'B':
               out.add(new EmoteMusic.Note(i, "basedrum", 6, 0.55F));
               break;
            case 'H':
               out.add(new EmoteMusic.Note(i, "hat", 18, 0.3F));
               break;
            case 'S':
               out.add(new EmoteMusic.Note(i, "snare", 12, 0.45F));
         }
      }

      return out;
   }

   private static List<EmoteMusic.Note> line(String inst, int start, int[] pitches, int step, float vol) {
      List<EmoteMusic.Note> out = new ArrayList<>();

      for (int i = 0; i < pitches.length; i++) {
         if (pitches[i] >= 0) {
            out.add(new EmoteMusic.Note(start + i * step, inst, pitches[i], vol));
         }
      }

      return out;
   }

   public static void tick(Minecraft mc) {
      if (mc.level != null && mc.player != null && ModConfig.get().emoteSounds) {
         for (Player p : mc.level.players()) {
            EmoteManager.Active a = EmoteManager.of(p.getUUID());
            if (a == null) {
               LAST_TICK.remove(p.getUUID());
            } else {
               EmoteMusic.Track tr = TRACKS.get(a.id());
               if (tr != null) {
                  long t = (System.currentTimeMillis() - a.startedAt()) / 50L;
                  Long last = LAST_TICK.get(p.getUUID());
                  long from = last == null ? t : last + 1L;
                  if (t - from > 10L) {
                     from = t;
                  }

                  for (long k = from; k <= t; k++) {
                     int inTrack = (int)Math.floorMod(k, (long)tr.length());

                     for (EmoteMusic.Note n : tr.notes()) {
                        if (n.tick() == inTrack) {
                           SoundEvent ev = sound(n.inst());
                           if (ev != null) {
                              float pitch = (float)Math.pow(2.0, (n.pitch() - 12) / 12.0);
                              float vol = n.vol() * (p == mc.player ? 1.0F : 0.8F);
                              mc.level.playLocalSound(p.getX(), p.getY() + 1.0, p.getZ(), ev, SoundSource.PLAYERS, vol, pitch, false);
                           }
                        }
                     }
                  }

                  LAST_TICK.put(p.getUUID(), t);
               }
            }
         }
      } else {
         LAST_TICK.clear();
      }
   }

   private static SoundEvent sound(String inst) {
      return SOUNDS.computeIfAbsent(inst, i -> SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace("block.note_block." + i)));
   }

   static {
      EmoteMusic.Track disco = track(
         32,
         drums("B...H...B...H...B...H...B.S.H.S."),
         line("bass", 0, new int[]{6, -1, 6, -1, 13, -1, 6, -1, 8, -1, 8, -1, 15, -1, 8, -1}, 2, 0.6F),
         line("pling", 0, new int[]{18, 22, 25, 22}, 8, 0.35F)
      );
      TRACKS.put("disco", disco);
      TRACKS.put("floss", track(24, drums("B..S..B.BS..B..S..B.BS.."), line("pling", 0, new int[]{13, 13, 16, 18, 16, 13}, 4, 0.4F)));
      TRACKS.put("roboter", track(32, drums("B.......S.......B.......S......."), line("bit", 0, new int[]{12, -1, 12, 15, -1, 12, 10, -1}, 4, 0.45F)));
      TRACKS.put("headbang", track(16, drums("B.S.B.S.B.SSB.S."), line("guitar", 0, new int[]{6, 6, 9, 6, 11, 9, 6, 4}, 2, 0.6F)));
      TRACKS.put("luftgitarre", track(32, drums("B...S...B.B.S...B...S...B.B.S.S."), line("guitar", 0, new int[]{6, -1, 9, 11, -1, 13, 11, 9}, 4, 0.6F)));
      TRACKS.put(
         "hula",
         track(
            40,
            line("flute", 0, new int[]{13, 15, 18, 15, 13, 11, 13, -1, 10, 11}, 4, 0.4F),
            line("bass", 0, new int[]{1, -1, 8, -1, 1, -1, 8, -1, 6, -1}, 4, 0.5F)
         )
      );
      TRACKS.put("wellen", TRACKS.get("hula"));
      TRACKS.put("huhn", track(16, drums("B.H.S.H.B.H.S.HH"), line("xylophone", 0, new int[]{18, 18, 20, 18, 23, 22}, 2, 0.45F)));
      TRACKS.put("pinguin", track(16, drums("B...S...B.B.S..."), line("xylophone", 0, new int[]{13, 15, 13, 10}, 4, 0.45F)));
      TRACKS.put("hampelmann", track(16, drums("B.H.S.H.B.H.S.H."), line("pling", 0, new int[]{13, 17, 20, 17}, 4, 0.35F)));
      TRACKS.put("kreiseln", track(24, line("bell", 0, new int[]{13, 17, 20, 25, 20, 17}, 4, 0.35F)));
      TRACKS.put("meditieren", track(60, line("chime", 0, new int[]{13, -1, 20, -1, 17}, 12, 0.3F)));
      TRACKS.put("zombie", track(32, drums("B.......B..S....B.......B..S...."), line("didgeridoo", 0, new int[]{1, -1, -1, 0, -1, -1, 3, -1}, 4, 0.5F)));
      TRACKS.put("boxen", track(16, drums("B.S.BBS.B.S.BBSS")));
      TRACKS.put("kniebeugen", track(20, drums("B....B....S....S....")));
      TRACKS.put("schwimmen", track(24, line("harp", 0, new int[]{10, 13, 17, 13, 10, 8}, 4, 0.35F)));
      TRACKS.put("herz", track(32, line("bell", 0, new int[]{18, -1, 17, 18, -1, 22, -1, -1}, 4, 0.35F)));
      TRACKS.put("jubeln", track(20, drums("B.B.S.B.B.S.SSB.B.S.")));
      TRACKS.put("klatschen", track(16, drums("....S.......S...")));
      TRACKS.put("take_the_l", track(16, drums("B.H.S.H.B.HBS.H."), line("bit", 0, new int[]{13, 13, 16, 13, 18, 16}, 2, 0.45F)));
      TRACKS.put(
         "griddy",
         track(
            16,
            drums("B..HS.H.B.BHS.H."),
            line("bass", 0, new int[]{6, -1, 6, 9, -1, 6, 4, -1}, 2, 0.6F),
            line("pling", 0, new int[]{18, -1, 21, -1}, 4, 0.35F)
         )
      );
      TRACKS.put("moonwalk", track(32, drums("B...S...B...S...B...S...B.B.S..."), line("bass", 0, new int[]{8, -1, 8, 11, -1, 8, 6, -1}, 4, 0.55F)));
      TRACKS.put("karate", track(48, line("chime", 0, new int[]{13, -1, 15, 18, -1, 20}, 8, 0.35F), drums("B.......S.......B.......S.......B.......S.......")));
      TRACKS.put("auslachen", track(12, line("xylophone", 0, new int[]{20, 18, 20, 18, 16, 13}, 2, 0.4F)));
      TRACKS.put("weinen", track(40, line("flute", 0, new int[]{13, 12, 10, -1, 8}, 8, 0.35F)));
      TRACKS.put("schweben", track(60, line("chime", 0, new int[]{18, -1, 22, -1, 25}, 12, 0.3F)));
      TRACKS.put("tornado", track(16, drums("BHHHSHHHBHBHSHHS"), line("bell", 0, new int[]{13, 17, 20, 25}, 4, 0.3F)));
      TRACKS.put("shuffle", track(16, drums("B.H.B.H.B.H.B.HS"), line("bit", 0, new int[]{6, 13, 6, 13, 8, 15, 8, 15}, 2, 0.45F)));
      TRACKS.put("helikopter", track(8, drums("SSSSSSSS")));
      TRACKS.put("machtwelle", track(30, drums("B.............................")));
      TRACKS.put("thron", track(48, line("harp", 0, new int[]{6, 10, 13, 18, 13, 10}, 8, 0.35F)));
      TRACKS.put("koenig", track(48, line("bell", 0, new int[]{13, -1, 17, 20, -1, 25}, 8, 0.35F), line("bass", 0, new int[]{1, -1, -1, 8, -1, -1}, 8, 0.5F)));
      TRACKS.put("party", track(16, drums("B.H.S.H.B.HBS.H."), line("pling", 0, new int[]{18, -1, 18, 20, -1, 22, 20, -1}, 2, 0.35F)));
      TRACKS.put("muskeln", track(32, drums("B.......S.......B.B.....S......."), line("bass", 0, new int[]{6, -1, 6, 8}, 8, 0.5F)));
      TRACKS.put("lasso", track(24, drums("B..H..S..H..B..H..S..HSS"), line("banjo", 0, new int[]{13, 17, 20, 17, 15, 13}, 4, 0.4F)));
      TRACKS.put("ninja", track(16, drums("BHBHSHBHBHBHSHSH"), line("flute", 0, new int[]{20, 18, 15, 13}, 4, 0.35F)));
      TRACKS.put("matrix", track(48, line("bit", 0, new int[]{8, -1, 13, -1, 15, -1, 8, -1, 20, -1, 15, -1}, 4, 0.35F)));
      TRACKS.put("aufladen", track(8, drums("B...B..."), line("bass", 0, new int[]{3, 5}, 4, 0.5F)));
      TRACKS.put("windmuehle", track(24, drums("B.H.S.H.B.H.S.H.B.H.S.HS"), line("guitar", 0, new int[]{13, 15, 17, 20, 17, 15}, 4, 0.35F)));
      TRACKS.put("bannhammer", track(30, drums("..................BB.........."), line("bass", 18, new int[]{1}, 1, 0.7F)));
      TRACKS.put("glitch", track(16, line("bit", 0, new int[]{24, 3, 18, -1, 6, 22, -1, 1}, 2, 0.3F)));
      TRACKS.put("schwerelos", track(80, line("chime", 0, new int[]{13, -1, 17, -1, 22, -1, 25, -1, 20, -1}, 8, 0.3F)));
      TRACKS.put(
         "dirigent",
         track(
            36, line("harp", 0, new int[]{13, 17, 20, 25, 20, 17, 13, 10, 13}, 4, 0.35F), line("bass", 0, new int[]{6, -1, -1, 1, -1, -1, 6, -1, -1}, 4, 0.45F)
         )
      );
      TRACKS.put("geldregen", track(16, drums("B.HHS.HHB.BHS.HH"), line("bell", 0, new int[]{22, -1, 25, -1}, 4, 0.3F)));
      TRACKS.put("engel", track(64, line("chime", 0, new int[]{18, 22, 25, 22, 18, -1, 17, -1}, 8, 0.3F)));
      TRACKS.put(
         "gottmodus",
         track(
            64, line("didgeridoo", 0, new int[]{1, -1, -1, -1, -1, -1, -1, -1}, 8, 0.5F), line("bell", 0, new int[]{13, 18, 22, 25, 22, 18, 13, -1}, 8, 0.3F)
         )
      );
      TRACKS.put(
         "weltherrscher",
         track(
            64, line("didgeridoo", 0, new int[]{1, -1, -1, -1, 3, -1, -1, -1}, 8, 0.5F), line("chime", 0, new int[]{13, -1, 20, -1, 25, -1, 20, -1}, 8, 0.3F)
         )
      );
   }

   private record Note(int tick, String inst, int pitch, float vol) {
   }

   private record Track(int length, List<EmoteMusic.Note> notes) {
   }
}
