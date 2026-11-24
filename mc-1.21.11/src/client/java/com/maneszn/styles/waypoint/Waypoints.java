package com.maneszn.styles.waypoint;

import com.google.gson.reflect.TypeToken;
import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.ModPaths;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.gui.Ui;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;

public final class Waypoints {
   public static final int[] COLORS = new int[]{-5745153, -50373, -24806, -7859, -11665558, -13377281, -12878849, -41006, -1};
   private static List<Waypoints.Waypoint> all = new ArrayList<>();
   private static boolean loaded;
   private static boolean wasDead;

   private Waypoints() {
   }

   private static Path file() {
      return ModPaths.root().resolve("waypoints.json");
   }

   private static void ensureLoaded() {
      if (!loaded) {
         loaded = true;
         Path f = file();
         if (Files.exists(f)) {
            try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
               List<Waypoints.Waypoint> l = (List<Waypoints.Waypoint>)ModConfig.GSON.fromJson(r, (new TypeToken<List<Waypoints.Waypoint>>() {}).getType());
               if (l != null) {
                  l.removeIf(w -> w == null);
                  all = new ArrayList<>(l);
               }
            } catch (Exception var6) {
               ManesznStyles.LOGGER.warn("waypoints.json konnte nicht gelesen werden: {}", var6.toString());
            }
         }
      }
   }

   public static void save() {
      try {
         Files.createDirectories(ModPaths.root());

         try (Writer w = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
            ModConfig.GSON.toJson(all, w);
         }
      } catch (Exception var5) {
         ManesznStyles.LOGGER.warn("waypoints.json konnte nicht gespeichert werden: {}", var5.toString());
      }
   }

   public static String currentWorld() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.getCurrentServer() != null) {
         return mc.getCurrentServer().ip.toLowerCase(Locale.ROOT);
      } else {
         return mc.getSingleplayerServer() != null ? "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName() : "?";
      }
   }

   public static String currentDim() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return "overworld";
      } else {
         String s = mc.level.dimension().toString();
         if (s.contains("the_nether")) {
            return "the_nether";
         } else if (s.contains("the_end")) {
            return "the_end";
         } else {
            int i = s.lastIndexOf(58);
            String tail = i >= 0 ? s.substring(i + 1).replace("]", "").trim() : s;
            return tail.isEmpty() ? "overworld" : tail;
         }
      }
   }

   public static List<Waypoints.Waypoint> forWorld() {
      ensureLoaded();
      String w = currentWorld();
      List<Waypoints.Waypoint> out = new ArrayList<>();

      for (Waypoints.Waypoint p : all) {
         if (w.equals(p.world)) {
            out.add(p);
         }
      }

      return out;
   }

   public static List<Waypoints.Waypoint> visibleHere() {
      String d = currentDim();
      List<Waypoints.Waypoint> out = new ArrayList<>();

      for (Waypoints.Waypoint p : forWorld()) {
         if (p.visible && d.equals(p.dim)) {
            out.add(p);
         }
      }

      return out;
   }

   public static Waypoints.Waypoint addHere(String name, int color) {
      ensureLoaded();
      Minecraft mc = Minecraft.getInstance();
      Waypoints.Waypoint w = new Waypoints.Waypoint();
      w.name = name != null && !name.isBlank() ? name.trim() : "Wegpunkt " + (forWorld().size() + 1);
      if (mc.player != null) {
         w.x = mc.player.getBlockX();
         w.y = mc.player.getBlockY();
         w.z = mc.player.getBlockZ();
      }

      w.world = currentWorld();
      w.dim = currentDim();
      w.color = color;
      all.add(w);
      save();
      return w;
   }

   public static void remove(Waypoints.Waypoint w) {
      ensureLoaded();
      all.remove(w);
      save();
   }

   public static String nextName() {
      return "Wegpunkt " + (forWorld().size() + 1);
   }

   public static void tick(Minecraft mc) {
      if (mc.player == null) {
         wasDead = false;
      } else {
         boolean dead = mc.player.isDeadOrDying();
         if (dead && !wasDead) {
            ensureLoaded();
            String world = currentWorld();
            all.removeIf(p -> p.death && world.equals(p.world));
            Waypoints.Waypoint w = addHere(Ui.tr("maneszn_styles.waypoint.death"), -50373);
            w.death = true;
            save();
         }

         wasDead = dead;
      }
   }

   public static class Waypoint {
      public String name = "Wegpunkt";
      public int x;
      public int y;
      public int z;
      public String world = "";
      public String dim = "overworld";
      public int color = -5745153;
      public boolean visible = true;
      public boolean death;
   }
}
