package dev.maneszn.advent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import net.fabricmc.loader.api.FabricLoader;

/** config/adventcosmetics.json – "timeZone": "server" (default) or an IANA zone like "Europe/Berlin". */
public final class AdventConfig {
	public String timeZone = "server";
	public boolean allowCatchUp = true;

	public static AdventConfig load() {
		Path p = FabricLoader.getInstance().getConfigDir().resolve("adventcosmetics.json");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try {
			if (Files.exists(p)) return gson.fromJson(Files.readString(p), AdventConfig.class);
			AdventConfig def = new AdventConfig();
			Files.writeString(p, gson.toJson(def));
			return def;
		} catch (Exception e) {
			AdventMod.LOG.warn("Could not read {} – using defaults", p, e);
			return new AdventConfig();
		}
	}

	public ZoneId zone() {
		if (timeZone == null || timeZone.equalsIgnoreCase("server")) return ZoneId.systemDefault();
		try { return ZoneId.of(timeZone); } catch (Exception e) { AdventMod.LOG.warn("Unknown timeZone '{}', using server zone", timeZone); return ZoneId.systemDefault(); }
	}
}
