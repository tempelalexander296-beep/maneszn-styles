package com.maneszn.styles.tools;

import com.maneszn.styles.config.ModConfig;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class AutoTpa {
   private static final Map<String, Long> cooldown = new HashMap<>();

   private AutoTpa() {
   }

   public static void onMessage(Component msg) {
      ModConfig cfg = ModConfig.get();
      if (cfg.autoTpa && cfg.tpaFriends != null && !cfg.tpaFriends.isEmpty()) {
         String s = ChatFormatting.stripFormatting(msg.getString());
         if (s != null) {
            String low = s.toLowerCase(Locale.ROOT);
            boolean request = low.contains("tpa") || low.contains("teleport") || low.contains("teleportier");
            if (request) {
               if (!low.startsWith("du hast") && !low.startsWith("you sent") && !low.contains("gesendet") && !low.contains("sent a")) {
                  Minecraft mc = Minecraft.getInstance();
                  if (mc.player != null && mc.getConnection() != null) {
                     for (String f : cfg.tpaFriends) {
                        if (f != null && !f.isBlank() && low.matches(".*\\b" + Pattern.quote(f.toLowerCase(Locale.ROOT)) + "\\b.*")) {
                           long now = System.currentTimeMillis();
                           if (now - cooldown.getOrDefault(f, 0L) < 4000L) {
                              return;
                           }

                           cooldown.put(f, now);
                           mc.getConnection().sendCommand("tpaccept " + f);
                           mc.player.displayClientMessage(Component.literal("§d[Maneszn] §fAuto-TPA: Anfrage von §e" + f + " §fangenommen."), false);
                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
