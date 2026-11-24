package com.maneszn.styles.image;

import com.maneszn.styles.style.Category;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class ImageProcessor {
   private ImageProcessor() {
   }

   public static ImageProcessor.Img process(BufferedImage input, Category.Kind kind, int targetW, int targetH, int forcedRes) {
      ImageProcessor.Img img = ImageProcessor.Img.of(input);
      if (kind == Category.Kind.SKY) {
         return panoramaToSkybox(img, forcedRes > 0 ? forcedRes * 8 : 512);
      } else {
         int maxDim = Math.max(img.w(), img.h());
         if (maxDim > 512) {
            int k = (int)Math.ceil(maxDim / 512.0);
            img = resizeArea(img, Math.max(1, img.w() / k), Math.max(1, img.h() / k));
         }

         switch (kind) {
            case ITEM:
            case GUI:
               img = removeBackground(img);
               img = cropToContent(img);
               img = padToAspect(img, targetW, targetH);
               break;
            case BLOCK:
               img = centerCropToAspect(img, targetW, targetH);
            case ENTITY:
            case LAYOUT:
            case SKY:
         }

         int block = detectBlockSize(img);
         if (block > 1) {
            img = shrinkBlocks(img, block);
         }

         boolean pixelArt = countColors(img, 257) <= 256 && block >= 1 && !looksLikePhoto(img);
         int[] size = chooseSize(img, kind, targetW, targetH, forcedRes, pixelArt);
         int w = size[0];
         int h = size[1];
         if (img.w() == w && img.h() == h) {
            if (!pixelArt) {
               img = quantize(img, w <= 16 ? 16 : 32);
            }
         } else if (!pixelArt && img.w() >= w) {
            img = resizeArea(img, w, h);
            img = quantize(img, w <= 16 ? 16 : (w <= 32 ? 24 : 32));
         } else {
            img = resizeNearest(img, w, h);
         }

         if (kind == Category.Kind.ITEM || kind == Category.Kind.GUI) {
            img = thresholdAlpha(img);
         }

         return img;
      }
   }

   private static int[] chooseSize(ImageProcessor.Img img, Category.Kind kind, int tw, int th, int forced, boolean pixelArt) {
      if (kind == Category.Kind.GUI) {
         return new int[]{tw, th};
      } else if (kind != Category.Kind.ENTITY && kind != Category.Kind.LAYOUT) {
         if (forced > 0) {
            return new int[]{forced * tw / 16, forced * th / 16};
         } else {
            int n = img.w();
            return pixelArt && n % tw == 0 && n <= tw * 8 ? new int[]{n, n * th / tw} : new int[]{tw, th};
         }
      } else {
         int k = Math.max(1, Math.round((float)img.w() / tw));
         k = k >= 4 ? 4 : (k >= 2 ? 2 : 1);
         if (forced > 0) {
            k = Math.max(1, forced / 16);
         }

         return new int[]{tw * k, th * k};
      }
   }

   public static ImageProcessor.Img panoramaToSkybox(ImageProcessor.Img src, int n) {
      int maxDim = Math.max(src.w(), src.h());
      if (maxDim > 2048) {
         src = resizeArea(src, src.w() * 2048 / maxDim, src.h() * 2048 / maxDim);
      }

      int[] out = new int[3 * n * 2 * n];
      int[] topAvg = avgRows(src, 0, Math.max(1, src.h() / 20));
      int[] botAvg = avgRows(src, src.h() - Math.max(1, src.h() / 20), src.h());
      String[] faces = new String[]{"down", "up", "south", "west", "north", "east"};
      int[][] cells = new int[][]{{0, 0}, {1, 0}, {2, 0}, {0, 1}, {1, 1}, {2, 1}};
      double band = 0.12;
      double horizon = 0.5;
      double top = 80.0;
      double bottom = -80.0;

      for (int f = 0; f < 6; f++) {
         for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
               double a = 2.0 * (i + 0.5) / n - 1.0;
               double b = 1.0 - 2.0 * (j + 0.5) / n;
               String face = faces[f];
               double x;
               double y;
               double z;
               switch (face) {
                  case "north":
                     x = a;
                     y = b;
                     z = -1.0;
                     break;
                  case "east":
                     x = 1.0;
                     y = b;
                     z = a;
                     break;
                  case "south":
                     x = -a;
                     y = b;
                     z = 1.0;
                     break;
                  case "west":
                     x = -1.0;
                     y = b;
                     z = -a;
                     break;
                  case "up":
                     x = a;
                     y = 1.0;
                     z = b;
                     break;
                  default:
                     x = a;
                     y = -1.0;
                     z = -b;
               }

               double yaw = Math.toDegrees(Math.atan2(x, -z));
               double pitch = Math.toDegrees(Math.atan2(y, Math.hypot(x, z)));
               double t = ((yaw / 360.0 + 0.5) % 1.0 + 1.0) % 1.0 * (1.0 - band);
               double hy = horizon * (src.h() - 1);
               double py = pitch >= 0.0 ? hy - Math.min(1.0, pitch / top) * hy : hy + Math.min(1.0, pitch / bottom) * (src.h() - 1 - hy);
               int c = bilinear(src, t * (src.w() - 1), py);
               if (t < band) {
                  int c2 = bilinear(src, (1.0 - band + t) * (src.w() - 1), py);
                  c = lerpColor(c2, c, t / band);
               }

               if (pitch > top) {
                  c = lerpColor(c, rgb(topAvg), Math.min(1.0, (pitch - top) / (90.0 - top)));
               }

               if (pitch < bottom) {
                  c = lerpColor(c, rgb(botAvg), Math.min(1.0, (bottom - pitch) / (90.0 + bottom)));
               }

               int ox = cells[f][0] * n + i;
               int oy = cells[f][1] * n + j;
               out[oy * 3 * n + ox] = 0xFF000000 | c & 16777215;
            }
         }
      }

      return new ImageProcessor.Img(3 * n, 2 * n, out);
   }

   public static ImageProcessor.Img skyboxNorth(ImageProcessor.Img sky) {
      int n = sky.w() / 3;
      return sub(sky, n, sky.h() / 2, n, Math.min(n, sky.h() / 2));
   }

   private static int[] avgRows(ImageProcessor.Img img, int y0, int y1) {
      long r = 0L;
      long g = 0L;
      long b = 0L;
      long n = 0L;

      for (int y = y0; y < y1; y++) {
         for (int x = 0; x < img.w(); x++) {
            int c = img.get(x, y);
            r += c >> 16 & 0xFF;
            g += c >> 8 & 0xFF;
            b += c & 0xFF;
            n++;
         }
      }

      n = Math.max(1L, n);
      return new int[]{(int)(r / n), (int)(g / n), (int)(b / n)};
   }

   private static int rgb(int[] c) {
      return 0xFF000000 | c[0] << 16 | c[1] << 8 | c[2];
   }

   private static int bilinear(ImageProcessor.Img img, double x, double y) {
      x = Math.max(0.0, Math.min(img.w() - 1.001, x));
      y = Math.max(0.0, Math.min(img.h() - 1.001, y));
      int x0 = (int)x;
      int y0 = (int)y;
      double fx = x - x0;
      double fy = y - y0;
      int c00 = img.get(x0, y0);
      int c10 = img.get(Math.min(img.w() - 1, x0 + 1), y0);
      int c01 = img.get(x0, Math.min(img.h() - 1, y0 + 1));
      int c11 = img.get(Math.min(img.w() - 1, x0 + 1), Math.min(img.h() - 1, y0 + 1));
      return lerpColor(lerpColor(c00, c10, fx), lerpColor(c01, c11, fx), fy);
   }

   private static int lerpColor(int a, int b, double f) {
      int r = (int)((a >> 16 & 0xFF) * (1.0 - f) + (b >> 16 & 0xFF) * f);
      int g = (int)((a >> 8 & 0xFF) * (1.0 - f) + (b >> 8 & 0xFF) * f);
      int bl = (int)((a & 0xFF) * (1.0 - f) + (b & 0xFF) * f);
      return 0xFF000000 | r << 16 | g << 8 | bl;
   }

   static ImageProcessor.Img removeBackground(ImageProcessor.Img img) {
      int w = img.w();
      int h = img.h();
      int transparent = 0;

      for (int c : img.px()) {
         if (c >>> 24 < 16) {
            transparent++;
         }
      }

      if (transparent > img.px().length * 0.02) {
         return img;
      } else {
         Map<Integer, Integer> counts = new HashMap<>();
         List<Integer> border = new ArrayList<>();

         for (int x = 0; x < w; x++) {
            border.add(img.get(x, 0));
            border.add(img.get(x, h - 1));
         }

         for (int y = 0; y < h; y++) {
            border.add(img.get(0, y));
            border.add(img.get(w - 1, y));
         }

         for (int cx : border) {
            counts.merge(cx & 15790320, 1, Integer::sum);
         }

         int bgKey = counts.entrySet().stream().max(Entry.comparingByValue()).get().getKey();
         int bg = 0;
         long r = 0L;
         long g = 0L;
         long b = 0L;
         long n = 0L;

         for (int cx : border) {
            if ((cx & 15790320) == bgKey) {
               r += cx >> 16 & 0xFF;
               g += cx >> 8 & 0xFF;
               b += cx & 0xFF;
               n++;
            }
         }

         bg = (int)(r / n) << 16 | (int)(g / n) << 8 | (int)(b / n);
         int similar = 0;

         for (int cxx : border) {
            if (dist(cxx, bg) < 40.0) {
               similar++;
            }
         }

         if (similar < border.size() * 0.6) {
            return img;
         } else {
            boolean[] remove = new boolean[w * h];
            ArrayDeque<int[]> q = new ArrayDeque<>();

            for (int x = 0; x < w; x++) {
               q.add(new int[]{x, 0});
               q.add(new int[]{x, h - 1});
            }

            for (int y = 0; y < h; y++) {
               q.add(new int[]{0, y});
               q.add(new int[]{w - 1, y});
            }

            while (!q.isEmpty()) {
               int[] p = q.poll();
               int x = p[0];
               int y = p[1];
               if (x >= 0 && y >= 0 && x < w && y < h) {
                  int i = y * w + x;
                  if (!remove[i] && !(dist(img.px()[i], bg) >= 45.0)) {
                     remove[i] = true;
                     q.add(new int[]{x + 1, y});
                     q.add(new int[]{x - 1, y});
                     q.add(new int[]{x, y + 1});
                     q.add(new int[]{x, y - 1});
                  }
               }
            }

            removeEnclosed(img, remove, bg, Math.max(12, w * h / 400));
            int[] out = (int[])img.px().clone();

            for (int i = 0; i < out.length; i++) {
               if (remove[i]) {
                  out[i] = 0;
               }
            }

            for (int y = 0; y < h; y++) {
               for (int x = 0; x < w; x++) {
                  int ix = y * w + x;
                  if (!remove[ix] && touches(remove, w, h, x, y)) {
                     int cxxx = out[ix];
                     float a = (float)Math.min(1.0, dist(cxxx, bg) / 150.0);
                     if (a < 0.08F) {
                        out[ix] = 0;
                     } else {
                        int cr = clamp(((cxxx >> 16 & 0xFF) - (bg >> 16 & 0xFF) * (1.0F - a)) / a);
                        int cg = clamp(((cxxx >> 8 & 0xFF) - (bg >> 8 & 0xFF) * (1.0F - a)) / a);
                        int cb = clamp(((cxxx & 0xFF) - (bg & 0xFF) * (1.0F - a)) / a);
                        out[ix] = (int)(a * 255.0F) << 24 | cr << 16 | cg << 8 | cb;
                     }
                  }
               }
            }

            return new ImageProcessor.Img(w, h, out);
         }
      }
   }

   private static void removeEnclosed(ImageProcessor.Img img, boolean[] remove, int bg, int minArea) {
      int w = img.w();
      int h = img.h();
      boolean[] seen = new boolean[w * h];

      for (int start = 0; start < w * h; start++) {
         if (!seen[start] && !remove[start] && !(dist(img.px()[start], bg) >= 22.0)) {
            List<Integer> comp = new ArrayList<>();
            ArrayDeque<Integer> q = new ArrayDeque<>();
            q.add(start);
            seen[start] = true;

            while (!q.isEmpty()) {
               int i = q.poll();
               comp.add(i);
               int x = i % w;
               int y = i / w;
               int[] nb = new int[]{x > 0 ? i - 1 : -1, x < w - 1 ? i + 1 : -1, y > 0 ? i - w : -1, y < h - 1 ? i + w : -1};

               for (int j : nb) {
                  if (j >= 0 && !seen[j] && !remove[j] && !(dist(img.px()[j], bg) >= 22.0)) {
                     seen[j] = true;
                     q.add(j);
                  }
               }
            }

            if (comp.size() > minArea) {
               for (int i : comp) {
                  remove[i] = true;
               }
            }
         }
      }
   }

   private static boolean touches(boolean[] m, int w, int h, int x, int y) {
      for (int dy = -1; dy <= 1; dy++) {
         for (int dx = -1; dx <= 1; dx++) {
            int nx = x + dx;
            int ny = y + dy;
            if (nx >= 0 && ny >= 0 && nx < w && ny < h && m[ny * w + nx]) {
               return true;
            }
         }
      }

      return false;
   }

   static ImageProcessor.Img cropToContent(ImageProcessor.Img img) {
      int minX = img.w();
      int minY = img.h();
      int maxX = -1;
      int maxY = -1;

      for (int y = 0; y < img.h(); y++) {
         for (int x = 0; x < img.w(); x++) {
            if (img.get(x, y) >>> 24 > 16) {
               minX = Math.min(minX, x);
               maxX = Math.max(maxX, x);
               minY = Math.min(minY, y);
               maxY = Math.max(maxY, y);
            }
         }
      }

      return maxX < 0 ? img : sub(img, minX, minY, maxX - minX + 1, maxY - minY + 1);
   }

   static ImageProcessor.Img padToAspect(ImageProcessor.Img img, int tw, int th) {
      int w = img.w();
      int h = img.h();
      int nw = w;
      int nh = h;
      if (w * th > h * tw) {
         nh = (int)Math.ceil((double)(w * th) / tw);
      } else {
         nw = (int)Math.ceil((double)(h * tw) / th);
      }

      if (nw == w && nh == h) {
         return img;
      } else {
         int[] out = new int[nw * nh];
         int ox = (nw - w) / 2;
         int oy = (nh - h) / 2;

         for (int y = 0; y < h; y++) {
            System.arraycopy(img.px(), y * w, out, (y + oy) * nw + ox, w);
         }

         return new ImageProcessor.Img(nw, nh, out);
      }
   }

   static ImageProcessor.Img centerCropToAspect(ImageProcessor.Img img, int tw, int th) {
      int w = img.w();
      int h = img.h();
      int nw = w;
      int nh = h;
      if (w * th > h * tw) {
         nw = h * tw / th;
      } else {
         nh = w * th / tw;
      }

      return sub(img, (w - nw) / 2, (h - nh) / 2, nw, nh);
   }

   private static ImageProcessor.Img sub(ImageProcessor.Img img, int x0, int y0, int w, int h) {
      int[] out = new int[w * h];

      for (int y = 0; y < h; y++) {
         System.arraycopy(img.px(), (y + y0) * img.w() + x0, out, y * w, w);
      }

      return new ImageProcessor.Img(w, h, out);
   }

   static int detectBlockSize(ImageProcessor.Img img) {
      int g = 0;

      for (int y = 0; y < img.h(); y++) {
         int run = 1;

         for (int x = 1; x < img.w(); x++) {
            if (img.get(x, y) == img.get(x - 1, y)) {
               run++;
            } else {
               g = gcd(g, run);
               run = 1;
               if (g == 1) {
                  return 1;
               }
            }
         }

         g = gcd(g, run);
      }

      for (int xx = 0; xx < img.w(); xx++) {
         int run = 1;

         for (int y = 1; y < img.h(); y++) {
            if (img.get(xx, y) == img.get(xx, y - 1)) {
               run++;
            } else {
               g = gcd(g, run);
               run = 1;
               if (g == 1) {
                  return 1;
               }
            }
         }

         g = gcd(g, run);
      }

      return g > 1 && img.w() % g == 0 && img.h() % g == 0 ? g : 1;
   }

   static ImageProcessor.Img shrinkBlocks(ImageProcessor.Img img, int k) {
      int w = img.w() / k;
      int h = img.h() / k;
      int[] out = new int[w * h];

      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            out[y * w + x] = img.get(x * k, y * k);
         }
      }

      return new ImageProcessor.Img(w, h, out);
   }

   static int countColors(ImageProcessor.Img img, int stopAt) {
      Set<Integer> s = new HashSet<>();

      for (int c : img.px()) {
         if (c >>> 24 >= 16) {
            s.add(c);
            if (s.size() >= stopAt) {
               break;
            }
         }
      }

      return s.size();
   }

   static boolean looksLikePhoto(ImageProcessor.Img img) {
      if (img.w() <= 64 && img.h() <= 64) {
         return false;
      } else {
         int soft = 0;
         int total = 0;

         for (int y = 0; y < img.h(); y++) {
            for (int x = 1; x < img.w(); x++) {
               int a = img.get(x, y);
               int b = img.get(x - 1, y);
               if (a >>> 24 >= 16 && b >>> 24 >= 16) {
                  total++;
                  double d = dist(a, b);
                  if (d > 0.0 && d < 12.0) {
                     soft++;
                  }
               }
            }
         }

         return total > 0 && soft > total * 0.25;
      }
   }

   public static ImageProcessor.Img resizeNearest(ImageProcessor.Img img, int w, int h) {
      int[] out = new int[w * h];

      for (int y = 0; y < h; y++) {
         int sy = Math.min(img.h() - 1, (int)((y + 0.5) * img.h() / h));

         for (int x = 0; x < w; x++) {
            int sx = Math.min(img.w() - 1, (int)((x + 0.5) * img.w() / w));
            out[y * w + x] = img.get(sx, sy);
         }
      }

      return new ImageProcessor.Img(w, h, out);
   }

   public static ImageProcessor.Img resizeArea(ImageProcessor.Img img, int w, int h) {
      int[] out = new int[w * h];
      double sx = (double)img.w() / w;
      double sy = (double)img.h() / h;

      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            int x0 = (int)Math.floor(x * sx);
            int x1 = Math.max(x0 + 1, (int)Math.ceil((x + 1) * sx));
            int y0 = (int)Math.floor(y * sy);
            int y1 = Math.max(y0 + 1, (int)Math.ceil((y + 1) * sy));
            double a = 0.0;
            double r = 0.0;
            double g = 0.0;
            double b = 0.0;
            int n = 0;

            for (int yy = y0; yy < Math.min(y1, img.h()); yy++) {
               for (int xx = x0; xx < Math.min(x1, img.w()); xx++) {
                  int c = img.get(xx, yy);
                  double ca = (c >>> 24) / 255.0;
                  a += ca;
                  r += (c >> 16 & 0xFF) * ca;
                  g += (c >> 8 & 0xFF) * ca;
                  b += (c & 0xFF) * ca;
                  n++;
               }
            }

            if (n != 0 && !(a <= 0.0)) {
               out[y * w + x] = (int)Math.round(a / n * 255.0) << 24 | clamp(r / a) << 16 | clamp(g / a) << 8 | clamp(b / a);
            } else {
               out[y * w + x] = 0;
            }
         }
      }

      return new ImageProcessor.Img(w, h, out);
   }

   static ImageProcessor.Img thresholdAlpha(ImageProcessor.Img img) {
      int[] out = (int[])img.px().clone();

      for (int i = 0; i < out.length; i++) {
         out[i] = out[i] >>> 24 >= 128 ? out[i] | 0xFF000000 : 0;
      }

      return new ImageProcessor.Img(img.w(), img.h(), out);
   }

   static ImageProcessor.Img quantize(ImageProcessor.Img img, int maxColors) {
      List<int[]> colors = new ArrayList<>();

      for (int c : img.px()) {
         if (c >>> 24 >= 16) {
            colors.add(new int[]{c >> 16 & 0xFF, c >> 8 & 0xFF, c & 0xFF});
         }
      }

      if (colors.size() <= maxColors) {
         return img;
      } else {
         List<List<int[]>> boxes = new ArrayList<>();
         boxes.add(colors);

         while (boxes.size() < maxColors) {
            List<int[]> best = null;
            int bestRange = -1;
            int bestCh = 0;

            for (List<int[]> box : boxes) {
               if (box.size() >= 2) {
                  for (int ch = 0; ch < 3; ch++) {
                     int lo = 255;
                     int hi = 0;

                     for (int[] cx : box) {
                        lo = Math.min(lo, cx[ch]);
                        hi = Math.max(hi, cx[ch]);
                     }

                     if (hi - lo > bestRange) {
                        bestRange = hi - lo;
                        best = box;
                        bestCh = ch;
                     }
                  }
               }
            }

            if (best == null || bestRange <= 0) {
               break;
            }

            int ch = bestCh;
            best.sort(Comparator.comparingInt(cx -> cx[ch]));
            int mid = best.size() / 2;
            boxes.remove(best);
            boxes.add(new ArrayList<>(best.subList(0, mid)));
            boxes.add(new ArrayList<>(best.subList(mid, best.size())));
         }

         int[][] palette = new int[boxes.size()][3];

         for (int i = 0; i < boxes.size(); i++) {
            long r = 0L;
            long g = 0L;
            long b = 0L;

            for (int[] cx : boxes.get(i)) {
               r += cx[0];
               g += cx[1];
               b += cx[2];
            }

            int n = Math.max(1, boxes.get(i).size());
            palette[i] = new int[]{(int)(r / n), (int)(g / n), (int)(b / n)};
         }

         int[] out = (int[])img.px().clone();

         for (int i = 0; i < out.length; i++) {
            int cx = out[i];
            if (cx >>> 24 >= 16) {
               int r = cx >> 16 & 0xFF;
               int g = cx >> 8 & 0xFF;
               int b = cx & 0xFF;
               int best = 0;
               long bd = Long.MAX_VALUE;

               for (int p = 0; p < palette.length; p++) {
                  long d = sq(r - palette[p][0]) + sq(g - palette[p][1]) + sq(b - palette[p][2]);
                  if (d < bd) {
                     bd = d;
                     best = p;
                  }
               }

               out[i] = cx & 0xFF000000 | palette[best][0] << 16 | palette[best][1] << 8 | palette[best][2];
            }
         }

         return new ImageProcessor.Img(img.w(), img.h(), out);
      }
   }

   static double dist(int a, int b) {
      int dr = (a >> 16 & 0xFF) - (b >> 16 & 0xFF);
      int dg = (a >> 8 & 0xFF) - (b >> 8 & 0xFF);
      int db = (a & 0xFF) - (b & 0xFF);
      return Math.sqrt(dr * dr + dg * dg + db * db);
   }

   private static long sq(int v) {
      return (long)v * v;
   }

   private static int clamp(double v) {
      return (int)Math.max(0L, Math.min(255L, Math.round(v)));
   }

   private static int gcd(int a, int b) {
      return b == 0 ? a : gcd(b, a % b);
   }

   static String debug(ImageProcessor.Img img) {
      return img.w() + "x" + img.h() + " " + Arrays.hashCode(img.px());
   }

   public record Img(int w, int h, int[] px) {
      int get(int x, int y) {
         return this.px[y * this.w + x];
      }

      void set(int x, int y, int c) {
         this.px[y * this.w + x] = c;
      }

      public BufferedImage toBuffered() {
         BufferedImage b = new BufferedImage(this.w, this.h, 2);
         b.setRGB(0, 0, this.w, this.h, this.px, 0, this.w);
         return b;
      }

      public static ImageProcessor.Img of(BufferedImage b) {
         int w = b.getWidth();
         int h = b.getHeight();
         int[] px = b.getRGB(0, 0, w, h, null, 0, w);
         return new ImageProcessor.Img(w, h, px);
      }
   }
}
