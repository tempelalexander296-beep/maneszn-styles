package com.maneszn.styles.pack;

import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.ModPaths;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.style.Category;
import com.maneszn.styles.style.Style;
import com.maneszn.styles.style.StyleRegistry;
import com.maneszn.styles.style.Target;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;

public final class StylePackBuilder {
   public static final int PACK_FORMAT = 75;
   private static volatile boolean hasContent;
   private static CompletableFuture<Void> pendingReload;
   private static volatile boolean reloadQueued;

   private StylePackBuilder() {
   }

   public static synchronized int rebuild() {
      Path dir = ModPaths.generatedPackDir();
      int files = 0;

      try {
         deleteRecursively(dir);
         Files.createDirectories(dir);
         files = writeActiveStyles(dir);
         String sounds = SoundChoices.soundsJson();
         if (sounds != null) {
            Path sj = dir.resolve("assets/minecraft/sounds.json");
            Files.createDirectories(sj.getParent());
            Files.writeString(sj, sounds, StandardCharsets.UTF_8);
            files++;
         }

         Files.writeString(dir.resolve("pack.mcmeta"), packMcmeta("Maneszn Styles (aktive Auswahl)"), StandardCharsets.UTF_8);
      } catch (IOException var4) {
         ManesznStyles.LOGGER.error("Virtuelles Pack konnte nicht gebaut werden", var4);
      }

      hasContent = files > 0;
      return files;
   }

   public static int writeActiveStyles(Path out) throws IOException {
      return writeStyles(out, ModConfig.get().active);
   }

   static void swapElytraWings(Path png) {
      try {
         BufferedImage img = ImageIO.read(png.toFile());
         if (img == null || img.getWidth() != img.getHeight() * 2) {
            return;
         }

         int s = img.getWidth() / 64;
         int[][] faces = new int[][]{{24, 34}, {36, 46}};

         for (int[] f : faces) {
            for (int y = 2 * s; y < 22 * s; y++) {
               for (int i = 0; i < (f[1] - f[0]) * s / 2; i++) {
                  int a = f[0] * s + i;
                  int b = f[1] * s - 1 - i;
                  int ca = img.getRGB(a, y);
                  int cb = img.getRGB(b, y);
                  img.setRGB(a, y, cb);
                  img.setRGB(b, y, ca);
               }
            }
         }

         for (int y = 2 * s; y < 22 * s; y++) {
            for (int i = 0; i < 2 * s; i++) {
               int a = 22 * s + i;
               int b = 34 * s + i;
               int ca = img.getRGB(a, y);
               int cb = img.getRGB(b, y);
               img.setRGB(a, y, cb);
               img.setRGB(b, y, ca);
            }
         }

         ImageIO.write(img, "png", png.toFile());
      } catch (Exception var14) {
      }
   }

   static boolean wrongShape(Target t, Path png) {
      if (t.width() > 0 && t.height() > 0) {
         try {
            boolean w;
            try (InputStream in = Files.newInputStream(png)) {
               byte[] h = in.readNBytes(24);
               if (h.length >= 24 && (h[1] & 255) == 80 && (h[2] & 255) == 78 && (h[3] & 255) == 71) {
                  long wx = (h[16] & 255L) << 24 | (h[17] & 255L) << 16 | (h[18] & 255L) << 8 | h[19] & 255L;
                  long ht = (h[20] & 255L) << 24 | (h[21] & 255L) << 16 | (h[22] & 255L) << 8 | h[23] & 255L;
                  if (wx > 0L && ht > 0L) {
                     if (t.width() != t.height()) {
                        return wx * t.height() != ht * t.width();
                     }

                     return wx != ht && (ht <= wx || ht % wx != 0L);
                  }

                  return false;
               }

               w = false;
            }

            return w;
         } catch (IOException var11) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static int writeStyles(Path out, Map<String, String> selection) throws IOException {
      int count = 0;
      StyleRegistry reg = StyleRegistry.get();

      for (Entry<String, String> e : selection.entrySet()) {
         Category cat = reg.category(e.getKey());
         if (cat != null && !"vanilla".equals(e.getValue())) {
            Style style = reg.style(cat.id(), e.getValue());
            if (style != null) {
               for (Target t : cat.targets()) {
                  Optional<Path> src = style.resolve(t);
                  if (!src.isEmpty()) {
                     if (wrongShape(t, src.get())) {
                        if (t.width() == t.height() && t.packPath().contains("/textures/item/")) {
                           String vp = t.packPath().substring(t.packPath().indexOf("textures/item/"));
                           BufferedImage icon = IconRecolor.make(src.get(), vp);
                           if (icon != null) {
                              Path dst = out.resolve(t.packPath());
                              Files.createDirectories(dst.getParent());
                              ImageIO.write(icon, "png", dst.toFile());
                              count++;
                           }
                        }
                     } else {
                        Path dst = out.resolve(t.packPath());
                        Files.createDirectories(dst.getParent());
                        Files.copy(src.get(), dst, StandardCopyOption.REPLACE_EXISTING);
                        if (ModConfig.get().elytraSwap && t.packPath().endsWith("wings/elytra.png")) {
                           swapElytraWings(dst);
                        }

                        count++;
                        Path meta = src.get().resolveSibling(src.get().getFileName().toString() + ".mcmeta");
                        if (Files.isRegularFile(meta)) {
                           Files.copy(meta, out.resolve(t.packPath() + ".mcmeta"), StandardCopyOption.REPLACE_EXISTING);
                        }
                     }
                  }
               }

               boolean anyTarget = false;

               for (Target tx : cat.targets()) {
                  if (style.resolve(tx).isPresent()) {
                     anyTarget = true;
                     break;
                  }
               }

               if (style.source() == Style.Source.PACK && cat.extras() != null) {
                  for (String folder : cat.extras().includeFolders()) {
                     int i = folder.indexOf(58);
                     String rel = "assets/" + folder.substring(0, Math.max(0, i)) + "/" + folder.substring(i + 1);
                     Path src = style.root().resolve(rel);
                     if (Files.isDirectory(src)) {
                        count += copyTree(src, out.resolve(rel));
                     }
                  }
               }

               Optional<Path> assets = style.assetsOverride();
               if (assets.isPresent()) {
                  count += copyTree(assets.get(), out.resolve("assets"));
               }

               if (anyTarget && cat.extras() != null) {
                  for (Entry<String, String> ef : cat.extras().extraFiles().entrySet()) {
                     Path dstx = out.resolve(ef.getKey());
                     Files.createDirectories(dstx.getParent());
                     Files.writeString(dstx, ef.getValue(), StandardCharsets.UTF_8);
                     count++;
                  }
               }
            }
         }
      }

      return count;
   }

   private static int copyTree(Path from, Path to) throws IOException {
      int[] n = new int[]{0};

      try (Stream<Path> walk = Files.walk(from)) {
         for (Path p : walk.toList()) {
            if (Files.isRegularFile(p)) {
               String rel = from.relativize(p).toString().replace('\\', '/');
               Path dst = to.resolve(rel);
               Files.createDirectories(dst.getParent());
               Files.copy(p, dst, StandardCopyOption.REPLACE_EXISTING);
               n[0]++;
            }
         }
      }

      return n[0];
   }

   public static String packMcmeta(String description) {
      return "{\n  \"pack\": {\n    \"description\": \""
         + description.replace("\"", "'")
         + "\",\n    \"pack_format\": 75,\n    \"min_format\": [75, 0],\n    \"max_format\": [75, 0]\n  }\n}\n";
   }

   public static PackResources createPackResources() {
      if (!hasContent) {
         return null;
      } else {
         Path dir = ModPaths.generatedPackDir();
         if (!Files.isDirectory(dir.resolve("assets"))) {
            return null;
         } else {
            PackLocationInfo info = new PackLocationInfo("maneszn_styles_virtual", Component.literal("Maneszn Styles"), PackSource.BUILT_IN, Optional.empty());
            return new PathPackResources(info, dir);
         }
      }
   }

   public static void applyAndReload() {
      Minecraft mc = Minecraft.getInstance();
      if (pendingReload != null && !pendingReload.isDone()) {
         reloadQueued = true;
      } else {
         rebuild();
         pendingReload = mc.reloadResourcePacks();
         pendingReload.whenComplete((v, err) -> mc.execute(() -> {
            if (reloadQueued) {
               reloadQueued = false;
               applyAndReload();
            }
         }));
      }
   }

   public static boolean isReloading() {
      return pendingReload != null && !pendingReload.isDone();
   }

   public static boolean hasContent() {
      return hasContent;
   }

   static void deleteRecursively(Path dir) throws IOException {
      if (Files.exists(dir)) {
         try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
               Files.deleteIfExists(p);
            }
         }
      }
   }
}
