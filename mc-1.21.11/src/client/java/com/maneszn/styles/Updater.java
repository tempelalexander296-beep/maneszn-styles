package com.maneszn.styles;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;

public final class Updater {
   private static volatile Updater.Status status = Updater.Status.IDLE;
   private static volatile String readyVersion;
   private static volatile boolean swapScheduled;

   private Updater() {
   }

   public static Updater.Status status() {
      return status;
   }

   public static String readyVersion() {
      return readyVersion;
   }

   public static boolean enabled() {
      Path cfg = FabricLoader.getInstance().getConfigDir().resolve("maneszn_styles_updater.json");

      try {
         if (Files.isRegularFile(cfg)) {
            try (Reader r = Files.newBufferedReader(cfg, StandardCharsets.UTF_8)) {
               JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
               if (o.has("autoUpdate")) {
                  return o.get("autoUpdate").getAsBoolean();
               }

               return true;
            }
         } else {
            Files.createDirectories(cfg.getParent());
            Files.writeString(cfg, "{\n  \"autoUpdate\": true\n}\n", StandardCharsets.UTF_8);
         }
      } catch (Exception var6) {
         ManesznStyles.LOGGER.warn("Maneszn Styles: Updater-Einstellung nicht lesbar ({})", var6.toString());
      }

      return true;
   }

   public static void setEnabled(boolean on) {
      Path cfg = FabricLoader.getInstance().getConfigDir().resolve("maneszn_styles_updater.json");

      try {
         Files.createDirectories(cfg.getParent());
         Files.writeString(cfg, "{\n  \"autoUpdate\": " + on + "\n}\n", StandardCharsets.UTF_8);
         if (!on && status != Updater.Status.READY) {
            status = Updater.Status.OFF;
         }
      } catch (IOException var3) {
         ManesznStyles.LOGGER.warn("Maneszn Styles: Updater-Einstellung nicht speicherbar", var3);
      }
   }

   public static void register() {
      if (!enabled()) {
         status = Updater.Status.OFF;
      } else {
         Path self = ownJar();
         if (self == null) {
            ManesznStyles.LOGGER.info("Maneszn Styles: Auto-Update aus (keine Jar – Entwicklungsumgebung?)");
            status = Updater.Status.OFF;
         } else {
            Thread t = new Thread(() -> run(self), "Maneszn-Updater");
            t.setDaemon(true);
            t.start();
         }
      }
   }

   private static Path ownJar() {
      Optional<ModContainer> mod = FabricLoader.getInstance().getModContainer("maneszn_styles");
      if (mod.isEmpty()) {
         return null;
      } else {
         for (Path p : mod.get().getOrigin().getPaths()) {
            if (Files.isRegularFile(p) && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
               return p.toAbsolutePath();
            }
         }

         return null;
      }
   }

   private static void run(Path self) {
      try {
         HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20L)).followRedirects(Redirect.ALWAYS).build();
         HttpResponse<String> vr = http.send(
            HttpRequest.newBuilder(URI.create(Website.url() + "/v1/version"))
               .timeout(Duration.ofSeconds(60L))
               .header("User-Agent", "ManesznStyles/" + Website.currentVersion())
               .GET()
               .build(),
            BodyHandlers.ofString()
         );
         if (vr.statusCode() != 200) {
            return;
         }

         JsonObject v = JsonParser.parseString(vr.body()).getAsJsonObject();
         String latest = v.has("version") ? v.get("version").getAsString() : null;
         int build = v.has("build") ? v.get("build").getAsInt() : 0;
         String mc = v.has("minecraft") ? v.get("minecraft").getAsString() : "";
         if (latest == null || Website.compare(latest, Website.currentVersion()) <= 0) {
            return;
         }

         String myMc = FabricLoader.getInstance().getModContainer("minecraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
         if (!mc.isEmpty() && !mc.equals(myMc)) {
            ManesznStyles.LOGGER.info("Maneszn Styles: Update {} ist für Minecraft {} – kein Auto-Update", latest, mc);
            return;
         }

         Path dir = FabricLoader.getInstance().getGameDir().resolve("maneszn-update");
         Files.createDirectories(dir);
         String name = "maneszn-styles-v" + (build > 0 ? build : latest) + ".jar";
         Path staged = dir.resolve(name);
         if (!Files.isRegularFile(staged) || !valid(staged, latest)) {
            status = Updater.Status.DOWNLOADING;

            try (Stream<Path> old = Files.list(dir)) {
               old.filter(p -> !p.equals(staged)).forEach(p -> {
                  try {
                     Files.deleteIfExists(p);
                  } catch (IOException var2x) {
                  }
               });
            }

            Path part = dir.resolve(name + ".part");
            ManesznStyles.LOGGER.info("Maneszn Styles: lade Update {} herunter …", latest);
            HttpResponse<Path> dr = http.send(
               HttpRequest.newBuilder(URI.create(Website.downloadUrl()))
                  .timeout(Duration.ofMinutes(15L))
                  .header("User-Agent", "ManesznStyles/" + Website.currentVersion())
                  .GET()
                  .build(),
               BodyHandlers.ofFile(part)
            );
            if (dr.statusCode() != 200 || !valid(part, latest)) {
               Files.deleteIfExists(part);
               status = Updater.Status.FAILED;
               ManesznStyles.LOGGER.warn("Maneszn Styles: Update-Download ungültig (HTTP {})", dr.statusCode());
               return;
            }

            Files.move(part, staged, StandardCopyOption.REPLACE_EXISTING);
         }

         scheduleSwap(self, staged);
         readyVersion = latest;
         status = Updater.Status.READY;
         ManesznStyles.LOGGER.info("Maneszn Styles: Update {} bereit – wird nach dem Beenden installiert", latest);
         Minecraft.getInstance().execute(Website::maybeNotify);
      } catch (Throwable var16) {
         status = Updater.Status.FAILED;
         ManesznStyles.LOGGER.warn("Maneszn Styles: Auto-Update fehlgeschlagen ({})", var16.toString());
      }
   }

   private static boolean valid(Path jar, String expectedVersion) {
      try {
         boolean var22;
         try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry meta = zip.getEntry("fabric.mod.json");
            if (meta == null) {
               return false;
            }

            Reader r = new InputStreamReader(zip.getInputStream(meta), StandardCharsets.UTF_8);

            JsonObject o;
            try {
               o = JsonParser.parseReader(r).getAsJsonObject();
            } catch (Throwable var14) {
               try {
                  r.close();
               } catch (Throwable var13) {
                  var14.addSuppressed(var13);
               }

               throw var14;
            }

            r.close();
            if (!"maneszn_styles".equals(o.has("id") ? o.get("id").getAsString() : "")) {
               return false;
            }

            if (!expectedVersion.equals(o.has("version") ? o.get("version").getAsString() : "")) {
               return false;
            }

            byte[] buf = new byte[65536];
            Enumeration<? extends ZipEntry> en = zip.entries();

            while (en.hasMoreElements()) {
               ZipEntry e = en.nextElement();
               if (!e.isDirectory()) {
                  try (InputStream in = zip.getInputStream(e)) {
                     while (in.read(buf) >= 0) {
                     }
                  }
               }
            }

            var22 = true;
         }

         return var22;
      } catch (Exception var17) {
         return false;
      }
   }

   private static void scheduleSwap(Path self, Path staged) throws IOException {
      if (!swapScheduled) {
         Path dest = self.getParent().resolve(staged.getFileName().toString());
         boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
         if (!windows) {
            Files.move(staged, dest, StandardCopyOption.REPLACE_EXISTING);
            if (!dest.equals(self)) {
               Files.deleteIfExists(self);
            }

            swapScheduled = true;
         } else {
            long pid = ProcessHandle.current().pid();
            String script = String.join(
               "\n",
               "$ErrorActionPreference = 'SilentlyContinue'",
               "Wait-Process -Id " + pid,
               "Start-Sleep -Seconds 2",
               "$old = " + ps(self.toString()),
               "$new = " + ps(staged.toString()),
               "$dest = " + ps(dest.toString()),
               "for ($i = 0; $i -lt 60; $i++) {",
               "  if (-not (Test-Path -LiteralPath $old)) { break }",
               "  Remove-Item -LiteralPath $old -Force",
               "  if (Test-Path -LiteralPath $old) { Start-Sleep -Seconds 1 }",
               "}",
               "if (-not (Test-Path -LiteralPath $old) -or ($old -eq $dest)) {",
               "  Move-Item -LiteralPath $new -Destination $dest -Force",
               "}"
            );
            String encoded = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
            new ProcessBuilder(
                  "powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-EncodedCommand", encoded
               )
               .redirectErrorStream(true)
               .redirectOutput(java.lang.ProcessBuilder.Redirect.DISCARD)
               .start();
            swapScheduled = true;
         }
      }
   }

   private static String ps(String s) {
      return "'" + s.replace("'", "''") + "'";
   }

   public static enum Status {
      IDLE,
      DOWNLOADING,
      READY,
      FAILED,
      OFF;
   }
}
