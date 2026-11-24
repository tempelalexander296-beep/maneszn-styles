package com.maneszn.styles.cosmetic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.image.ImageProcessor;
import com.maneszn.styles.render.MiniPlayer;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class SkinFetcher {
   private static final Map<String, SkinFetcher.State> CACHE = new ConcurrentHashMap<>();
   private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "Maneszn-Skins");
      t.setDaemon(true);
      return t;
   });
   private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6L)).followRedirects(Redirect.NORMAL).build();
   private static int counter;

   private SkinFetcher() {
   }

   public static boolean validName(String name) {
      return name != null && name.matches("[A-Za-z0-9_]{1,16}");
   }

   public static SkinFetcher.Skin get(String name) {
      if (!validName(name)) {
         return null;
      } else {
         String key = name.toLowerCase(Locale.ROOT);
         SkinFetcher.State st = CACHE.get(key);
         if (st != null) {
            if (st.skin() != null || st.loading()) {
               return st.skin();
            }

            if (System.currentTimeMillis() - st.failedAt() < 300000L) {
               return null;
            }
         }

         CACHE.put(key, new SkinFetcher.State(null, 0L, true));
         EXEC.submit(() -> load(key));
         return null;
      }
   }

   public static boolean loading(String name) {
      SkinFetcher.State st = name == null ? null : CACHE.get(name.toLowerCase(Locale.ROOT));
      return st != null && st.loading();
   }

   public static boolean failed(String name) {
      SkinFetcher.State st = name == null ? null : CACHE.get(name.toLowerCase(Locale.ROOT));
      return st != null && !st.loading() && st.skin() == null;
   }

   private static void load(String key) {
      try {
         JsonObject who = json("https://api.mojang.com/users/profiles/minecraft/" + key);
         String id = who.get("id").getAsString();
         JsonObject profile = json("https://sessionserver.mojang.com/session/minecraft/profile/" + id);
         String value = profile.getAsJsonArray("properties").get(0).getAsJsonObject().get("value").getAsString();
         JsonObject tex = JsonParser.parseString(new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8))
            .getAsJsonObject()
            .getAsJsonObject("textures");
         if (tex == null || !tex.has("SKIN")) {
            throw new IllegalStateException("kein Skin");
         }

         JsonObject skin = tex.getAsJsonObject("SKIN");
         boolean slim = skin.has("metadata") && "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString());
         String url = skin.get("url").getAsString().replace("http://", "https://");
         HttpResponse<byte[]> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10L)).build(), BodyHandlers.ofByteArray());
         if (r.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + r.statusCode());
         }

         ImageProcessor.Img img = MiniPlayer.upgrade(ImageProcessor.Img.of(ImageIO.read(new ByteArrayInputStream(r.body()))));
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         ImageIO.write(img.toBuffered(), "png", bos);
         byte[] png = bos.toByteArray();
         Minecraft.getInstance().execute(() -> {
            try {
               NativeImage ni = NativeImage.read(new ByteArrayInputStream(png));
               Identifier tid = ManesznStyles.id("skin/s" + counter++);
               Minecraft.getInstance().getTextureManager().register(tid, new DynamicTexture(() -> "maneszn_skin " + key, ni));
               CACHE.put(key, new SkinFetcher.State(new SkinFetcher.Skin(tid, img, slim), 0L, false));
            } catch (Exception var6x) {
               fail(key, var6x);
            }
         });
      } catch (Exception var13) {
         fail(key, var13);
      }
   }

   private static void fail(String key, Exception e) {
      ManesznStyles.LOGGER.warn("Skin von {} nicht ladbar: {}", key, e.toString());
      CACHE.put(key, new SkinFetcher.State(null, System.currentTimeMillis(), false));
   }

   private static JsonObject json(String url) throws Exception {
      HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8L)).build(), BodyHandlers.ofString());
      if (r.statusCode() != 200) {
         throw new IllegalStateException("HTTP " + r.statusCode() + " für " + url);
      } else {
         return JsonParser.parseString(r.body()).getAsJsonObject();
      }
   }

   public record Skin(Identifier texture, ImageProcessor.Img image, boolean slim) {
   }

   private record State(SkinFetcher.Skin skin, long failedAt, boolean loading) {
   }
}
