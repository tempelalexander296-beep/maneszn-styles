package com.maneszn.styles.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.maneszn.styles.ModPaths;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

public final class Market {
   private static final Pattern[] PRICE = new Pattern[]{
      Pattern.compile("(?i)(preis|price|kosten|kostet|cost|sofortkauf|buy ?now|gebot|bid|für|for)[^0-9]{0,14}([0-9][0-9.,]*\\s*(?:k|m|mio|b|mrd)?)\\b"),
      Pattern.compile("\\$\\s*([0-9][0-9.,]*\\s*(?:[kKmMbB])?)\\b"),
      Pattern.compile("([0-9][0-9.,]*\\s*(?:[kKmMbB])?)\\s*(?:\\$|coins?|geld)", 2)
   };
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static Map<String, Market.Entry> entries;
   private static final Set<String> sessionSeen = new HashSet<>();
   private static Object lastScreen;
   private static int tick;
   private static boolean dirty;

   private Market() {
   }

   private static Path file() {
      return ModPaths.root().resolve("market.json");
   }

   public static Map<String, Market.Entry> all() {
      if (entries == null) {
         try {
            Path f = file();
            entries = Files.isRegularFile(f)
               ? (Map)GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), (new TypeToken<LinkedHashMap<String, Market.Entry>>() {}).getType())
               : null;
         } catch (Exception var1) {
            entries = null;
         }

         if (entries == null) {
            entries = new LinkedHashMap<>();
         }
      }

      return entries;
   }

   public static void save() {
      try {
         Files.createDirectories(file().getParent());
         Files.writeString(file(), GSON.toJson(all()), StandardCharsets.UTF_8);
         dirty = false;
      } catch (Exception var1) {
      }
   }

   public static String key(ItemStack s) {
      return BuiltInRegistries.ITEM.getKey(s.getItem()) + "|" + plain(s.getHoverName());
   }

   public static Market.Entry get(ItemStack s) {
      return all().get(key(s));
   }

   public static void tick(Minecraft mc) {
      if (mc.screen != lastScreen) {
         lastScreen = mc.screen;
         sessionSeen.clear();
         if (dirty) {
            save();
         }
      }

      if (++tick % 10 == 0 && mc.player != null) {
         if (mc.screen instanceof AbstractContainerScreen<?> scr
            && !(mc.screen instanceof InventoryScreen)
            && !(mc.screen instanceof CreativeModeInventoryScreen)) {
            for (Slot slot : scr.getMenu().slots) {
               if (slot.container != mc.player.getInventory()) {
                  ItemStack st = slot.getItem();
                  if (!st.isEmpty()) {
                     Double total = price(st);
                     if (total != null && !(total <= 0.0)) {
                        double unit = total / Math.max(1, st.getCount());
                        String k = key(st);
                        String once = k + "@" + slot.index + "@" + total;
                        if (sessionSeen.add(once)) {
                           Market.Entry e = all().computeIfAbsent(k, x -> {
                              Market.Entry n = new Market.Entry();
                              n.item = BuiltInRegistries.ITEM.getKey(st.getItem()).toString();
                              n.name = plain(st.getHoverName());
                              return n;
                           });
                           e.min = Math.min(e.min, unit);
                           e.last = unit;
                           e.sum += unit;
                           e.count++;
                           e.seen = System.currentTimeMillis();
                           dirty = true;
                           if (e.alarm > 0.0 && unit <= e.alarm) {
                              alarm(mc, e, unit);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void alarm(Minecraft mc, Market.Entry e, double unit) {
      mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.6F);
      mc.player
         .displayClientMessage(
            Component.literal(
               "§d[Maneszn] §fPreis-Alarm: §e" + e.name + " §ffür §a" + Num.compact(unit) + " §7(pro Stück, Alarm ≤ " + Num.compact(e.alarm) + ")"
            ),
            false
         );
   }

   public static Double price(ItemStack st) {
      List<String> lines = new ArrayList<>();
      ItemLore lore = st.get(DataComponents.LORE);
      if (lore != null) {
         for (Component c : lore.lines()) {
            lines.add(plain(c));
         }
      }

      for (String l : lines) {
         for (Pattern p : PRICE) {
            Matcher m = p.matcher(l);
            if (m.find()) {
               Double v = Num.parse(m.group(m.groupCount()));
               if (v != null && v > 0.0) {
                  return v;
               }
            }
         }
      }

      return null;
   }

   static String plain(Component c) {
      String s = ChatFormatting.stripFormatting(c.getString());
      return s == null ? "" : s.trim();
   }

   public static Component tooltip(ItemStack st) {
      if (!st.isEmpty() && (entries != null || Files.isRegularFile(file()))) {
         Market.Entry e = get(st);
         return e != null && e.count != 0
            ? Component.literal("§8[Markt] §7Ø §e" + Num.compact(e.avg()) + " §7· min §a" + Num.compact(e.min) + " §8(pro Stück, " + e.count + "×)")
            : null;
      } else {
         return null;
      }
   }

   public static final class Entry {
      public String item;
      public String name;
      public double min = Double.MAX_VALUE;
      public double last;
      public double sum;
      public int count;
      public long seen;
      public double alarm;

      public double avg() {
         return this.count == 0 ? 0.0 : this.sum / this.count;
      }

      public boolean spawner() {
         return this.item.contains("spawner") || this.name.toLowerCase(Locale.ROOT).contains("spawner");
      }
   }
}
