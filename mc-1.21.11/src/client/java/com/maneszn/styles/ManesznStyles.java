package com.maneszn.styles;

import com.maneszn.styles.compat.Access;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.cosmetic.AuraTicker;
import com.maneszn.styles.cosmetic.CosmeticLayer;
import com.maneszn.styles.cosmetic.CosmeticManager;
import com.maneszn.styles.cosmetic.CosmeticRegistry;
import com.maneszn.styles.cosmetic.CustomCapes;
import com.maneszn.styles.emote.EmoteManager;
import com.maneszn.styles.gui.EmoteScreen;
import com.maneszn.styles.gui.HudEditScreen;
import com.maneszn.styles.gui.StylesScreen;
import com.maneszn.styles.hud.HudManager;
import com.maneszn.styles.hud.PvpStats;
import com.maneszn.styles.net.ApiClient;
import com.maneszn.styles.pack.StylePackBuilder;
import com.maneszn.styles.perf.FpsBoost;
import com.maneszn.styles.perf.PieChart;
import com.maneszn.styles.preset.PresetManager;
import com.maneszn.styles.render.OverlayColor;
import com.maneszn.styles.style.StyleRegistry;
import com.maneszn.styles.tools.ArmorWarn;
import com.maneszn.styles.tools.Market;
import com.maneszn.styles.tools.Money;
import com.maneszn.styles.view.Freelook;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ManesznStyles implements ClientModInitializer {
   public static final String MOD_ID = "maneszn_styles";
   public static final Logger LOGGER = LoggerFactory.getLogger("Maneszn Styles");
   public static final Category KEY_CATEGORY = Category.register(id("main"));
   public static KeyMapping openMenuKey;
   public static KeyMapping emoteKey;
   public static KeyMapping freelookKey;
   public static KeyMapping zoomKey;
   public static KeyMapping hudKey;
   public static KeyMapping fullbrightKey;
   private static final Set<String> FAILED_TICKS = new HashSet<>();
   private static final Map<KeyMapping, Boolean> RAW_DOWN = new HashMap<>();

   public void onInitializeClient() {
      ModPaths.ensureDirectories();
      ModConfig.load();
      StyleRegistry.get().reload();
      PresetManager.get().reload();
      CosmeticRegistry.get().reload();
      CosmeticManager.migrateLegacyBuddy(ModConfig.get());
      ApiClient.get().start();
      StylePackBuilder.rebuild();
      openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.maneszn_styles.open", Type.KEYSYM, 75, KEY_CATEGORY));
      emoteKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.maneszn_styles.emote_wheel", Type.KEYSYM, 66, KEY_CATEGORY));
      freelookKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.maneszn_styles.freelook", Type.KEYSYM, 342, KEY_CATEGORY));
      zoomKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.maneszn_styles.zoom", Type.KEYSYM, 67, KEY_CATEGORY));
      hudKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.maneszn_styles.hud_editor", Type.KEYSYM, 72, KEY_CATEGORY));
      fullbrightKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.maneszn_styles.fullbright", Type.KEYSYM, -1, KEY_CATEGORY));
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         safe("keymigrate", () -> migrateKeys(client));
         safe("keys", () -> handleKeys(client));
         safe("api", () -> ApiClient.get().tick(client));
         safe("aura", () -> AuraTicker.tick(client));
         safe("emote", () -> EmoteManager.tick(client));
         safe("freelook", () -> Freelook.tick(client));
         safe("hud", () -> HudManager.tick(client));
         safe("access", () -> Access.tick(client));
         safe("fps", () -> FpsBoost.tick(client));
         safe("pie", () -> PieChart.tick(client));
         safe("market", () -> Market.tick(client));
         safe("money", () -> Money.tick(client));
         safe("armorwarn", () -> ArmorWarn.tick(client));
         safe("capes", () -> CustomCapes.tick(client));
         safe("overlay", () -> OverlayColor.tick(client));
         safe("droplock", () -> com.maneszn.styles.tools.DropLock.tick(client));
      });
      safe("init-hud", HudManager::register);
      safe("init-pvp", PvpStats::register);
      safe("init-access", Access::register);
      safe("init-web", Website::register);
      safe("init-updater", Updater::register);
      safe("init-droplock", com.maneszn.styles.tools.DropLock::register);
      safe(
         "init-layer",
         () -> LivingEntityFeatureRendererRegistrationCallback.EVENT
            .register((LivingEntityFeatureRendererRegistrationCallback)(entityType, renderer, helper, context) -> {
               if (renderer instanceof AvatarRenderer<?> avatar) {
                  helper.register(new CosmeticLayer(avatar));
               }
            })
      );
      LOGGER.info(
         "Maneszn Styles geladen: {} Kategorien, {} Styles, {} Cosmetics",
         new Object[]{StyleRegistry.get().categories().size(), StyleRegistry.get().styleCount(), CosmeticRegistry.get().count()}
      );
   }

   private static void safe(String name, Runnable r) {
      try {
         r.run();
      } catch (Throwable var3) {
         if (FAILED_TICKS.add(name)) {
            LOGGER.error("Maneszn Styles: Fehler in '{}'", name, var3);
         }
      }
   }

   public static void openScreen(Minecraft client, Supplier<Screen> s) {
      try {
         client.setScreen(s.get());
      } catch (Throwable var3) {
         LOGGER.error("Maneszn Styles: Bildschirm konnte nicht geöffnet werden", var3);
         if (client.player != null) {
            client.player.displayClientMessage(Component.literal("§c[Maneszn Styles] Menü konnte nicht geöffnet werden: " + var3), false);
         }
      }
   }

   private static boolean pressed(Minecraft client, KeyMapping m) {
      boolean clicked = false;

      while (m.consumeClick()) {
         clicked = true;
      }

      boolean raw = false;

      try {
         Key key = KeyBindingHelper.getBoundKeyOf(m);
         if (key != null && key.getType() == Type.KEYSYM && key.getValue() > 0) {
            long window = GLFW.glfwGetCurrentContext();
            if (window != 0L) {
               raw = GLFW.glfwGetKey(window, key.getValue()) == 1;
            }
         }
      } catch (Throwable var7) {
      }

      boolean before = Boolean.TRUE.equals(RAW_DOWN.put(m, raw));
      if (!clicked && raw && !before && client.screen == null) {
         clicked = true;
      }

      return clicked;
   }

   private static void migrateKeys(Minecraft client) {
      ModConfig cfg = ModConfig.get();
      if (cfg.keyVersion < 1 && client.options != null) {
         openMenuKey.setKey(Type.KEYSYM.getOrCreate(75));
         emoteKey.setKey(Type.KEYSYM.getOrCreate(66));
         KeyMapping.resetMapping();
         client.options.save();
         cfg.keyVersion = 1;
         ModConfig.save();
      }
   }

   private static void handleKeys(Minecraft client) {
      if (pressed(client, openMenuKey) && client.screen == null) {
         openScreen(client, () -> new StylesScreen(null));
      }

      if (pressed(client, emoteKey) && client.screen == null && client.player != null) {
         if (EmoteManager.local() != null) {
            EmoteManager.stop();
         } else {
            openScreen(client, EmoteScreen::new);
         }
      }

      if (pressed(client, hudKey) && client.screen == null) {
         openScreen(client, () -> new HudEditScreen(null));
      }

      if (pressed(client, fullbrightKey)) {
         ModConfig cfg = ModConfig.get();
         cfg.fullbright = !cfg.fullbright;
         ModConfig.save();
         if (client.player != null) {
            client.player
               .displayClientMessage(
                  Component.translatable(cfg.fullbright ? "maneszn_styles.toast.fullbright_on" : "maneszn_styles.toast.fullbright_off"), true
               );
         }
      }
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("maneszn_styles", path);
   }
}
