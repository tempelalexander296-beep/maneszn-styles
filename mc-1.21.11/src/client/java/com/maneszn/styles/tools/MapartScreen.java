package com.maneszn.styles.tools;

import com.maneszn.styles.gui.FileDialogs;
import com.maneszn.styles.gui.TextureCache;
import com.maneszn.styles.gui.Theme;
import com.maneszn.styles.gui.Ui;
import com.maneszn.styles.image.ImageProcessor;
import com.maneszn.styles.render.PreviewScenes;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.MapColor.Brightness;

public class MapartScreen extends ToolScreen {
   private static final Block[] PALETTE = new Block[]{
      Blocks.WHITE_CONCRETE,
      Blocks.ORANGE_CONCRETE,
      Blocks.MAGENTA_CONCRETE,
      Blocks.LIGHT_BLUE_CONCRETE,
      Blocks.YELLOW_CONCRETE,
      Blocks.LIME_CONCRETE,
      Blocks.PINK_CONCRETE,
      Blocks.GRAY_CONCRETE,
      Blocks.LIGHT_GRAY_CONCRETE,
      Blocks.CYAN_CONCRETE,
      Blocks.PURPLE_CONCRETE,
      Blocks.BLUE_CONCRETE,
      Blocks.BROWN_CONCRETE,
      Blocks.GREEN_CONCRETE,
      Blocks.RED_CONCRETE,
      Blocks.BLACK_CONCRETE,
      Blocks.WHITE_TERRACOTTA,
      Blocks.ORANGE_TERRACOTTA,
      Blocks.MAGENTA_TERRACOTTA,
      Blocks.LIGHT_BLUE_TERRACOTTA,
      Blocks.YELLOW_TERRACOTTA,
      Blocks.LIME_TERRACOTTA,
      Blocks.PINK_TERRACOTTA,
      Blocks.GRAY_TERRACOTTA,
      Blocks.LIGHT_GRAY_TERRACOTTA,
      Blocks.CYAN_TERRACOTTA,
      Blocks.PURPLE_TERRACOTTA,
      Blocks.BLUE_TERRACOTTA,
      Blocks.BROWN_TERRACOTTA,
      Blocks.GREEN_TERRACOTTA,
      Blocks.RED_TERRACOTTA,
      Blocks.BLACK_TERRACOTTA,
      Blocks.GRASS_BLOCK,
      Blocks.SAND,
      Blocks.SNOW_BLOCK,
      Blocks.IRON_BLOCK,
      Blocks.GOLD_BLOCK,
      Blocks.DIAMOND_BLOCK,
      Blocks.LAPIS_BLOCK,
      Blocks.EMERALD_BLOCK,
      Blocks.REDSTONE_BLOCK,
      Blocks.OAK_PLANKS,
      Blocks.SPRUCE_PLANKS,
      Blocks.BIRCH_PLANKS,
      Blocks.DARK_OAK_PLANKS,
      Blocks.CRIMSON_PLANKS,
      Blocks.WARPED_PLANKS,
      Blocks.STONE,
      Blocks.CLAY,
      Blocks.NETHERRACK,
      Blocks.PRISMARINE,
      Blocks.OBSIDIAN,
      Blocks.WARPED_WART_BLOCK,
      Blocks.CRIMSON_NYLIUM,
      Blocks.DEEPSLATE,
      Blocks.RAW_IRON_BLOCK,
      Blocks.GLOW_LICHEN,
      Blocks.PACKED_ICE,
      Blocks.HONEY_BLOCK,
      Blocks.TERRACOTTA,
      Blocks.MUD
   };
   private static int size = 1;
   private static boolean dither = true;
   private static ImageProcessor.Img source;
   private static ImageProcessor.Img result;
   private static Map<String, Integer> counts = new LinkedHashMap<>();
   private static String status = "";

   public MapartScreen(Screen parent) {
      super("maneszn_styles.mapart.title", parent);
   }

   @Override
   protected int maxWidth() {
      return 440;
   }

   @Override
   protected void draw(GuiGraphics g, int mx, int my, Theme th) {
      int x = this.px + 10;
      int y = this.py + 24;
      boolean h1 = Ui.hover(mx, my, x, y, 110, 16);
      Ui.button(g, x, y, 110, 16, Ui.tr("maneszn_styles.mapart.pick"), h1, true, th);
      this.zones.add(x, y, 110, 16, this::pick);
      int bx = x + 116;

      for (int s = 1; s <= 3; s++) {
         boolean hov = Ui.hover(mx, my, bx, y, 34, 16);
         Ui.button(g, bx, y, 34, 16, s + "×" + s, hov, s == size, th);
         int ss = s;
         this.zones.add(bx, y, 34, 16, () -> {
            size = ss;
            convert();
         });
         bx += 37;
      }

      boolean hd = Ui.hover(mx, my, bx + 4, y, 90, 16);
      Ui.button(g, bx + 4, y, 90, 16, Ui.tr(dither ? "maneszn_styles.mapart.dither_on" : "maneszn_styles.mapart.dither_off"), hd, dither, th);
      this.zones.add(bx + 4, y, 90, 16, () -> {
         dither = !dither;
         convert();
      });
      y += 22;
      if (!status.isEmpty()) {
         Ui.text(g, Ui.trim(status, this.pw - 20), x, y, -6646866);
         y += 12;
      }

      if (result == null) {
         Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.mapart.hint"), this.pw - 20), x, y + 4, -9739136);
      } else {
         int box = Math.min(this.pw / 2 - 15, this.py + this.ph - y - 10);
         int iy = y;
         TextureCache.replaceImage("mapart_result", result).ifPresent(t -> Ui.texture(g, t, x, iy, box, box));
         int lx = x + box + 10;
         int total = 0;

         for (int c : counts.values()) {
            total += c;
         }

         Ui.textShadow(g, Ui.tr("maneszn_styles.mapart.blocks", total, result.w() + "×" + result.h()), lx, y, -1185282);
         int ly = y + 14;

         for (Entry<String, Integer> e : counts.entrySet()) {
            if (ly > this.py + this.ph - 12) {
               Ui.text(g, "…", lx, ly, -9739136);
               break;
            }

            int n = e.getValue();
            String stacks = n >= 64 ? " (" + n / 64 + "×64" + (n % 64 > 0 ? "+" + n % 64 : "") + ")" : "";
            Ui.text(g, Ui.trim(e.getKey() + ": " + n + stacks, this.px + this.pw - 10 - lx), lx, ly, -6646866);
            ly += 10;
         }
      }
   }

   private void pick() {
      FileDialogs.openImage(Ui.tr("maneszn_styles.mapart.pick")).thenAccept(opt -> opt.ifPresent(p -> this.minecraft.execute(() -> {
         try (InputStream in = Files.newInputStream(p)) {
            source = PreviewScenes.decode(in);
            status = p.getFileName().toString();
            convert();
         } catch (Exception var6) {
            status = Ui.tr("maneszn_styles.mapart.error");
         }
      })));
   }

   private static void convert() {
      if (source != null) {
         int n = 128 * size;
         ImageProcessor.Img img = ImageProcessor.resizeArea(source, n, n);
         int[] cols = new int[PALETTE.length];
         String[] names = new String[PALETTE.length];

         for (int i = 0; i < PALETTE.length; i++) {
            MapColor mc = PALETTE[i].defaultMapColor();
            cols[i] = mc.calculateARGBColor(Brightness.NORMAL);
            names[i] = PALETTE[i].getName().getString();
         }

         float[][] err = new float[n * n][3];
         int[] out = new int[n * n];
         int[] cnt = new int[PALETTE.length];

         for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
               int i = y * n + x;
               int c = img.px()[i];
               if (c >>> 24 < 128) {
                  out[i] = 0;
               } else {
                  float r = (c >> 16 & 0xFF) + err[i][0];
                  float gg = (c >> 8 & 0xFF) + err[i][1];
                  float b = (c & 0xFF) + err[i][2];
                  int best = 0;
                  float bd = Float.MAX_VALUE;

                  for (int k = 0; k < cols.length; k++) {
                     int pc = cols[k];
                     float dr = r - (pc >> 16 & 0xFF);
                     float dg = gg - (pc >> 8 & 0xFF);
                     float db = b - (pc & 0xFF);
                     float d = dr * dr * 0.3F + dg * dg * 0.59F + db * db * 0.11F;
                     if (d < bd) {
                        bd = d;
                        best = k;
                     }
                  }

                  int pc = cols[best];
                  out[i] = 0xFF000000 | pc & 16777215;
                  cnt[best]++;
                  if (dither) {
                     float er = r - (pc >> 16 & 0xFF);
                     float eg = gg - (pc >> 8 & 0xFF);
                     float eb = b - (pc & 0xFF);
                     spread(err, n, x + 1, y, er, eg, eb, 0.4375F);
                     spread(err, n, x - 1, y + 1, er, eg, eb, 0.1875F);
                     spread(err, n, x, y + 1, er, eg, eb, 0.3125F);
                     spread(err, n, x + 1, y + 1, er, eg, eb, 0.0625F);
                  }
               }
            }
         }

         result = new ImageProcessor.Img(n, n, out);
         List<Integer> idx = new ArrayList<>();

         for (int kx = 0; kx < cnt.length; kx++) {
            if (cnt[kx] > 0) {
               idx.add(kx);
            }
         }

         idx.sort((a, bx) -> cnt[bx] - cnt[a]);
         counts = new LinkedHashMap<>();

         for (int kxx : idx) {
            counts.put(names[kxx], cnt[kxx]);
         }
      }
   }

   private static void spread(float[][] err, int n, int x, int y, float r, float g, float b, float f) {
      if (x >= 0 && x < n && y < n) {
         float[] e = err[y * n + x];
         e[0] += r * f;
         e[1] += g * f;
         e[2] += b * f;
      }
   }
}
