package com.maneszn.styles.hud;

import com.maneszn.styles.config.ModConfig;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

public final class Crosshair {
   public static final int PIXEL_GRID = 15;
   public static final String[] SHAPES = new String[]{"cross", "dot", "circle", "t", "x", "square", "arrow", "plus_dot", "pixel"};
   public static final int[] COLORS = new int[]{-1, -16777216, -50373, -24806, -7859, -11665558, -13377281, -12878849, -5745153, -41006};

   private Crosshair() {
   }

   public static boolean[][] grid(ModConfig.CrosshairCfg c) {
      if ("pixel".equals(c.shape)) {
         return pixelGrid(c);
      } else {
         int size = clamp(c.size, 1, 12);
         int th = clamp(c.thickness, 1, 4);
         int gap = clamp(c.gap, 0, 8);
         int r = size + gap + th + 1;
         int n = 2 * r + 1;
         boolean[][] g = new boolean[n][n];
         int lo = -(th - 1) / 2;
         int hi = th / 2;
         String var9 = c.shape == null ? "cross" : c.shape;
         switch (var9) {
            case "dot": {
               int s = th + (size >= 6 ? 1 : 0);
               fill(g, r, -(s - 1) / 2, s / 2, -(s - 1) / 2, s / 2);
               break;
            }
            case "circle":
               double rad = Math.round(size / 2.0 + gap / 2.0 + 1.0) + 0.3;

               for (int y = -r; y <= r; y++) {
                  for (int x = -r; x <= r; x++) {
                     double d = Math.sqrt(x * x + y * y);
                     if (Math.abs(d - rad) <= th * 0.5) {
                        set(g, r, x, y);
                     }
                  }
               }
               break;
            case "t":
               fill(g, r, -gap - size, -gap - 1, lo, hi);
               fill(g, r, gap + 1, gap + size, lo, hi);
               fill(g, r, lo, hi, gap + 1, gap + size);
               break;
            case "x":
               for (int i = gap + 1; i <= gap + size; i++) {
                  for (int t = lo; t <= hi; t++) {
                     set(g, r, i + t, i);
                     set(g, r, -i + t, i);
                     set(g, r, i + t, -i);
                     set(g, r, -i + t, -i);
                  }
               }
               break;
            case "square": {
               int s = (size + gap) / 2 + 1;

               for (int i = -s; i <= s; i++) {
                  for (int t = 0; t < th; t++) {
                     set(g, r, i, -s + t);
                     set(g, r, i, s - t);
                     set(g, r, -s + t, i);
                     set(g, r, s - t, i);
                  }
               }
               break;
            }
            case "arrow":
               for (int i = 0; i < size + 1; i++) {
                  for (int t = 0; t < th; t++) {
                     set(g, r, -i - t, gap + i);
                     set(g, r, i + t, gap + i);
                  }
               }
               break;
            default:
               fill(g, r, -gap - size, -gap - 1, lo, hi);
               fill(g, r, gap + 1, gap + size, lo, hi);
               fill(g, r, lo, hi, -gap - size, -gap - 1);
               fill(g, r, lo, hi, gap + 1, gap + size);
               if ("plus_dot".equals(c.shape)) {
                  fill(g, r, lo, hi, lo, hi);
               }
         }

         if (c.centerDot) {
            fill(g, r, lo, hi, lo, hi);
         }

         return g;
      }
   }

   private static boolean[][] pixelGrid(ModConfig.CrosshairCfg c) {
      boolean[][] g = new boolean[15][15];
      List<String> rows = c.pixels;
      if (rows == null || rows.isEmpty()) {
         rows = defaultPixels();
      }

      for (int y = 0; y < 15 && y < rows.size(); y++) {
         String row = rows.get(y);

         for (int x = 0; x < 15 && x < row.length(); x++) {
            g[y][x] = row.charAt(x) == '#';
         }
      }

      return g;
   }

   public static List<String> defaultPixels() {
      List<String> rows = new ArrayList<>();

      for (int y = 0; y < 15; y++) {
         StringBuilder b = new StringBuilder();

         for (int x = 0; x < 15; x++) {
            boolean on = x == 7 && y >= 3 && y <= 11 && y != 7 || y == 7 && x >= 3 && x <= 11 && x != 7;
            b.append((char)(on ? '#' : '.'));
         }

         rows.add(b.toString());
      }

      return rows;
   }

   private static void fill(boolean[][] g, int r, int x0, int x1, int y0, int y1) {
      for (int y = y0; y <= y1; y++) {
         for (int x = x0; x <= x1; x++) {
            set(g, r, x, y);
         }
      }
   }

   private static void set(boolean[][] g, int r, int x, int y) {
      int gx = x + r;
      int gy = y + r;
      if (gy >= 0 && gy < g.length && gx >= 0 && gx < g.length) {
         g[gy][gx] = true;
      }
   }

   public static int color(ModConfig.CrosshairCfg c) {
      if (!c.rainbow) {
         return c.color | 0xFF000000;
      } else {
         float h = (float)(System.currentTimeMillis() % 3000L) / 3000.0F;
         return 0xFF000000 | Color.HSBtoRGB(h, 0.85F, 1.0F) & 16777215;
      }
   }

   public static void draw(GuiGraphics gr, boolean[][] g, int cx, int cy, int px, int color, boolean outline) {
      int n = g.length;
      int r = n / 2;
      int x0 = cx - r * px;
      int y0 = cy - r * px;
      if (outline) {
         for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
               if (g[y][x]) {
                  for (int dy = -1; dy <= 1; dy++) {
                     for (int dx = -1; dx <= 1; dx++) {
                        if (dx != 0 || dy != 0) {
                           int nx = x + dx;
                           int ny = y + dy;
                           boolean inside = nx >= 0 && ny >= 0 && nx < n && ny < n && g[ny][nx];
                           if (!inside) {
                              gr.fill(x0 + nx * px, y0 + ny * px, x0 + nx * px + px, y0 + ny * px + px, -1073741824);
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      for (int y = 0; y < n; y++) {
         for (int xx = 0; xx < n; xx++) {
            if (g[y][xx]) {
               gr.fill(x0 + xx * px, y0 + y * px, x0 + xx * px + px, y0 + y * px + px, color);
            }
         }
      }
   }

   private static int clamp(int v, int lo, int hi) {
      return Math.max(lo, Math.min(hi, v));
   }
}
