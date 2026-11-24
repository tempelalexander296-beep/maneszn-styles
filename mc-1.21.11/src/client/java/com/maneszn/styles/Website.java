package com.maneszn.styles;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.maneszn.styles.config.ModConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterInit;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ClickEvent.OpenUrl;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.util.Util;

public final class Website {
   private static final String FALLBACK_URL = "https://maneszn-api.onrender.com";
   private static volatile String latestVersion;
   private static volatile boolean notified;

   private Website() {
   }

   public static String url() {
      String u = null;

      try {
         u = ModConfig.get().apiUrl;
      } catch (Throwable var2) {
      }

      if (u == null || u.isBlank()) {
         u = "https://maneszn-api.onrender.com";
      }

      u = u.trim();

      while (u.endsWith("/")) {
         u = u.substring(0, u.length() - 1);
      }

      return u;
   }

   public static String downloadUrl() {
      return url() + "/download";
   }

   public static void open() {
      String u = url() + "/";

      try {
         Util.getPlatform().openUri(u);
      } catch (Throwable var3) {
         ManesznStyles.LOGGER.warn("Maneszn Styles: Webseite konnte nicht geöffnet werden ({})", u, var3);
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null) {
            mc.player.displayClientMessage(link("§d[Maneszn Styles] §fWebseite: ", u), false);
         }
      }
   }

   public static void register() {
      ScreenEvents.AFTER_INIT.register((AfterInit)(client, screen, w, h) -> {
         if (screen instanceof PauseScreen) {
            Screens.getButtons(screen).removeIf(b -> b.getX() == 6 && b.getWidth() == 110 && b.getY() <= 50);
         }
      });
      ClientCommandRegistrationCallback.EVENT.register((ClientCommandRegistrationCallback)(dispatcher, registryAccess) -> {
         for (String sub : new String[]{"web", "webseite"}) {
            dispatcher.register((LiteralArgumentBuilder)ClientCommandManager.literal("ms").then(ClientCommandManager.literal(sub).executes(ctx -> {
               Minecraft.getInstance().execute(Website::open);
               return 1;
            })));
         }
      });
      ClientPlayConnectionEvents.JOIN.register((Join)(handler, sender, client) -> client.execute(Website::maybeNotify));
      checkVersionAsync();
   }

   private static void checkVersionAsync() {
      Thread t = new Thread(
         () -> {
            try {
               HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20L)).build();
               HttpRequest req = HttpRequest.newBuilder(URI.create(url() + "/v1/version"))
                  .timeout(Duration.ofSeconds(60L))
                  .header("User-Agent", "ManesznStyles/" + currentVersion())
                  .GET()
                  .build();
               HttpResponse<String> res = http.send(req, BodyHandlers.ofString());
               if (res.statusCode() != 200) {
                  return;
               }

               JsonObject o = JsonParser.parseString(res.body()).getAsJsonObject();
               if (o.has("version")) {
                  latestVersion = o.get("version").getAsString();
               }

               Minecraft.getInstance().execute(Website::maybeNotify);
            } catch (Throwable var4) {
               ManesznStyles.LOGGER.info("Maneszn Styles: Versions-Abfrage nicht möglich ({})", var4.toString());
            }
         },
         "Maneszn-Versioncheck"
      );
      t.setDaemon(true);
      t.start();
   }

   static void maybeNotify() {
      Minecraft mc = Minecraft.getInstance();
      String latest = latestVersion;
      if (!notified && latest != null && mc.player != null) {
         if (compare(latest, currentVersion()) > 0) {
            Updater.Status st = Updater.status();
            if (st != Updater.Status.DOWNLOADING) {
               notified = true;
               if (st == Updater.Status.READY) {
                  mc.player
                     .displayClientMessage(
                        Component.literal(
                           "§d[Maneszn Styles] §fUpdate §a" + Updater.readyVersion() + "§f ist heruntergeladen. Starte Minecraft neu, dann ist es installiert."
                        ),
                        false
                     );
               } else {
                  mc.player
                     .displayClientMessage(
                        Component.literal("§d[Maneszn Styles] §fNeue Version §a" + latest + "§f verfügbar (du hast " + currentVersion() + "). "), false
                     );
                  mc.player.displayClientMessage(link("§7» ", downloadUrl()), false);
               }
            }
         }
      }
   }

   private static Component link(String prefix, String url) {
      MutableComponent l = Component.literal(url)
         .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.UNDERLINE)
         .withStyle(s -> s.withClickEvent(new OpenUrl(URI.create(url))).withHoverEvent(new ShowText(Component.literal("Im Browser öffnen"))));
      return Component.literal(prefix).append(l);
   }

   public static String currentVersion() {
      return FabricLoader.getInstance().getModContainer("maneszn_styles").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("0");
   }

   static int compare(String a, String b) {
      String[] x = a.replaceAll("[^0-9.]", "").split("\\.");
      String[] y = b.replaceAll("[^0-9.]", "").split("\\.");

      for (int i = 0; i < Math.max(x.length, y.length); i++) {
         int p = i < x.length ? parse(x[i]) : 0;
         int q = i < y.length ? parse(y[i]) : 0;
         if (p != q) {
            return Integer.compare(p, q);
         }
      }

      return 0;
   }

   private static int parse(String s) {
      try {
         return s.isEmpty() ? 0 : Integer.parseInt(s);
      } catch (NumberFormatException var2) {
         return 0;
      }
   }
}
