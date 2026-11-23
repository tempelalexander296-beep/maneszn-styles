package dev.maneszn.advent;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.maneszn.advent.core.AdventClock;
import dev.maneszn.advent.core.CosmeticSlot;
import dev.maneszn.advent.core.PlayerAdvent;
import dev.maneszn.advent.core.Reward;
import dev.maneszn.advent.core.Rewards;
import dev.maneszn.advent.net.Payloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AdventMod implements ModInitializer {
	public static final String MOD_ID = "adventcosmetics";
	public static final Logger LOG = LoggerFactory.getLogger("AdventCosmetics");
	public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }

	public static final AttachmentType<AdventData> DATA = AttachmentRegistry.<AdventData>builder()
		.persistent(AdventData.CODEC)
		.copyOnDeath()
		.initializer(() -> AdventData.EMPTY)
		.syncWith(AdventData.STREAM_CODEC, AttachmentSyncPredicate.all())
		.buildAndRegister(id("data"));

	public static AdventConfig config;
	public static AdventClock clock;
	private static int lastDay = -1;

	@Override
	public void onInitialize() {
		config = AdventConfig.load();
		clock = new AdventClock(config.zone());
		PayloadTypeRegistry.clientboundPlay().register(Payloads.Clock.TYPE, Payloads.Clock.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Payloads.ClaimResult.TYPE, Payloads.ClaimResult.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Payloads.Claim.TYPE, Payloads.Claim.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Payloads.Equip.TYPE, Payloads.Equip.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(Payloads.Claim.TYPE, (payload, ctx) -> ctx.server().execute(() -> handleClaim(ctx.player(), payload.day())));
		ServerPlayNetworking.registerGlobalReceiver(Payloads.Equip.TYPE, (payload, ctx) -> ctx.server().execute(() -> handleEquip(ctx.player(), payload.slot(), payload.id())));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendClock(handler.getPlayer(), false));
		ServerTickEvents.END_SERVER_TICK.register(AdventMod::tick);
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> dispatcher.register(Commands.literal("advent")
			.executes(c -> { sendClock(c.getSource().getPlayerOrException(), true); return 1; })
			.then(Commands.literal("debug").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("day").then(Commands.argument("day", IntegerArgumentType.integer(0, 26)).executes(c -> {
					clock.travelTo(IntegerArgumentType.getInteger(c, "day"));
					broadcastClock(c.getSource().getServer());
					c.getSource().sendSuccess(() -> Component.literal("Advent clock → " + clock.now().toLocalDate() + " (day " + clock.currentDay() + ")"), true);
					return 1;
				})))
				.then(Commands.literal("realtime").executes(c -> { clock.setOverride(null); broadcastClock(c.getSource().getServer()); c.getSource().sendSuccess(() -> Component.literal("Advent clock → real time"), true); return 1; }))
				.then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player()).executes(c -> {
					ServerPlayer p = EntityArgument.getPlayer(c, "player");
					p.setAttached(DATA, AdventData.EMPTY);
					c.getSource().sendSuccess(() -> Component.literal("Advent progress reset for " + p.getName().getString()), true);
					return 1;
				}))))));
		LOG.info("Christmas Advent Calendar ready – unlock zone {}", clock.zone());
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) return;
		int day = clock.currentDay();
		if (day != lastDay) {
			boolean first = lastDay == -1; lastDay = day;
			if (!first) {
				broadcastClock(server);
				if (day >= 1 && day <= 24) {
					Reward r = Rewards.byDay(day);
					Component msg = Component.literal("🎁 Türchen " + day + " ist offen: ").withStyle(ChatFormatting.GOLD)
						.append(Component.literal(r.name()).withStyle(s -> s.withColor(TextColor.fromRgb(r.rarity().color & 0xFFFFFF)).withBold(true)))
						.append(Component.literal("  /advent").withStyle(ChatFormatting.GRAY));
					server.getPlayerList().broadcastSystemMessage(msg, false);
				}
			}
		}
	}

	public static void broadcastClock(MinecraftServer server) { for (ServerPlayer p : server.getPlayerList().getPlayers()) sendClock(p, false); }

	public static void sendClock(ServerPlayer p, boolean open) {
		ServerPlayNetworking.send(p, new Payloads.Clock(clock.currentDay(), clock.nowMs(), clock.nextUnlockMs(), clock.christmasEveMs(), clock.zone().getId(), open));
	}

	private static void handleClaim(ServerPlayer player, int day) {
		PlayerAdvent st = player.getAttachedOrElse(DATA, AdventData.EMPTY).toLogic();
		int current = clock.currentDay();
		PlayerAdvent.ClaimResult res = config.allowCatchUp || day == current ? st.claim(day, current) : PlayerAdvent.ClaimResult.LOCKED;
		if (res == PlayerAdvent.ClaimResult.OK) {
			player.setAttached(DATA, AdventData.of(st));
			Reward r = Rewards.byDay(day);
			player.level().playSound(null, player.blockPosition(), r.day() == 24 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
			if (r.day() == 24) player.level().getServer().getPlayerList().broadcastSystemMessage(Component.literal("✨ " + player.getName().getString() + " hat die ULTIMATE CHRISTMAS AURA freigeschaltet!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
		}
		ServerPlayNetworking.send(player, new Payloads.ClaimResult(day, res.ordinal()));
	}

	private static void handleEquip(ServerPlayer player, int slotOrdinal, String id) {
		if (slotOrdinal < 0 || slotOrdinal >= CosmeticSlot.values().length) return;
		PlayerAdvent st = player.getAttachedOrElse(DATA, AdventData.EMPTY).toLogic();
		if (st.equip(CosmeticSlot.values()[slotOrdinal], id)) player.setAttached(DATA, AdventData.of(st));
	}
}
