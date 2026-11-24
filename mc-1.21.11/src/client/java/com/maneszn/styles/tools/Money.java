package com.maneszn.styles.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.maneszn.styles.ModPaths;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public final class Money {
   private static final Pattern LINE = Pattern.compile(
      "(?i)(geld|money|balance|kontostand|coins?)\\s*[:»>\\-]?\\s*\\$?\\s*([0-9][0-9.,]*\\s*(?:k|m|mio|b|mrd)?)"
   );
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static Money.Data data;
   private static Double last;
   private static double sessionIn;
   private static double sessionOut;
   private static int tick;

   private Money() {
   }

   private static Path file() {
      return ModPaths.root().resolve("money.json");
   }

   public static Money.Data data() {
      if (data == null) {
         try {
            Path f = file();
            data = Files.isRegularFile(f) ? (Money.Data)GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Money.Data.class) : null;
         } catch (Exception var1) {
            data = null;
         }

         if (data == null) {
            data = new Money.Data();
         }

         if (data.changes == null) {
            data.changes = new ArrayList<>();
         }

         if (data.days == null) {
            data.days = new LinkedHashMap<>();
         }
      }

      return data;
   }

   private static void save() {
      try {
         Files.createDirectories(file().getParent());
         Files.writeString(file(), GSON.toJson(data()), StandardCharsets.UTF_8);
      } catch (Exception var1) {
      }
   }

   public static Double balance() {
      return last;
   }

   public static double sessionIn() {
      return sessionIn;
   }

   public static double sessionOut() {
      return sessionOut;
   }

   public static double[] today() {
      return data().days.getOrDefault(LocalDate.now().toString(), new double[2]);
   }

   public static void tick(Minecraft mc) {
      if (mc.level != null && ++tick % 20 == 0) {
         Double v = readSidebar(mc);
         if (v != null) {
            if (last != null && Math.abs(v - last) > 1.0E-4) {
               double d = v - last;
               Money.Data dt = data();
               Money.Change c = new Money.Change();
               c.time = System.currentTimeMillis();
               c.delta = d;
               c.balance = v;
               dt.changes.add(c);

               while (dt.changes.size() > 500) {
                  dt.changes.remove(0);
               }

               double[] day = dt.days.computeIfAbsent(LocalDate.now().toString(), k -> new double[2]);
               if (d > 0.0) {
                  day[0] += d;
                  sessionIn += d;
               } else {
                  day[1] += -d;
                  sessionOut += -d;
               }

               while (dt.days.size() > 60) {
                  dt.days.remove(dt.days.keySet().iterator().next());
               }

               save();
            }

            last = v;
         }
      }
   }

   private static Double readSidebar(Minecraft mc) {
      Scoreboard sb = mc.level.getScoreboard();
      Objective obj = sb.getDisplayObjective(DisplaySlot.SIDEBAR);
      if (obj == null) {
         return null;
      } else {
         for (PlayerScoreEntry e : sb.listPlayerScores(obj)) {
            if (!e.isHidden()) {
               PlayerTeam team = sb.getPlayersTeam(e.owner());
               Component name = (Component)(e.display() != null ? e.display() : PlayerTeam.formatNameForTeam(team, Component.literal(e.owner())));
               String s = ChatFormatting.stripFormatting(name.getString());
               if (s != null) {
                  Matcher m = LINE.matcher(s);
                  if (m.find()) {
                     Double v = Num.parse(m.group(2));
                     if (v != null) {
                        return v;
                     }
                  }
               }
            }
         }

         return null;
      }
   }

   public static String fmtSigned(double d) {
      return (d >= 0.0 ? "+" : "-") + Num.compact(Math.abs(d));
   }

   static String lower(String s) {
      return s.toLowerCase(Locale.ROOT);
   }

   public static final class Change {
      public long time;
      public double delta;
      public double balance;
   }

   public static final class Data {
      public List<Money.Change> changes = new ArrayList<>();
      public Map<String, double[]> days = new LinkedHashMap<>();
   }
}
