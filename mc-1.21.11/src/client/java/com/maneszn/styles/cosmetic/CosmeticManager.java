package com.maneszn.styles.cosmetic;

import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.name.NameDecorator;
import com.maneszn.styles.net.ApiClient;
import com.maneszn.styles.render.CosmeticScenes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class CosmeticManager {
   private static final Map<String, Identifier> IDS = new ConcurrentHashMap<>();
   public static final String[] TINTS = new String[]{
      "ff4d4d", "ff9f1a", "ffe14d", "4dff6a", "33e0ff", "3b7bff", "a855ff", "ff5fd2", "ffffff", "555555", "rainbow"
   };
   public static final String PET_POS = "pet_pos";
   public static final String DEFAULT_SKIN = "Maneszn";

   private CosmeticManager() {
   }

   public static Map<String, String> loadout(UUID uuid) {
      if (uuid == null) {
         return Map.of();
      } else if (ApiClient.isSelf(uuid)) {
         return ModConfig.get().cosmetics;
      } else {
         return !ModConfig.get().showOthersCosmetics ? Map.of() : ApiClient.get().cosmeticsOf(uuid);
      }
   }

   public static Cosmetic get(UUID uuid, Cosmetic.Type type) {
      Map<String, String> lo = loadout(uuid);
      Cosmetic c = CosmeticRegistry.get().of(lo, type);
      if (c == null && type == Cosmetic.Type.CAPE) {
         String id = lo.get(type.id());
         if (CustomCapes.isCustom(id)) {
            if (!ApiClient.isSelf(uuid) && !ModConfig.get().showCustomCapes) {
               return null;
            }

            CustomCapes.request(id);
         }
      }

      return c;
   }

   public static double ticks() {
      return System.currentTimeMillis() / 50.0;
   }

   public static Identifier capeTexture(UUID uuid) {
      Cosmetic cape = get(uuid, Cosmetic.Type.CAPE);
      return cape == null ? null : texture(cape.framePath(cape.frameAt((long)ticks())));
   }

   public static Identifier texture(String path) {
      return IDS.computeIfAbsent(path, ManesznStyles::id);
   }

   public static UUID uuidOf(int entityId) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return null;
      } else {
         return mc.level.getEntity(entityId) instanceof Player p ? p.getUUID() : null;
      }
   }

   public static boolean locked(Cosmetic c) {
      return c != null && c.requires() != null && !ApiClient.get().entitled(c.requires());
   }

   public static boolean hidden(Cosmetic c) {
      if (c == null) {
         return false;
      } else {
         return "owner".equals(c.requires()) ? !ApiClient.get().isOwner() : "admin".equals(c.requires()) && !ApiClient.get().isAdmin();
      }
   }

   public static void equip(Cosmetic.Type type, String id) {
      ModConfig cfg = ModConfig.get();
      if (id == null) {
         cfg.cosmetics.remove(type.id());
      } else {
         cfg.cosmetics.put(type.id(), id);
      }

      ModConfig.save();
      ApiClient.get().markCosmeticsDirty();
   }

   public static boolean tintable(Cosmetic.Type t) {
      return t.modeled() || t == Cosmetic.Type.CAPE || t == Cosmetic.Type.AURA || t == Cosmetic.Type.PET;
   }

   public static String tintKey(Cosmetic.Type t) {
      return "tint_" + t.id();
   }

   public static int tint(Map<String, String> loadout, Cosmetic.Type t) {
      return loadout == null ? -1 : parseTint(loadout.get(tintKey(t)));
   }

   public static int parseTint(String v) {
      if (v == null || v.isEmpty()) {
         return -1;
      } else if ("rainbow".equals(v)) {
         return 0xFF000000 | NameDecorator.hsv(ticks() / 80.0 % 1.0, 0.75, 1.0);
      } else {
         try {
            return 0xFF000000 | Integer.parseInt(v, 16);
         } catch (NumberFormatException var2) {
            return -1;
         }
      }
   }

   public static String skinNameOr(String name) {
      return name != null && !name.isBlank() ? name : "Maneszn";
   }

   public static List<CosmeticManager.BuddyAt> buddies(Map<String, String> loadout) {
      List<CosmeticManager.BuddyAt> out = new ArrayList<>();
      if (loadout != null && !loadout.isEmpty()) {
         CosmeticRegistry reg = CosmeticRegistry.get();

         for (CosmeticScenes.BuddySpot spot : CosmeticScenes.BuddySpot.values()) {
            String id = loadout.get(spot.key());
            if (id != null) {
               Cosmetic c = reg.get(Cosmetic.Type.BUDDY, id);
               if (c == null && "mini_ich".equals(id)) {
                  c = miniMeFor(spot);
               }

               if (c != null) {
                  out.add(new CosmeticManager.BuddyAt(spot, c, loadout.get(spot.skinKey())));
               }
            }
         }

         if (out.isEmpty() && "mini_ich".equals(loadout.get("buddy"))) {
            CosmeticScenes.BuddySpot spotx = CosmeticScenes.BuddySpot.of(loadout.get("buddy_pos"));
            Cosmetic cx = miniMeFor(spotx);
            if (cx != null) {
               out.add(new CosmeticManager.BuddyAt(spotx, cx, loadout.get("buddy_skin")));
            }
         }

         return out;
      } else {
         return out;
      }
   }

   public static Cosmetic miniMeFor(CosmeticScenes.BuddySpot spot) {
      for (Cosmetic c : CosmeticRegistry.get().list(Cosmetic.Type.BUDDY)) {
         if (c.spot() != null && CosmeticScenes.BuddySpot.of(c.spot()) == spot) {
            return c;
         }
      }

      return null;
   }

   public static CosmeticScenes.BuddySpot spotOf(Cosmetic c) {
      return CosmeticScenes.BuddySpot.of(c == null ? null : c.spot());
   }

   public static boolean buddyEquipped(Cosmetic c) {
      if (c == null) {
         return false;
      } else {
         for (CosmeticManager.BuddyAt b : buddies(ModConfig.get().cosmetics)) {
            if (b.buddy().id().equals(c.id())) {
               return true;
            }
         }

         return false;
      }
   }

   public static void toggleBuddy(Cosmetic c) {
      if (c != null) {
         equipBuddy(spotOf(c), buddyEquipped(c) ? null : c.id());
      }
   }

   public static void unequipAll() {
      ModConfig cfg = ModConfig.get();
      cfg.cosmetics.keySet().removeIf(k -> !k.startsWith("skin_"));
      ModConfig.save();
      ApiClient.get().markCosmeticsDirty();
   }

   public static void applyLoadout(Map<String, String> loadout) {
      ModConfig cfg = ModConfig.get();
      cfg.cosmetics.clear();
      if (loadout != null) {
         cfg.cosmetics.putAll(loadout);
      }

      ModConfig.save();
      ApiClient.get().markCosmeticsDirty();
   }

   public static int equippedCount() {
      int n = 0;

      for (String k : ModConfig.get().cosmetics.keySet()) {
         if (!k.startsWith("skin_")) {
            n++;
         }
      }

      return n;
   }

   public static void equipBuddy(CosmeticScenes.BuddySpot spot, String id) {
      ModConfig cfg = ModConfig.get();
      migrateLegacyBuddy(cfg);
      if (id == null) {
         cfg.cosmetics.remove(spot.key());
      } else {
         cfg.cosmetics.put(spot.key(), id);
      }

      ModConfig.save();
      ApiClient.get().markCosmeticsDirty();
   }

   public static void migrateLegacyBuddy(ModConfig cfg) {
      String old = cfg.cosmetics.remove("buddy");
      String pos = cfg.cosmetics.remove("buddy_pos");
      String skin = cfg.cosmetics.remove("buddy_skin");
      if (old != null) {
         CosmeticScenes.BuddySpot spot = CosmeticScenes.BuddySpot.of(pos);
         cfg.cosmetics.putIfAbsent(spot.key(), old);
         if (skin != null) {
            cfg.cosmetics.putIfAbsent(spot.skinKey(), skin);
         }
      }

      CosmeticRegistry reg = CosmeticRegistry.get();

      for (CosmeticScenes.BuddySpot spot : CosmeticScenes.BuddySpot.values()) {
         String id = cfg.cosmetics.get(spot.key());
         if (id != null && reg.get(Cosmetic.Type.BUDDY, id) == null) {
            if ("mini_ich".equals(id)) {
               Cosmetic m = miniMeFor(spot);
               if (m != null) {
                  cfg.cosmetics.put(spot.key(), m.id());
               } else {
                  cfg.cosmetics.remove(spot.key());
               }
            } else {
               cfg.cosmetics.remove(spot.key());
               if (reg.get(Cosmetic.Type.PET, id) != null) {
                  cfg.cosmetics.putIfAbsent(Cosmetic.Type.PET.id(), id);
               }
            }
         }
      }
   }

   public static boolean holdsHand(UUID uuid) {
      for (CosmeticManager.BuddyAt b : buddies(loadout(uuid))) {
         if (b.spot() == CosmeticScenes.BuddySpot.HAND) {
            return true;
         }
      }

      return false;
   }

   public static void setExtra(String key, String value) {
      ModConfig cfg = ModConfig.get();
      if (value != null && !value.isBlank()) {
         cfg.cosmetics.put(key, value);
      } else {
         cfg.cosmetics.remove(key);
      }

      ModConfig.save();
      ApiClient.get().markCosmeticsDirty();
   }

   public static String extra(String key) {
      return ModConfig.get().cosmetics.get(key);
   }

   public static String equipped(Cosmetic.Type type) {
      return ModConfig.get().cosmetics.get(type.id());
   }

   public record BuddyAt(CosmeticScenes.BuddySpot spot, Cosmetic buddy, String skinName) {
   }
}
