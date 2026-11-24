package com.maneszn.styles.compat;

import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.emote.Emote;
import com.maneszn.styles.emote.EmoteManager;
import com.maneszn.styles.emote.Emotes;
import com.maneszn.styles.gui.EmoteScreen;
import com.maneszn.styles.gui.HudEditScreen;
import com.maneszn.styles.gui.StylesScreen;
import com.maneszn.styles.tools.AutoTpa;
import com.maneszn.styles.tools.CalcScreen;
import com.maneszn.styles.tools.MapartScreen;
import com.maneszn.styles.tools.Market;
import com.maneszn.styles.tools.MarketScreen;
import com.maneszn.styles.tools.TrimScreen;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.Game;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;

public final class Access {
   private static Supplier<Screen> pending;

   private Access() {
   }

   public static void register() {
      ScreenEvents.AFTER_INIT
         .register(
            (AfterInit)(client, screen, w, h) -> {
               if (ModConfig.get().calcButton) {
                  if (screen instanceof AbstractContainerScreen) {
                     if (!(screen instanceof CreativeModeInventoryScreen)) {
                        Screens.getButtons(screen)
                           .add(
                              Button.builder(Component.translatable("maneszn_styles.calc.button"), b -> client.setScreen(new CalcScreen(screen)))
                                 .bounds(4, 4, 60, 16)
                                 .build()
                           );
                     }
                  }
               }
            }
         );
      ClientReceiveMessageEvents.GAME.register((Game)(message, overlay) -> {
         if (!overlay) {
            AutoTpa.onMessage(message);
         }
      });
      ItemTooltipCallback.EVENT.register((ItemTooltipCallback)(stack, context, type, lines) -> {
         Component c = Market.tooltip(stack);
         if (c != null) {
            lines.add(c);
         }
      });
      ScreenEvents.AFTER_INIT
         .register(
            (AfterInit)(client, screen, w, h) -> {
               boolean pause = screen instanceof PauseScreen || screen.getTitle().getString().equals(Component.translatable("menu.game").getString());
               if (pause) {
                  Screens.getButtons(screen)
                     .add(
                        Button.builder(Component.literal("✦ Maneszn Styles"), b -> ManesznStyles.openScreen(client, () -> new StylesScreen(screen)))
                           .bounds(6, 6, 110, 20)
                           .build()
                     );
                  Screens.getButtons(screen).add(Button.builder(Component.translatable("maneszn_styles.access.emotes"), b -> {
                     client.setScreen(null);
                     pending = EmoteScreen::new;
                  }).bounds(6, 28, 110, 20).build());
               }
            }
         );
      ClientCommandRegistrationCallback.EVENT
         .register(
            (ClientCommandRegistrationCallback)(dispatcher, registryAccess) -> {
               for (String name : new String[]{"maneszn", "ms"}) {
                  dispatcher.register(
                     (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommandManager.literal(
                                                         name
                                                      )
                                                      .executes(ctx -> open(() -> new StylesScreen(null))))
                                                   .then(ClientCommandManager.literal("menu").executes(ctx -> open(() -> new StylesScreen(null)))))
                                                .then(ClientCommandManager.literal("hud").executes(ctx -> open(() -> new HudEditScreen(null)))))
                                             .then(ClientCommandManager.literal("rechner").executes(ctx -> open(() -> new CalcScreen(null)))))
                                          .then(ClientCommandManager.literal("markt").executes(ctx -> open(() -> new MarketScreen(null, "market")))))
                                       .then(ClientCommandManager.literal("geld").executes(ctx -> open(() -> new MarketScreen(null, "money")))))
                                    .then(ClientCommandManager.literal("trims").executes(ctx -> open(() -> new TrimScreen(null)))))
                                 .then(ClientCommandManager.literal("mapart").executes(ctx -> open(() -> new MapartScreen(null)))))
                              .then(
                                 ((LiteralArgumentBuilder)((LiteralArgumentBuilder)ClientCommandManager.literal("tpa")
                                          .then(
                                             ClientCommandManager.literal("add")
                                                .then(
                                                   ClientCommandManager.argument("name", StringArgumentType.word())
                                                      .executes(ctx -> tpa(StringArgumentType.getString(ctx, "name"), true))
                                                )
                                          ))
                                       .then(
                                          ClientCommandManager.literal("remove")
                                             .then(
                                                ClientCommandManager.argument("name", StringArgumentType.word())
                                                   .executes(ctx -> tpa(StringArgumentType.getString(ctx, "name"), false))
                                             )
                                       ))
                                    .then(ClientCommandManager.literal("list").executes(ctx -> tpa(null, false)))
                              ))
                           .then(ClientCommandManager.literal("stop").executes(ctx -> {
                              EmoteManager.stop();
                              return 1;
                           })))
                        .then(
                           ((LiteralArgumentBuilder)ClientCommandManager.literal("emote").executes(ctx -> open(EmoteScreen::new)))
                              .then(ClientCommandManager.argument("id", StringArgumentType.word()).suggests((ctx, b) -> {
                                 for (Emote e : Emotes.all()) {
                                    b.suggest(e.id());
                                 }

                                 return b.buildFuture();
                              }).executes(ctx -> {
                                 String id = StringArgumentType.getString(ctx, "id");
                                 Minecraft.getInstance().execute(() -> EmoteManager.play(id));
                                 return 1;
                              }))
                        )
                  );
               }
            }
         );
   }

   private static int tpa(String name, boolean add) {
      ModConfig cfg = ModConfig.get();
      Minecraft mc = Minecraft.getInstance();
      if (name != null) {
         cfg.tpaFriends.removeIf(f -> f.equalsIgnoreCase(name));
         if (add) {
            cfg.tpaFriends.add(name);
         }

         ModConfig.save();
      }

      if (mc.player != null) {
         mc.player
            .displayClientMessage(
               Component.literal(
                  "§d[Maneszn] §fAuto-TPA "
                     + (cfg.autoTpa ? "§aAN" : "§cAUS")
                     + "§f – Freunde: §e"
                     + (cfg.tpaFriends.isEmpty() ? "–" : String.join(", ", cfg.tpaFriends))
               ),
               false
            );
      }

      return 1;
   }

   private static int open(Supplier<Screen> s) {
      pending = s;
      return 1;
   }

   public static void tick(Minecraft mc) {
      if (pending != null && mc.screen == null) {
         Supplier<Screen> s = pending;
         pending = null;
         ManesznStyles.openScreen(mc, s);
      }
   }
}
