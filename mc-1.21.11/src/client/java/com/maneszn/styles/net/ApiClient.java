package com.maneszn.styles.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.ModPaths;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.emote.EmoteManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;

public final class ApiClient {
   private static final ApiClient INSTANCE = new ApiClient();
   private static final long HEARTBEAT_MS = 60000L;
   private static final long LOOKUP_MS = 5000L;
   private static final long REFRESH_MS = 120000L;
   private static final long STATS_MS = 60000L;
   private static final long KNOWN_KEEP_MS = 1209600000L;
   private final ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "Maneszn-API");
      t.setDaemon(true);
      return t;
   });
   private final Map<UUID, ApiClient.Entry> players = new ConcurrentHashMap<>();
   private final Map<String, UUID> byName = new ConcurrentHashMap<>();
   private volatile ApiClient.Status status = ApiClient.Status.DISABLED;
   private volatile int online = -1;
   private volatile String lastError = "";
   private volatile String token;
   private volatile boolean busy;
   private volatile boolean cosmeticsDirty = true;
   private volatile Set<String> entitlements = Set.of();
   private volatile boolean admin;
   private volatile boolean entsLoaded;
   private ApiProtocol proto;
   private String protoUrl = "";
   private long nextLoginAt;
   private long nextHeartbeatAt;
   private long nextLookupAt;
   private long nextStatsAt;
   private long nextSaveAt;
   private long nextEmotesAt;
   private long backoff = 15000L;
   private boolean knownDirty;
   private static final Set<UUID> BUILTIN_OWNERS = Set.of(UUID.fromString("af302e87-42d2-4014-a8f7-287656ec032d"));
   private static final Set<UUID> BUILTIN_ADMINS = Set.of(
      UUID.fromString("5b1c60b6-2a72-4062-92f3-060757ef77fb"),
      UUID.fromString("f0531c06-06c8-4337-9df1-a4c757725c9c"),
      UUID.fromString("6c922e52-aa16-4f7f-98cf-e236e129fa7a")
   );

   private ApiClient() {
   }

   public static ApiClient get() {
      return INSTANCE;
   }

   public ApiClient.Status status() {
      return this.status;
   }

   public int online() {
      return this.online;
   }

   public String lastError() {
      return this.lastError;
   }

   private static String builtinRank() {
      UUID self = selfId();
      if (self == null) {
         return null;
      } else if (BUILTIN_OWNERS.contains(self)) {
         return "owner";
      } else {
         return BUILTIN_ADMINS.contains(self) ? "admin" : null;
      }
   }

   public boolean entitled(String requires) {
      if (requires != null && !requires.isEmpty()) {
         String rank = builtinRank();
         if ("owner".equals(rank)) {
            return true;
         } else if ("admin".equals(rank) && !"owner".equals(requires)) {
            return true;
         } else if (!this.entsLoaded) {
            return ModConfig.get().entCache.contains(requires);
         } else {
            return "owner".equals(requires) ? this.entitlements.contains("owner") : this.admin || this.entitlements.contains(requires);
         }
      } else {
         return true;
      }
   }

   public boolean isOwner() {
      return this.entitled("owner");
   }

   public boolean isAdmin() {
      if (builtinRank() != null) {
         return true;
      } else {
         return this.entsLoaded ? this.admin : ModConfig.get().entCache.contains("admin");
      }
   }

   public boolean loggedIn() {
      return this.token != null;
   }

   public void redeem(String code, String name, BiConsumer<Boolean, String[]> onDone) {
      String t = this.token;
      ApiProtocol p = this.proto;
      if (t != null && p != null) {
         this.exec.submit(() -> {
            boolean ok = false;

            String[] res;
            try {
               res = p.redeem(t, code, name);
               rememberTicket(res.length > 2 ? res[2] : null);
               ok = true;
               this.nextHeartbeatAt = 0L;
            } catch (Exception var10) {
               res = new String[]{var10.getMessage() == null ? var10.toString() : var10.getMessage()};
            }

            boolean fOk = ok;
            String[] fRes = res;
            Minecraft.getInstance().execute(() -> onDone.accept(fOk, fRes));
         });
      } else {
         onDone.accept(false, new String[]{"offline"});
      }
   }

   public void adminCodes(String reward, int count, int uses, BiConsumer<List<String>, String> onDone) {
      String t = this.token;
      ApiProtocol p = this.proto;
      if (t != null && p != null) {
         this.exec.submit(() -> {
            List<String> codes = null;
            String err = "";

            try {
               codes = p.adminCodes(t, reward, count, uses);
            } catch (Exception var10) {
               err = var10.getMessage() == null ? var10.toString() : var10.getMessage();
            }

            List<String> fc = codes;
            String fe = err;
            Minecraft.getInstance().execute(() -> onDone.accept(fc, fe));
         });
      } else {
         onDone.accept(null, "offline");
      }
   }

   public void adminGrant(String name, String ent, boolean revoke, Consumer<String> onDone) {
      String t = this.token;
      ApiProtocol p = this.proto;
      if (t != null && p != null) {
         this.exec.submit(() -> {
            String err = "";

            try {
               rememberTicket(p.adminGrant(t, name, ent, revoke));
            } catch (Exception var8) {
               err = var8.getMessage() == null ? var8.toString() : var8.getMessage();
            }

            String fe = err;
            Minecraft.getInstance().execute(() -> onDone.accept(fe));
         });
      } else {
         onDone.accept("offline");
      }
   }

   private static void rememberTicket(String ticket) {
      if (ticket != null && !ticket.isBlank()) {
         Minecraft.getInstance().execute(() -> {
            List<String> list = ModConfig.get().rankTickets;
            if (!list.contains(ticket)) {
               list.add(ticket);

               while (list.size() > 100) {
                  list.remove(0);
               }

               ModConfig.save();
            }
         });
      }
   }

   public static UUID selfId() {
      User u = Minecraft.getInstance().getUser();
      return u == null ? null : u.getProfileId();
   }

   public static boolean isSelf(UUID id) {
      return id != null && id.equals(selfId());
   }

   public boolean isModUser(UUID id) {
      if (id == null) {
         return false;
      } else if (isSelf(id)) {
         return true;
      } else {
         ApiClient.Entry e = this.players.get(id);
         return e != null && e.isUser();
      }
   }

   public Map<String, String> cosmeticsOf(UUID id) {
      ApiClient.Entry e = id == null ? null : this.players.get(id);
      return e != null && e.isUser() ? e.cosmetics() : Map.of();
   }

   public Map<String, UUID> namesIndex() {
      return this.byName;
   }

   public void markCosmeticsDirty() {
      this.cosmeticsDirty = true;
   }

   public void reconnect() {
      String old = this.token;
      this.token = null;
      this.nextLoginAt = 0L;
      this.backoff = 15000L;
      this.nextStatsAt = 0L;
      if (old != null && this.proto != null) {
         ApiProtocol p = this.proto;
         this.exec.submit(() -> {
            try {
               p.logout(old);
            } catch (Exception var3) {
            }
         });
      }
   }

   public void start() {
      this.loadKnown();
   }

   public void tick(Minecraft mc) {
      long now = System.currentTimeMillis();
      ModConfig cfg = ModConfig.get();
      if (this.knownDirty && now > this.nextSaveAt) {
         this.saveKnown(now);
      }

      String url = cfg.apiUrl == null ? "" : cfg.apiUrl.trim();
      if (url.isEmpty()) {
         this.status = ApiClient.Status.DISABLED;
         this.token = null;
      } else {
         if (!url.equals(this.protoUrl)) {
            this.proto = new ApiProtocol(url, version());
            this.protoUrl = url;
            this.token = null;
            this.nextLoginAt = 0L;
         }

         if (!this.busy) {
            if (cfg.anonymous) {
               if (this.token != null) {
                  this.reconnect();
               }

               if (this.status != ApiClient.Status.OFFLINE) {
                  this.status = ApiClient.Status.ANONYMOUS;
               }

               if (now >= this.nextStatsAt) {
                  this.nextStatsAt = now + 60000L;
                  this.run(() -> {
                     this.online = this.proto.stats();
                     this.status = ApiClient.Status.ANONYMOUS;
                  });
                  return;
               }
            } else if (this.token == null) {
               if (now >= this.nextLoginAt) {
                  this.login(mc);
                  return;
               }
            } else if (now >= this.nextHeartbeatAt || this.cosmeticsDirty) {
               this.heartbeat();
               return;
            }

            if (now >= this.nextLookupAt) {
               this.nextLookupAt = now + 5000L;
               Set<UUID> want = this.collectPlayers(mc, now);
               if (!want.isEmpty()) {
                  this.lookup(want);
                  return;
               }
            }

            if (now >= this.nextEmotesAt && mc.level != null) {
               this.nextEmotesAt = now + 1000L;
               Set<UUID> near = new HashSet<>();
               UUID self = selfId();

               for (AbstractClientPlayer p : mc.level.players()) {
                  UUID id = p.getUUID();
                  if (!id.equals(self) && this.isModUser(id)) {
                     near.add(id);
                  }
               }

               if (!near.isEmpty()) {
                  ApiProtocol px = this.proto;
                  this.run(() -> {
                     Map<UUID, Object[]> r = px.emotes(near);
                     long t = System.currentTimeMillis();
                     Map<UUID, EmoteManager.Active> act = new HashMap<>();

                     for (Map.Entry<UUID, Object[]> e : r.entrySet()) {
                        String idx = (String)e.getValue()[0];
                        if (!idx.isEmpty()) {
                           act.put(e.getKey(), new EmoteManager.Active(idx, t - (Long)e.getValue()[1]));
                        }
                     }

                     EmoteManager.setRemote(act);
                  });
               } else {
                  EmoteManager.setRemote(Map.of());
               }
            }
         }
      }
   }

   public void uploadCape(byte[] png, Consumer<Boolean> onDone) {
      String t = this.token;
      ApiProtocol p = this.proto;
      if (t != null && p != null && !ModConfig.get().anonymous) {
         this.exec.submit(() -> {
            try {
               p.uploadCape(t, png);
               onDone.accept(true);
            } catch (Exception var6) {
               ManesznStyles.LOGGER.warn("Umhang nicht hochgeladen: {}", var6.toString());
               this.lastError = var6.getMessage();
               onDone.accept(false);
            }
         });
      } else {
         onDone.accept(false);
      }
   }

   public void downloadCape(String hash, Consumer<byte[]> onDone) {
      ApiProtocol p = this.proto;
      if (p == null) {
         onDone.accept(null);
      } else {
         this.exec.submit(() -> {
            try {
               onDone.accept(p.downloadCape(hash));
            } catch (Exception var4) {
               ManesznStyles.LOGGER.debug("Umhang {} nicht geladen: {}", hash, var4.toString());
               onDone.accept(null);
            }
         });
      }
   }

   public void sendEmote(String id) {
      String t = this.token;
      ApiProtocol p = this.proto;
      if (t != null && p != null && !ModConfig.get().anonymous) {
         this.exec.submit(() -> {
            try {
               p.emote(t, id);
            } catch (Exception var4) {
               ManesznStyles.LOGGER.debug("Emote nicht gesendet: {}", var4.toString());
            }
         });
      }
   }

   private void login(Minecraft mc) {
      User u = mc.getUser();
      if (u != null) {
         UUID id = u.getProfileId();
         String name = u.getName();
         String access = u.getAccessToken();
         this.status = ApiClient.Status.CONNECTING;
         ApiProtocol p = this.proto;
         this.busy = true;
         this.exec.submit(() -> {
            try {
               String serverId = p.challenge(id, name);
               p.mojangJoin(access, id, serverId);
               this.token = p.verify(name, serverId);
               this.status = ApiClient.Status.ONLINE;
               this.lastError = "";
               this.backoff = 15000L;
               this.nextHeartbeatAt = 0L;
               this.cosmeticsDirty = true;
               ManesznStyles.LOGGER.info("Maneszn API: angemeldet als {}", name);
            } catch (ApiProtocol.AuthException var10) {
               this.status = ApiClient.Status.AUTH_FAILED;
               this.lastError = var10.getMessage();
               this.nextLoginAt = System.currentTimeMillis() + 600000L;
               ManesznStyles.LOGGER.warn("Maneszn API: Anmeldung fehlgeschlagen: {}", var10.getMessage());
            } catch (Exception var11) {
               this.fail(var11);
               this.nextLoginAt = System.currentTimeMillis() + this.backoff;
               this.backoff = Math.min(this.backoff * 2L, 300000L);
            } finally {
               this.busy = false;
            }
         });
      }
   }

   private void heartbeat() {
      Map<String, String> mine = new LinkedHashMap<>(ModConfig.get().cosmetics);
      this.cosmeticsDirty = false;
      String t = this.token;
      ApiProtocol p = this.proto;
      this.nextHeartbeatAt = System.currentTimeMillis() + 60000L;
      this.run(() -> {
         try {
            ApiProtocol.Beat beat = p.heartbeat(t, mine, new ArrayList<>(ModConfig.get().rankTickets));
            updateTickets(beat);
            this.online = beat.online();
            this.entitlements = Set.copyOf(beat.entitlements());
            this.admin = beat.admin();
            this.entsLoaded = true;
            List<String> cache = new ArrayList<>(beat.entitlements());
            Collections.sort(cache);
            if (!cache.equals(ModConfig.get().entCache)) {
               ModConfig.get().entCache = cache;
               Minecraft.getInstance().execute(ModConfig::save);
            }

            this.status = ApiClient.Status.ONLINE;
            this.lastError = "";
         } catch (ApiProtocol.AuthException var6) {
            this.token = null;
            this.nextLoginAt = 0L;
         }
      });
   }

   private static void updateTickets(ApiProtocol.Beat beat) {
      UUID self = selfId();
      Minecraft.getInstance().execute(() -> {
         List<String> list = ModConfig.get().rankTickets;
         boolean changed = false;

         for (String t : beat.tickets()) {
            if (!list.contains(t)) {
               list.add(t);
               changed = true;
            }
         }

         if (self != null) {
            for (String ent : beat.revoked()) {
               changed |= list.removeIf(tx -> tx.startsWith(self + "." + ent + "."));
            }
         }

         if (changed) {
            ModConfig.save();
         }
      });
   }

   private void lookup(Set<UUID> want) {
      ApiProtocol p = this.proto;
      this.run(() -> {
         List<ApiProtocol.Remote> found = p.lookup(want);
         long now = System.currentTimeMillis();
         Set<UUID> hit = new HashSet<>();

         for (ApiProtocol.Remote r : found) {
            hit.add(r.uuid());
            this.put(r.uuid(), new ApiClient.Entry(r.name(), Map.copyOf(r.cosmetics()), now, true));
         }

         for (UUID u : want) {
            if (!hit.contains(u)) {
               ApiClient.Entry old = this.players.get(u);
               this.put(u, new ApiClient.Entry(old == null ? "" : old.name(), Map.of(), now, false));
            }
         }

         if (this.status == ApiClient.Status.OFFLINE) {
            this.status = this.token != null ? ApiClient.Status.ONLINE : (ModConfig.get().anonymous ? ApiClient.Status.ANONYMOUS : ApiClient.Status.CONNECTING);
         }

         this.knownDirty = true;
      });
   }

   private Set<UUID> collectPlayers(Minecraft mc, long now) {
      Set<UUID> out = new HashSet<>();
      UUID self = selfId();
      ClientPacketListener conn = mc.getConnection();
      if (conn != null) {
         for (PlayerInfo info : conn.getOnlinePlayers()) {
            UUID id = Profiles.id(info.getProfile());
            if (id != null && !id.equals(self) && this.stale(id, now)) {
               out.add(id);
            }

            if (out.size() >= 400) {
               break;
            }
         }
      }

      if (mc.level != null) {
         for (AbstractClientPlayer p : mc.level.players()) {
            UUID idx = p.getUUID();
            if (!idx.equals(self) && this.stale(idx, now)) {
               out.add(idx);
            }
         }
      }

      return out;
   }

   private boolean stale(UUID id, long now) {
      ApiClient.Entry e = this.players.get(id);
      return e == null || now - e.fetchedAt() > 120000L;
   }

   private void put(UUID id, ApiClient.Entry e) {
      ApiClient.Entry old = this.players.put(id, e);
      if (old != null && !old.name().isEmpty()) {
         this.byName.remove(old.name().toLowerCase(Locale.ROOT), id);
      }

      if (e.isUser() && !e.name().isEmpty()) {
         this.byName.put(e.name().toLowerCase(Locale.ROOT), id);
      }
   }

   private void run(ApiClient.Job job) {
      this.busy = true;
      this.exec.submit(() -> {
         try {
            job.run();
         } catch (Exception var6) {
            this.fail(var6);
         } finally {
            this.busy = false;
         }
      });
   }

   private void fail(Exception e) {
      if (this.status != ApiClient.Status.OFFLINE) {
         ManesznStyles.LOGGER.warn("Maneszn API nicht erreichbar: {}", e.toString());
      }

      this.status = ApiClient.Status.OFFLINE;
      this.lastError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
   }

   private static String version() {
      return FabricLoader.getInstance().getModContainer("maneszn_styles").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
   }

   private void loadKnown() {
      Path f = ModPaths.knownUsersFile();
      if (Files.exists(f)) {
         try {
            JsonObject root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
            long now = System.currentTimeMillis();

            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
               JsonObject o = e.getValue().getAsJsonObject();
               long seen = o.has("seen") ? o.get("seen").getAsLong() : 0L;
               if (now - seen <= 1209600000L) {
                  Map<String, String> cos = new LinkedHashMap<>();
                  if (o.has("cosmetics")) {
                     for (Map.Entry<String, JsonElement> c : o.getAsJsonObject("cosmetics").entrySet()) {
                        cos.put(c.getKey(), c.getValue().getAsString());
                     }
                  }

                  this.put(
                     UUID.fromString(e.getKey()),
                     new ApiClient.Entry(o.has("name") ? o.get("name").getAsString() : "", Map.copyOf(cos), Math.min(seen, now - 120000L - 1L), true)
                  );
               }
            }
         } catch (Exception var13) {
            ManesznStyles.LOGGER.warn("known_users.json nicht lesbar: {}", var13.toString());
         }
      }
   }

   private void saveKnown(long now) {
      this.knownDirty = false;
      this.nextSaveAt = now + 30000L;
      JsonObject root = new JsonObject();

      for (Map.Entry<UUID, ApiClient.Entry> e : this.players.entrySet()) {
         ApiClient.Entry en = e.getValue();
         if (en.isUser()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", en.name());
            o.addProperty("seen", en.fetchedAt());
            JsonObject c = new JsonObject();

            for (Map.Entry<String, String> ce : en.cosmetics().entrySet()) {
               c.addProperty(ce.getKey(), ce.getValue());
            }

            o.add("cosmetics", c);
            root.add(e.getKey().toString(), o);
         }
      }

      String json = ModConfig.GSON.toJson(root);
      this.exec.submit(() -> {
         try {
            Files.writeString(ModPaths.knownUsersFile(), json, StandardCharsets.UTF_8);
         } catch (Exception var2) {
            ManesznStyles.LOGGER.warn("known_users.json nicht speicherbar: {}", var2.toString());
         }
      });
   }

   public record Entry(String name, Map<String, String> cosmetics, long fetchedAt, boolean isUser) {
   }

   private interface Job {
      void run() throws Exception;
   }

   public static enum Status {
      DISABLED,
      ANONYMOUS,
      CONNECTING,
      ONLINE,
      OFFLINE,
      AUTH_FAILED;
   }
}
