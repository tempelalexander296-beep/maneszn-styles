package com.maneszn.styles.cosmetic;

import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.ModPaths;
import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.image.ImageProcessor;
import com.maneszn.styles.net.ApiClient;
import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;

public final class CustomCapes {
   public static final String PREFIX = "eigen_";
   private static final int W = 256;
   private static final int H = 128;
   private static final int MAX_FRAMES = 16;
   private static final long MAX_BYTES = 1500000L;
   private static final Map<String, Cosmetic> LOADED = new LinkedHashMap<>();
   private static final Map<String, ImageProcessor.Img> PREVIEW = new ConcurrentHashMap<>();
   private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();
   private static final Map<String, Long> FAILED = new ConcurrentHashMap<>();
   private static boolean scanned;

   private CustomCapes() {
   }

   public static Path dir() {
      return ModPaths.root().resolve("capes");
   }

   public static boolean isCustom(String id) {
      return id != null
         && id.startsWith("eigen_")
         && id.length() == "eigen_".length() + 12
         && id.substring("eigen_".length()).chars().allMatch(c -> c >= 48 && c <= 57 || c >= 97 && c <= 102);
   }

   public static synchronized List<Cosmetic> own() {
      List<Cosmetic> out = new ArrayList<>();

      for (Cosmetic c : LOADED.values()) {
         if (Files.exists(dir().resolve(hash(c.id()) + ".png"))) {
            out.add(c);
         }
      }

      return out;
   }

   public static synchronized Cosmetic get(String id) {
      return LOADED.get(id);
   }

   public static void tick(Minecraft mc) {
      if (!scanned && mc.getTextureManager() != null) {
         scanned = true;

         try {
            Files.createDirectories(dir());

            try (Stream<Path> files = Files.list(dir())) {
               for (Path p : files.toList()) {
                  String n = p.getFileName().toString();
                  if (n.endsWith(".png") && n.length() == 16) {
                     try {
                        register(n.substring(0, 12), Files.readAllBytes(p));
                     } catch (Exception var7) {
                        ManesznStyles.LOGGER.warn("Umhang {} kaputt: {}", n, var7.toString());
                     }
                  }
               }
            }
         } catch (Exception var9) {
            ManesznStyles.LOGGER.warn("Umhang-Ordner nicht lesbar: {}", var9.toString());
         }
      }
   }

   public static void request(String id) {
      if (isCustom(id) && ModConfig.get().showCustomCapes) {
         synchronized (CustomCapes.class) {
            if (LOADED.containsKey(id)) {
               return;
            }
         }

         Long f = FAILED.get(id);
         if (f == null || System.currentTimeMillis() - f >= 600000L) {
            if (PENDING.add(id)) {
               String h = hash(id);
               ApiClient.get().downloadCape(h, bytes -> {
                  if (bytes != null && bytes.length <= 1500000L) {
                     Minecraft.getInstance().execute(() -> {
                        try {
                           if (!h.equals(sha12(bytes))) {
                              throw new IllegalStateException("Prüfsumme passt nicht");
                           }

                           register(h, bytes);
                        } catch (Exception var7) {
                           ManesznStyles.LOGGER.warn("Umhang {} ungültig: {}", id, var7.toString());
                           FAILED.put(id, System.currentTimeMillis());
                        } finally {
                           PENDING.remove(id);
                        }
                     });
                  } else {
                     FAILED.put(id, System.currentTimeMillis());
                     PENDING.remove(id);
                  }
               });
            }
         }
      }
   }

   public static CustomCapes.Result importFile(Path file) {
      try {
         if (Files.size(file) > 20000000L) {
            return new CustomCapes.Result(null, "too_big");
         } else {
            List<BufferedImage> frames = readFrames(file);
            if (frames.isEmpty()) {
               return new CustomCapes.Result(null, "no_image");
            } else {
               BufferedImage strip = new BufferedImage(256, 128 * frames.size(), 2);
               Graphics2D g = strip.createGraphics();

               for (int i = 0; i < frames.size(); i++) {
                  g.drawImage(toCape(frames.get(i)), 0, i * 128, null);
               }

               g.dispose();
               ByteArrayOutputStream bos = new ByteArrayOutputStream();
               ImageIO.write(strip, "png", bos);
               byte[] png = bos.toByteArray();
               if (png.length > 1500000L) {
                  return new CustomCapes.Result(null, "too_big");
               } else {
                  String h = sha12(png);
                  Files.createDirectories(dir());
                  Files.write(dir().resolve(h + ".png"), png);
                  register(h, png);
                  ApiClient.get().uploadCape(png, ok -> {});
                  return new CustomCapes.Result("eigen_" + h, null);
               }
            }
         }
      } catch (Exception var7) {
         ManesznStyles.LOGGER.warn("Umhang-Import fehlgeschlagen: {}", var7.toString());
         return new CustomCapes.Result(null, "error");
      }
   }

   public static synchronized void delete(String id) {
      try {
         Files.deleteIfExists(dir().resolve(hash(id) + ".png"));
      } catch (Exception var2) {
      }

      LOADED.remove(id);
      CosmeticRegistry.get().removeCustom(id);
   }

   private static List<BufferedImage> readFrames(Path file) throws Exception {
      List<BufferedImage> out = new ArrayList<>();
      String n = file.getFileName().toString().toLowerCase(Locale.ROOT);
      if (n.endsWith(".gif")) {
         try (ImageInputStream in = ImageIO.createImageInputStream(file.toFile())) {
            Iterator<ImageReader> it = ImageIO.getImageReaders(in);
            if (it.hasNext()) {
               ImageReader r = it.next();
               r.setInput(in);
               int count = Math.min(r.getNumImages(true), 64);
               int step = Math.max(1, (int)Math.ceil(count / 16.0));
               BufferedImage canvas = null;

               for (int i = 0; i < count; i++) {
                  BufferedImage f = r.read(i);
                  if (canvas == null) {
                     canvas = new BufferedImage(r.getWidth(0), r.getHeight(0), 2);
                  }

                  Graphics2D g = canvas.createGraphics();
                  g.drawImage(f, 0, 0, null);
                  g.dispose();
                  if (i % step == 0) {
                     BufferedImage copy = new BufferedImage(canvas.getWidth(), canvas.getHeight(), 2);
                     Graphics2D cg = copy.createGraphics();
                     cg.drawImage(canvas, 0, 0, null);
                     cg.dispose();
                     out.add(copy);
                  }
               }

               r.dispose();
            }
         }
      }

      if (out.isEmpty()) {
         BufferedImage b = ImageIO.read(file.toFile());
         if (b != null) {
            out.add(b);
         }
      }

      while (out.size() > 16) {
         out.remove(out.size() - 1);
      }

      return out;
   }

   static BufferedImage toCape(BufferedImage src) {
      BufferedImage out = new BufferedImage(256, 128, 2);
      Graphics2D g = out.createGraphics();
      double ratio = (double)src.getWidth() / src.getHeight();
      boolean pixelArt = src.getWidth() <= 128;
      g.setRenderingHint(
         RenderingHints.KEY_INTERPOLATION, pixelArt ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : RenderingHints.VALUE_INTERPOLATION_BILINEAR
      );
      if (Math.abs(ratio - 2.0) < 0.05 && src.getWidth() >= 64) {
         g.drawImage(src, 0, 0, 256, 128, null);
         g.dispose();
         return out;
      } else {
         int avg = average(src);
         int dark = mix(avg, -16777216, 0.55);
         g.setColor(new Color(dark, true));
         g.fillRect(0, 0, 256, 128);
         int fx = 4;
         int fy = 4;
         int fw = 40;
         int fh = 64;
         drawCover(g, src, fx, fy, fw, fh);
         drawCover(g, src, 48, 4, 40, 64);
         g.setColor(new Color(0, 0, 0, 120));
         g.fillRect(48, 4, 40, 64);
         g.dispose();
         return out;
      }
   }

   private static void drawCover(Graphics2D g, BufferedImage src, int x, int y, int w, int h) {
      double s = Math.max((double)w / src.getWidth(), (double)h / src.getHeight());
      int sw = (int)Math.round(w / s);
      int sh = (int)Math.round(h / s);
      int sx = (src.getWidth() - sw) / 2;
      int sy = (src.getHeight() - sh) / 2;
      g.drawImage(src, x, y, x + w, y + h, sx, sy, sx + sw, sy + sh, null);
   }

   private static int average(BufferedImage b) {
      long r = 0L;
      long gg = 0L;
      long bl = 0L;
      long n = 0L;
      int y = 0;

      while (y < b.getHeight()) {
         for (int x = 0; x < b.getWidth(); x += Math.max(1, b.getWidth() / 32)) {
            int c = b.getRGB(x, y);
            if (c >>> 24 >= 40) {
               r += c >> 16 & 0xFF;
               gg += c >> 8 & 0xFF;
               bl += c & 0xFF;
               n++;
            }
         }

         y += Math.max(1, b.getHeight() / 32);
      }

      return n == 0L ? -13623232 : 0xFF000000 | (int)(r / n) << 16 | (int)(gg / n) << 8 | (int)(bl / n);
   }

   private static int mix(int a, int b, double f) {
      int r = (int)Math.round((a >> 16 & 0xFF) * (1.0 - f) + (b >> 16 & 0xFF) * f);
      int g = (int)Math.round((a >> 8 & 0xFF) * (1.0 - f) + (b >> 8 & 0xFF) * f);
      int bl = (int)Math.round((a & 0xFF) * (1.0 - f) + (b & 0xFF) * f);
      return 0xFF000000 | r << 16 | g << 8 | bl;
   }

   private static synchronized void register(String h, byte[] png) throws Exception {
      String id = "eigen_" + h;
      if (!LOADED.containsKey(id)) {
         BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
         if (img == null) {
            throw new IllegalStateException("kein Bild");
         } else {
            int w = img.getWidth();
            int fh = w / 2;
            if (w >= 64 && w <= 512 && w % 64 == 0 && img.getHeight() % fh == 0) {
               int frames = img.getHeight() / fh;
               if (frames >= 1 && frames <= 16) {
                  String base = "dyn/cape/" + h;
                  TextureManager tm = Minecraft.getInstance().getTextureManager();

                  for (int i = 0; i < frames; i++) {
                     BufferedImage frame = img.getSubimage(0, i * fh, w, fh);
                     ByteArrayOutputStream bos = new ByteArrayOutputStream();
                     ImageIO.write(frame, "png", bos);
                     NativeImage ni = NativeImage.read(new ByteArrayInputStream(bos.toByteArray()));
                     String path = frames > 1 ? base + "_" + i + ".png" : base + ".png";
                     tm.register(CosmeticManager.texture(path), new DynamicTexture(() -> "maneszn_cape " + h, ni));
                     PREVIEW.put(path, ImageProcessor.Img.of(frame));
                  }

                  int n = LOADED.size() + 1;
                  Cosmetic c = new Cosmetic(
                     id,
                     Cosmetic.Type.CAPE,
                     Map.of("de", "Eigener Umhang " + n, "en", "Custom cape " + n),
                     base,
                     frames,
                     2,
                     new int[]{64, 32},
                     false,
                     false,
                     List.of(),
                     null,
                     "around",
                     4,
                     null,
                     List.of(),
                     false,
                     "none",
                     "none",
                     null,
                     null
                  );
                  LOADED.put(id, c);
                  CosmeticRegistry.get().addCustom(c);
               } else {
                  throw new IllegalStateException("zu viele Bilder");
               }
            } else {
               throw new IllegalStateException("falsches Format");
            }
         }
      }
   }

   public static ImageProcessor.Img preview(String path) {
      return PREVIEW.get(path);
   }

   private static String hash(String id) {
      return id.substring("eigen_".length());
   }

   static String sha12(byte[] data) throws Exception {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)).substring(0, 12);
   }

   public record Result(String id, String error) {
   }
}
