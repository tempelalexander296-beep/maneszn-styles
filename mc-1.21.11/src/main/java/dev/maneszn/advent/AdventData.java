package dev.maneszn.advent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.maneszn.advent.core.PlayerAdvent;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Immutable attachment value (persisted + synced to every client tracking the player). */
public record AdventData(int claimedMask, String equipped) {
	public static final AdventData EMPTY = new AdventData(0, "");

	public static final Codec<AdventData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.optionalFieldOf("claimed", 0).forGetter(AdventData::claimedMask),
		Codec.STRING.optionalFieldOf("equipped", "").forGetter(AdventData::equipped)
	).apply(i, AdventData::new));

	public static final StreamCodec<ByteBuf, AdventData> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, AdventData::claimedMask,
		ByteBufCodecs.STRING_UTF8, AdventData::equipped,
		AdventData::new);

	public PlayerAdvent toLogic() {
		PlayerAdvent p = new PlayerAdvent();
		p.setClaimedMask(claimedMask);
		p.loadEquipped(equipped);
		return p;
	}

	public static AdventData of(PlayerAdvent p) { return new AdventData(p.claimedMask(), p.equippedString()); }
}
