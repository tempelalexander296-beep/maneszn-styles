package dev.maneszn.advent.net;

import dev.maneszn.advent.AdventMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** All custom packets. The server is authoritative for dates and claims. */
public final class Payloads {
	private Payloads() {}

	/** S2C: server clock snapshot (+ optionally open the calendar). */
	public record Clock(int currentDay, long serverNowMs, long nextUnlockMs, long christmasEveMs, String zone, boolean open) implements CustomPacketPayload {
		public static final Type<Clock> TYPE = new Type<>(AdventMod.id("clock"));
		public static final StreamCodec<ByteBuf, Clock> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Clock::currentDay, ByteBufCodecs.VAR_LONG, Clock::serverNowMs, ByteBufCodecs.VAR_LONG, Clock::nextUnlockMs,
			ByteBufCodecs.VAR_LONG, Clock::christmasEveMs, ByteBufCodecs.STRING_UTF8, Clock::zone, ByteBufCodecs.BOOL, Clock::open, Clock::new);
		@Override public Type<Clock> type() { return TYPE; }
	}

	/** C2S: open door `day`. */
	public record Claim(int day) implements CustomPacketPayload {
		public static final Type<Claim> TYPE = new Type<>(AdventMod.id("claim"));
		public static final StreamCodec<ByteBuf, Claim> CODEC = ByteBufCodecs.VAR_INT.map(Claim::new, Claim::day);
		@Override public Type<Claim> type() { return TYPE; }
	}

	/** S2C: result of a claim (ordinal of PlayerAdvent.ClaimResult). */
	public record ClaimResult(int day, int result) implements CustomPacketPayload {
		public static final Type<ClaimResult> TYPE = new Type<>(AdventMod.id("claim_result"));
		public static final StreamCodec<ByteBuf, ClaimResult> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, ClaimResult::day, ByteBufCodecs.VAR_INT, ClaimResult::result, ClaimResult::new);
		@Override public Type<ClaimResult> type() { return TYPE; }
	}

	/** C2S: equip (id) or unequip (empty id) a slot. */
	public record Equip(int slot, String id) implements CustomPacketPayload {
		public static final Type<Equip> TYPE = new Type<>(AdventMod.id("equip"));
		public static final StreamCodec<ByteBuf, Equip> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Equip::slot, ByteBufCodecs.STRING_UTF8, Equip::id, Equip::new);
		@Override public Type<Equip> type() { return TYPE; }
	}
}
