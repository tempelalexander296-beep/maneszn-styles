package com.maneszn.styles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class ModPaths {
   private ModPaths() {
   }

   public static Path root() {
      return FabricLoader.getInstance().getConfigDir().resolve("maneszn_styles");
   }

   public static Path configFile() {
      return root().resolve("config.json");
   }

   public static Path customDir() {
      return root().resolve("custom");
   }

   public static Path presetsDir() {
      return root().resolve("presets");
   }

   public static Path exportsDir() {
      return root().resolve("exports");
   }

   public static Path knownUsersFile() {
      return root().resolve("known_users.json");
   }

   public static Path generatedPackDir() {
      return root().resolve(".generated").resolve("pack");
   }

   public static Path resourcePacksDir() {
      return FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
   }

   public static void ensureDirectories() {
      try {
         Files.createDirectories(customDir());
         Files.createDirectories(presetsDir());
         Files.createDirectories(exportsDir());
         Files.createDirectories(generatedPackDir());
      } catch (IOException var1) {
         ManesznStyles.LOGGER.error("Konnte Ordner nicht anlegen", var1);
      }
   }
}
