package com.maneszn.styles.pack;

import com.maneszn.styles.gui.VanillaLookup;
import com.maneszn.styles.image.ImageProcessor;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

final class IconRecolor {
   private IconRecolor() {
   }

   static BufferedImage make(Path styleImage, String vanillaPath) {
      try {
         ImageProcessor.Img icon = VanillaLookup.INSTANCE.texture("minecraft", vanillaPath);
         BufferedImage src = ImageIO.read(styleImage.toFile());
         if (icon != null && src != null) {
            List<Integer> cols = new ArrayList<>();

            for (int y = 0; y < src.getHeight(); y++) {
               for (int x = 0; x < src.getWidth(); x++) {
                  int c = src.getRGB(x, y);
                  if (c >>> 24 > 200) {
                     cols.add(c);
                  }
               }
            }

            if (cols.size() < 8) {
               return null;
            } else {
               int[] pal = cols.stream().mapToInt(Integer::intValue).toArray();
               Integer[] boxed = Arrays.stream(pal).boxed().toArray(Integer[]::new);
               Arrays.sort(boxed, (ax, bx) -> Double.compare(lum(ax), lum(bx)));
               List<Double> ls = new ArrayList<>();

               for (int c : icon.px()) {
                  if (c >>> 24 > 0) {
                     ls.add(lum(c));
                  }
               }

               double[] sorted = ls.stream().mapToDouble(Double::doubleValue).sorted().toArray();
               if (sorted.length == 0) {
                  return null;
               } else {
                  BufferedImage out = new BufferedImage(icon.w(), icon.h(), 2);
                  int win = Math.max(1, boxed.length / 40);

                  for (int y = 0; y < icon.h(); y++) {
                     for (int xx = 0; xx < icon.w(); xx++) {
                        int cx = icon.px()[y * icon.w() + xx];
                        int a = cx >>> 24;
                        if (a != 0) {
                           double q = rank(sorted, lum(cx));
                           int idx = (int)Math.round(q * (boxed.length - 1));
                           int lo = Math.max(0, idx - win);
                           int hi = Math.min(boxed.length - 1, idx + win);
                           long r = 0L;
                           long g = 0L;
                           long b = 0L;

                           for (int i = lo; i <= hi; i++) {
                              int p = boxed[i];
                              r += p >> 16 & 0xFF;
                              g += p >> 8 & 0xFF;
                              b += p & 0xFF;
                           }

                           int n = hi - lo + 1;
                           out.setRGB(xx, y, a << 24 | (int)(r / n) << 16 | (int)(g / n) << 8 | (int)(b / n));
                        }
                     }
                  }

                  return out;
               }
            }
         } else {
            return null;
         }
      } catch (Throwable var28) {
         return null;
      }
   }

   private static double rank(double[] sorted, double v) {
      int lo = 0;
      int hi = sorted.length;

      while (lo < hi) {
         int m = lo + hi >>> 1;
         if (sorted[m] < v) {
            lo = m + 1;
         } else {
            hi = m;
         }
      }

      int lo2 = lo;

      while (lo2 < sorted.length && sorted[lo2] <= v) {
         lo2++;
      }

      double mid = (lo + lo2) / 2.0;
      return sorted.length == 1 ? 0.5 : Math.max(0.0, Math.min(1.0, mid / (sorted.length - 1)));
   }

   private static double lum(int c) {
      return 0.299 * (c >> 16 & 0xFF) + 0.587 * (c >> 8 & 0xFF) + 0.114 * (c & 0xFF);
   }
}
