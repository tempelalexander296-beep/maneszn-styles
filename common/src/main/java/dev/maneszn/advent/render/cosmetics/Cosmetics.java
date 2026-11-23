package dev.maneszn.advent.render.cosmetics;

import dev.maneszn.advent.render.CosmeticModel;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** reward id → lazily built 3D model (geometry is generated once, on first use). */
public final class Cosmetics {
	private static final Map<String, Supplier<CosmeticModel>> FACTORIES = new HashMap<>();
	private static final Map<String, CosmeticModel> CACHE = new HashMap<>();

	static {
		FACTORIES.put("snowflake_wings", BackCosmetics.SnowflakeWings::new);
		FACTORIES.put("christmas_trail", AuraTrailCosmetics.ChristmasTrail::new);
		FACTORIES.put("candy_cane_sword", HandFloatCosmetics.CandyCaneSword::new);
		FACTORIES.put("snowfall_aura", AuraTrailCosmetics.SnowfallAura::new);
		FACTORIES.put("gift_backpack", BackCosmetics.GiftBackpack::new);
		FACTORIES.put("frozen_crown", HeadCosmetics.FrozenCrown::new);
		FACTORIES.put("reindeer_horns", HeadCosmetics.ReindeerHorns::new);
		FACTORIES.put("candy_cane_wings", BackCosmetics.CandyCaneWings::new);
		FACTORIES.put("snow_globe", HandFloatCosmetics.SnowGlobe::new);
		FACTORIES.put("christmas_halo", HeadCosmetics.ChristmasHalo::new);
		FACTORIES.put("santa_hat", HeadCosmetics.SantaHat::new);
		FACTORIES.put("ice_crystal_trail", AuraTrailCosmetics.IceCrystalTrail::new);
		FACTORIES.put("golden_bell", HandFloatCosmetics.GoldenBells::new);
		FACTORIES.put("present_hammer", HandFloatCosmetics.PresentHammer::new);
		FACTORIES.put("reindeer_companion", CompanionCosmetics.Reindeer::new);
		FACTORIES.put("frozen_wings", BackCosmetics.FrozenWings::new);
		FACTORIES.put("christmas_lantern", HandFloatCosmetics.ChristmasLantern::new);
		FACTORIES.put("snowman_companion", CompanionCosmetics.Snowman::new);
		FACTORIES.put("golden_gift_wings", BackCosmetics.GoldenGiftWings::new);
		FACTORIES.put("winter_storm_aura", AuraTrailCosmetics.WinterStormAura::new);
		FACTORIES.put("santas_sleigh_trail", AuraTrailCosmetics.SleighTrail::new);
		FACTORIES.put("christmas_dragon", CompanionCosmetics.Dragon::new);
		FACTORIES.put("legendary_ice_crown", HeadCosmetics.LegendaryIceCrown::new);
		FACTORIES.put("ultimate_christmas_aura", UltimateChristmasAura::new);
	}

	private Cosmetics() {}

	public static synchronized CosmeticModel get(String id) {
		CosmeticModel m = CACHE.get(id);
		if (m == null) {
			Supplier<CosmeticModel> f = FACTORIES.get(id);
			if (f == null) return null;
			m = f.get(); CACHE.put(id, m);
		}
		return m;
	}

	public static java.util.Set<String> ids() { return FACTORIES.keySet(); }
}
