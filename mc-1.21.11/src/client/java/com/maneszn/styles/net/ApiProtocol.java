package com.maneszn.styles.net;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

public final class ApiProtocol {
   private static final Gson GSON = new Gson();
   private static final String MOJANG_JOIN = System.getProperty("maneszn.mojangJoinUrl", "https://sessionserver.mojang.com/session/minecraft/join");
   private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).followRedirects(Redirect.NORMAL).build();
   private final String base;
   private final String userAgent;

   public ApiProtocol(String baseUrl, String version) {
      String b = baseUrl.trim();

      while (b.endsWith("/")) {
         b = b.substring(0, b.length() - 1);
      }

      this.base = b;
      this.userAgent = "ManesznStyles/" + version;
   }

   public String challenge(UUID uuid, String name) throws IOException {
      JsonObject body = new JsonObject();
      body.addProperty("uuid", uuid.toString());
      body.addProperty("name", name);
      return this.post("/v1/auth/challenge", body, null).get("serverId").getAsString();
   }

   public void mojangJoin(String accessToken, UUID uuid, String serverId) throws IOException {
      JsonObject body = new JsonObject();
      body.addProperty("accessToken", accessToken);
      body.addProperty("selectedProfile", uuid.toString().replace("-", ""));
      body.addProperty("serverId", serverId);
      HttpResponse<String> r = this.send(
         HttpRequest.newBuilder(URI.create(MOJANG_JOIN)).header("Content-Type", "application/json").POST(BodyPublishers.ofString(GSON.toJson(body)))
      );
      if (r.statusCode() / 100 != 2) {
         throw new ApiProtocol.AuthException("Mojang-Anmeldung abgelehnt (" + r.statusCode() + ")");
      }
   }

   public String verify(String name, String serverId) throws IOException {
      JsonObject body = new JsonObject();
      body.addProperty("name", name);
      body.addProperty("serverId", serverId);
      return this.post("/v1/auth/verify", body, null).get("token").getAsString();
   }

   public void logout(String token) throws IOException {
      this.post("/v1/logout", new JsonObject(), token);
   }

   public ApiProtocol.Beat heartbeat(String token, Map<String, String> cosmetics, List<String> tickets, List<String> ledger) throws IOException {
      JsonObject body = new JsonObject();
      JsonObject c = new JsonObject();

      for (Entry<String, String> e : cosmetics.entrySet()) {
         c.addProperty(e.getKey(), e.getValue());
      }

      body.add("cosmetics", c);
      JsonArray ta = new JsonArray();

      for (String t : tickets) {
         ta.add(t);
      }

      body.add("tickets", ta);
      JsonArray la = new JsonArray();

      for (String t : ledger) {
         la.add(t);
      }

      body.add("ledger", la);
      JsonObject r = this.post("/v1/heartbeat", body, token);
      Set<String> ents = new HashSet<>();
      if (r.has("entitlements") && r.get("entitlements").isJsonArray()) {
         for (JsonElement e : r.getAsJsonArray("entitlements")) {
            ents.add(e.getAsString());
         }
      }

      return new ApiProtocol.Beat(
         r.has("online") ? r.get("online").getAsInt() : -1, ents, r.has("admin") && r.get("admin").getAsBoolean(), strings(r, "tickets"), strings(r, "revoked"),
         r.has("ledger") ? strings(r, "ledger") : null
      );
   }

   private static List<String> strings(JsonObject r, String key) {
      List<String> out = new ArrayList<>();
      if (r.has(key) && r.get(key).isJsonArray()) {
         for (JsonElement e : r.getAsJsonArray(key)) {
            if (e.isJsonPrimitive()) {
               out.add(e.getAsString());
            }
         }
      }

      return out;
   }

   private static String ticket(JsonObject r) {
      return r.has("ticket") && r.get("ticket").isJsonPrimitive() ? r.get("ticket").getAsString() : null;
   }

   public String[] redeem(String token, String code, String name) throws IOException {
      JsonObject body = new JsonObject();
      body.addProperty("code", code);
      body.addProperty("name", name);
      JsonObject r = this.post("/v1/redeem", body, token);
      return new String[]{r.get("reward").getAsString(), r.has("name") ? r.get("name").getAsString() : name, ticket(r)};
   }

   public List<String> adminCodes(String token, String reward, int count, int uses) throws IOException {
      JsonObject body = new JsonObject();
      body.addProperty("reward", reward);
      body.addProperty("count", count);
      body.addProperty("uses", uses);
      JsonObject r = this.post("/v1/admin/codes", body, token);
      List<String> out = new ArrayList<>();

      for (JsonElement e : r.getAsJsonArray("codes")) {
         out.add(e.getAsString());
      }

      return out;
   }

   public ApiProtocol.Grant adminGrant(String token, String name, String ent, boolean revoke) throws IOException {
      JsonObject body = new JsonObject();
      body.addProperty("name", name);
      body.addProperty("ent", ent);
      body.addProperty("revoke", revoke);
      JsonObject r = this.post("/v1/admin/grant", body, token);
      return new ApiProtocol.Grant(ticket(r), r.has("name") ? r.get("name").getAsString() : name, strings(r, "ranks"));
   }

   public List<ApiProtocol.Remote> lookup(Collection<UUID> uuids) throws IOException {
      List<ApiProtocol.Remote> out = new ArrayList<>();
      List<UUID> all = new ArrayList<>(uuids);

      for (int i = 0; i < all.size(); i += 200) {
         JsonObject body = new JsonObject();
         JsonArray arr = new JsonArray();

         for (UUID u : all.subList(i, Math.min(all.size(), i + 200))) {
            arr.add(u.toString());
         }

         body.add("uuids", arr);
         JsonObject r = this.post("/v1/lookup", body, null);
         if (r.has("users")) {
            for (JsonElement e : r.getAsJsonArray("users")) {
               JsonObject u = e.getAsJsonObject();
               Map<String, String> cos = new LinkedHashMap<>();
               if (u.has("cosmetics") && u.get("cosmetics").isJsonObject()) {
                  for (Entry<String, JsonElement> c : u.getAsJsonObject("cosmetics").entrySet()) {
                     cos.put(c.getKey(), c.getValue().getAsString());
                  }
               }

               out.add(new ApiProtocol.Remote(UUID.fromString(u.get("uuid").getAsString()), u.has("name") ? u.get("name").getAsString() : "", cos));
            }
         }
      }

      return out;
   }

   public void emote(String token, String id) throws IOException {
      JsonObject body = new JsonObject();
      body.addProperty("id", id == null ? "" : id);
      this.post("/v1/emote", body, token);
   }

   public Map<UUID, Object[]> emotes(Collection<UUID> uuids) throws IOException {
      JsonObject body = new JsonObject();
      JsonArray arr = new JsonArray();
      int n = 0;

      for (UUID u : uuids) {
         if (n++ >= 200) {
            break;
         }

         arr.add(u.toString());
      }

      body.add("uuids", arr);
      JsonObject r = this.post("/v1/emotes", body, null);
      Map<UUID, Object[]> out = new LinkedHashMap<>();
      if (r.has("emotes")) {
         for (JsonElement e : r.getAsJsonArray("emotes")) {
            JsonObject o = e.getAsJsonObject();
            out.put(UUID.fromString(o.get("uuid").getAsString()), new Object[]{o.get("id").getAsString(), o.get("ms").getAsLong()});
         }
      }

      return out;
   }

   public String uploadCape(String token, byte[] png) throws IOException {
      JsonObject body = new JsonObject();
      body.addProperty("png", Base64.getEncoder().encodeToString(png));
      JsonObject r = this.post("/v1/cape", body, token);
      return r.get("id").getAsString();
   }

   public byte[] downloadCape(String hash) throws IOException {
      try {
         HttpResponse<byte[]> r = this.http
            .send(
               HttpRequest.newBuilder(URI.create(this.base + "/v1/cape/" + hash))
                  .timeout(Duration.ofSeconds(10L))
                  .header("User-Agent", this.userAgent)
                  .GET()
                  .build(),
               BodyHandlers.ofByteArray()
            );
         if (r.statusCode() == 404) {
            return null;
         } else if (r.statusCode() != 200) {
            throw new IOException("HTTP " + r.statusCode());
         } else {
            return r.body();
         }
      } catch (InterruptedException var3) {
         Thread.currentThread().interrupt();
         throw new IOException("unterbrochen", var3);
      }
   }

   public int stats() throws IOException {
      JsonObject r = this.get("/v1/stats");
      return r.has("online") ? r.get("online").getAsInt() : -1;
   }

   private JsonObject post(String path, JsonObject body, String token) throws IOException {
      Builder b = HttpRequest.newBuilder(URI.create(this.base + path))
         .header("Content-Type", "application/json")
         .POST(BodyPublishers.ofString(GSON.toJson(body)));
      if (token != null) {
         b.header("Authorization", "Bearer " + token);
      }

      return json(this.send(b));
   }

   private JsonObject get(String path) throws IOException {
      return json(this.send(HttpRequest.newBuilder(URI.create(this.base + path)).GET()));
   }

   private HttpResponse<String> send(Builder b) throws IOException {
      try {
         return this.http.send(b.timeout(Duration.ofSeconds(8L)).header("User-Agent", this.userAgent).build(), BodyHandlers.ofString());
      } catch (InterruptedException var3) {
         Thread.currentThread().interrupt();
         throw new IOException("unterbrochen", var3);
      } catch (IllegalArgumentException var4) {
         throw new IOException("ungültige API-Adresse: " + this.base, var4);
      }
   }

   private static JsonObject json(HttpResponse<String> r) throws IOException {
      if (r.statusCode() == 401) {
         throw new ApiProtocol.AuthException("Sitzung abgelaufen");
      } else if (r.statusCode() / 100 != 2) {
         String msg = r.body() == null ? "" : r.body();

         try {
            JsonElement e = JsonParser.parseString(msg);
            if (e.isJsonObject() && e.getAsJsonObject().has("error")) {
               throw new IOException(e.getAsJsonObject().get("error").getAsString());
            }
         } catch (RuntimeException var3) {
         }

         throw new IOException("HTTP " + r.statusCode() + (msg.isBlank() ? "" : ": " + msg.substring(0, Math.min(120, msg.length()))));
      } else {
         String s = r.body();
         if (s != null && !s.isBlank()) {
            JsonElement e = JsonParser.parseString(s);
            return e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
         } else {
            return new JsonObject();
         }
      }
   }

   public static final class AuthException extends IOException {
      public AuthException(String msg) {
         super(msg);
      }
   }

   public record Beat(int online, Set<String> entitlements, boolean admin, List<String> tickets, List<String> revoked, List<String> ledger) {
   }

   /** Antwort auf Rang geben/nehmen: neues Ticket (oder null), richtiger Name, Ränge danach (owner, admin, legend, vip). */
   public record Grant(String ticket, String name, List<String> ranks) {
   }

   public record Remote(UUID uuid, String name, Map<String, String> cosmetics) {
   }
}
