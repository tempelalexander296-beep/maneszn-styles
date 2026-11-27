package com.maneszn.styles.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.ModPaths;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ModConfig {
   public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private static ModConfig instance = new ModConfig();
   public Map<String, String> active = new LinkedHashMap<>();
   public Set<String> favorites = new LinkedHashSet<>();
   public String theme = "lila";
   public boolean scanResourcePacks = true;
   public boolean anonymous = false;
   public static final String DEFAULT_API = "https://maneszn-api.onrender.com";
   public String apiUrl = "https://maneszn-api.onrender.com";
   public Map<String, String> cosmetics = new LinkedHashMap<>();
   public boolean showLogo = true;
   public boolean showOthersCosmetics = true;
   public boolean showAuras = true;
   public boolean freelookToggle = false;
   public double zoomLevel = 4.0;
   public boolean zoomSmooth = true;
   public boolean toggleSprint = false;
   public Map<String, ModConfig.HudPos> hud = new LinkedHashMap<>();
   public List<String> entCache = new ArrayList<>();
   public List<String> rankTickets = new ArrayList<>();
   public List<String> rankLedger = new ArrayList<>();
   public int keyVersion = 0;
   public boolean fullbright = false;
   public ModConfig.CrosshairCfg crosshair = new ModConfig.CrosshairCfg();
   public Map<String, String> sounds = new LinkedHashMap<>();
   public int blockOutlineColor = 0;
   public int hitColor = 0;
   public List<ModConfig.Outfit> outfits = new ArrayList<>();
   public boolean showCustomCapes = true;
   public boolean capePhysics = true;
   public double heldItemScale = 1.0;
   public double droppedItemScale = 1.0;
   public boolean fpsBoost = false;
   public String droppedItemStyle = "vanilla";
   public boolean armorWarn = true;
   public int armorWarnPercent = 15;
   public boolean autoTpa = false;
   public List<String> tpaFriends = new ArrayList<>();
   public boolean calcButton = true;
   public boolean elytraSwap = false;
   public boolean duraGuard = true;
   public boolean dropLock = true;
   public List<String> lockedItems = new ArrayList<>();
   public int duraGuardPercent = 5;
   public boolean duraGuardAllTools = false;
   public String pingServer = "";
   public Map<String, String> fpsBoostBackup = new LinkedHashMap<>();
   public Map<String, Double> itemScales = new LinkedHashMap<>();
   public boolean emoteSounds = true;
   public String lastImportDir = "";
   public int importResolution = 0;

   public static ModConfig get() {
      return instance;
   }

   public static void load() {
      Path file = ModPaths.configFile();
      if (Files.exists(file)) {
         try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            ModConfig loaded = (ModConfig)GSON.fromJson(r, ModConfig.class);
            if (loaded != null) {
               instance = loaded;
               if (instance.active == null) {
                  instance.active = new LinkedHashMap<>();
               }

               if (instance.favorites == null) {
                  instance.favorites = new LinkedHashSet<>();
               }

               if (instance.theme == null) {
                  instance.theme = "lila";
               }

               if (instance.cosmetics == null) {
                  instance.cosmetics = new LinkedHashMap<>();
               }

               if (instance.hud == null) {
                  instance.hud = new LinkedHashMap<>();
               }

               if (instance.crosshair == null) {
                  instance.crosshair = new ModConfig.CrosshairCfg();
               }

               if (instance.crosshair.pixels == null) {
                  instance.crosshair.pixels = new ArrayList<>();
               }

               if (instance.sounds == null) {
                  instance.sounds = new LinkedHashMap<>();
               }

               if (instance.outfits == null) {
                  instance.outfits = new ArrayList<>();
               }

               if (instance.itemScales == null) {
                  instance.itemScales = new LinkedHashMap<>();
               }

               if (instance.fpsBoostBackup == null) {
                  instance.fpsBoostBackup = new LinkedHashMap<>();
               }

               if (instance.rankTickets == null) {
                  instance.rankTickets = new ArrayList<>();
               }

               if (instance.rankLedger == null) {
                  instance.rankLedger = new ArrayList<>();
               }

               if (instance.lockedItems == null) {
                  instance.lockedItems = new ArrayList<>();
               }

               if (instance.tpaFriends == null) {
                  instance.tpaFriends = new ArrayList<>();
               }

               if (instance.armorWarnPercent <= 0) {
                  instance.armorWarnPercent = 15;
               }

               if (instance.duraGuardPercent <= 0) {
                  instance.duraGuardPercent = 5;
               }

               if (instance.droppedItemStyle == null) {
                  instance.droppedItemStyle = "vanilla";
               }

               instance.heldItemScale = Math.max(0.3, Math.min(2.0, instance.heldItemScale <= 0.0 ? 1.0 : instance.heldItemScale));
               instance.droppedItemScale = Math.max(0.3, Math.min(4.0, instance.droppedItemScale <= 0.0 ? 1.0 : instance.droppedItemScale));

               for (ModConfig.HudPos p : instance.hud.values()) {
                  if (p != null && (p.scale < 0.3 || p.scale > 4.0)) {
                     p.scale = 1.0;
                  }
               }

               if (instance.apiUrl == null
                  || instance.apiUrl.isBlank()
                  || instance.apiUrl.contains("maneszn.example")
                  || instance.apiUrl.contains("localhost")
                  || instance.apiUrl.contains("127.0.0.1")) {
                  instance.apiUrl = "https://maneszn-api.onrender.com";
               }
            }
         } catch (Exception var7) {
            ManesznStyles.LOGGER.error("config.json konnte nicht gelesen werden – benutze Standardwerte", var7);
            instance = new ModConfig();
         }
      }

      save();
   }

   public static void save() {
      try {
         Files.createDirectories(ModPaths.root());

         try (Writer w = Files.newBufferedWriter(ModPaths.configFile(), StandardCharsets.UTF_8)) {
            GSON.toJson(instance, w);
         }
      } catch (IOException var5) {
         ManesznStyles.LOGGER.error("config.json konnte nicht gespeichert werden", var5);
      }
   }

   public String activeStyle(String categoryId) {
      return this.active.getOrDefault(categoryId, "vanilla");
   }

   public boolean isFavorite(String categoryId, String styleId) {
      return this.favorites.contains(categoryId + "/" + styleId);
   }

   public void toggleFavorite(String categoryId, String styleId) {
      String key = categoryId + "/" + styleId;
      if (!this.favorites.remove(key)) {
         this.favorites.add(key);
      }

      save();
   }

   public static class CrosshairCfg {
      public boolean on = false;
      public String shape = "cross";
      public int size = 5;
      public int thickness = 1;
      public int gap = 2;
      public int color = -1;
      public boolean outline = true;
      public boolean centerDot = false;
      public boolean rainbow = false;
      public List<String> pixels = new ArrayList<>();
   }

   public static class HudPos {
      public boolean on;
      public double x;
      public double y;
      public double scale = 1.0;
      public int textColor = 0;
      public boolean background = true;
      public int bgColor = 0;
      public int bgOpacity = 40;
      public boolean accentBar = true;
      public boolean shadow = true;
      public Map<String, Boolean> opts = new LinkedHashMap<>();

      public HudPos() {
      }

      public HudPos(boolean on, double x, double y) {
         this.on = on;
         this.x = x;
         this.y = y;
      }
   }

   public static class Outfit {
      public String name = "Outfit";
      public Map<String, String> cosmetics = new LinkedHashMap<>();

      public Outfit() {
      }

      public Outfit(String name, Map<String, String> cosmetics) {
         this.name = name;
         this.cosmetics = new LinkedHashMap<>(cosmetics);
      }
   }
}
