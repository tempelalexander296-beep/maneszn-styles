package com.maneszn.styles.gui;

import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.hud.Crosshair;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class CrosshairScreen extends Screen {
   private final Screen parent;
   private final Ui.Zones zones = new Ui.Zones();
   private int gridX;
   private int gridY;
   private int cell;
   private Boolean painting;

   public CrosshairScreen(Screen parent) {
      super(Component.translatable("maneszn_styles.crosshair.title"));
      this.parent = parent;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private static ModConfig.CrosshairCfg cfg() {
      return ModConfig.get().crosshair;
   }

   @Override
   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      g.fill(0, 0, this.width, this.height, -1442512118);
   }

   @Override
   public void render(GuiGraphics g, int mx, int my, float partialTick) {
      super.render(g, mx, my, partialTick);
      this.zones.clear();
      Theme th = Theme.current();
      ModConfig.CrosshairCfg c = cfg();
      int pw = Math.min(this.width - 20, 420);
      int ph = Math.min(this.height - 20, 250);
      int px = (this.width - pw) / 2;
      int py = (this.height - ph) / 2;
      Ui.rect(g, px, py, pw, ph, -15462116);
      Ui.border(g, px, py, pw, ph, th.accent);
      Ui.textShadow(g, Ui.tr("maneszn_styles.crosshair.title"), px + 8, py + 6, -1185282);
      boolean[][] grid = Crosshair.grid(c);
      int box = Math.min(ph - 70, pw / 2 - 20);
      int bx = px + 8;
      int by = py + 22;
      Ui.rect(g, bx, by, box, box, -12959926);
      int n = "pixel".equals(c.shape) ? 15 : grid.length;
      this.cell = Math.max(2, box / Math.max(n, 15));
      this.gridX = bx + (box - this.cell * n) / 2;
      this.gridY = by + (box - this.cell * n) / 2;

      for (int y = 0; y < n; y++) {
         for (int x = 0; x < n; x++) {
            if ((x + y) % 2 == 0) {
               g.fill(
                  this.gridX + x * this.cell,
                  this.gridY + y * this.cell,
                  this.gridX + x * this.cell + this.cell,
                  this.gridY + y * this.cell + this.cell,
                  -12301738
               );
            }
         }
      }

      Crosshair.draw(g, grid, this.gridX + n / 2 * this.cell, this.gridY + n / 2 * this.cell, this.cell, Crosshair.color(c), c.outline);
      if ("pixel".equals(c.shape)) {
         Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.crosshair.paint_hint"), box), bx, by + box + 4, -6646866);
      }

      int sy = by + box + 16;
      Ui.text(g, Ui.tr("maneszn_styles.crosshair.real_size"), bx, sy, -9739136);
      Ui.rect(g, bx, sy + 10, 30, 30, -7883288);
      Ui.rect(g, bx + 32, sy + 10, 30, 30, -15000028);
      Crosshair.draw(g, grid, bx + 15, sy + 25, 1, Crosshair.color(c), c.outline);
      Crosshair.draw(g, grid, bx + 47, sy + 25, 1, Crosshair.color(c), c.outline);
      int rx = px + pw / 2 + 4;
      int rw = pw / 2 - 12;
      int y = py + 22;
      y = this.toggle(g, mx, my, th, rx, y, rw, Ui.tr("maneszn_styles.crosshair.enabled"), c.on, () -> c.on = !c.on);
      Ui.text(g, Ui.tr("maneszn_styles.crosshair.shape"), rx, y + 1, -9739136);
      y += 11;
      int sw = (rw - 4) / 3;

      for (int i = 0; i < Crosshair.SHAPES.length; i++) {
         String s = Crosshair.SHAPES[i];
         int sx = rx + i % 3 * (sw + 2);
         int syy = y + i / 3 * 15;
         boolean hov = Ui.hover(mx, my, sx, syy, sw, 13);
         Ui.button(g, sx, syy, sw, 13, Ui.trim(Ui.tr("maneszn_styles.crosshair.shape." + s), sw - 4), hov, s.equals(c.shape), th);
         this.zones.add(sx, syy, sw, 13, () -> {
            if ("pixel".equals(s) && (c.pixels == null || c.pixels.isEmpty())) {
               c.pixels = fromShape(c);
            }

            c.shape = s;
         });
      }

      y += (Crosshair.SHAPES.length + 2) / 3 * 15 + 2;
      if (!"pixel".equals(c.shape)) {
         y = this.stepper(g, mx, my, th, rx, y, rw, "size", c.size, 1, 12, v -> c.size = v);
         y = this.stepper(g, mx, my, th, rx, y, rw, "thickness", c.thickness, 1, 4, v -> c.thickness = v);
         y = this.stepper(g, mx, my, th, rx, y, rw, "gap", c.gap, 0, 8, v -> c.gap = v);
      } else {
         int hw = (rw - 2) / 2;
         boolean h1 = Ui.hover(mx, my, rx, y, hw, 13);
         boolean h2 = Ui.hover(mx, my, rx + hw + 2, y, hw, 13);
         Ui.button(g, rx, y, hw, 13, Ui.trim(Ui.tr("maneszn_styles.crosshair.clear"), hw - 4), h1, true, th);
         Ui.button(g, rx + hw + 2, y, hw, 13, Ui.trim(Ui.tr("maneszn_styles.crosshair.reset_pixels"), hw - 4), h2, true, th);
         this.zones.add(rx, y, hw, 13, () -> c.pixels = emptyPixels());
         this.zones.add(rx + hw + 2, y, hw, 13, () -> c.pixels = Crosshair.defaultPixels());
         y += 16;
      }

      Ui.text(g, Ui.tr("maneszn_styles.crosshair.color"), rx, y + 1, -9739136);
      y += 11;
      int sq = Math.min(14, (rw - 18) / 10);

      for (int i = 0; i < Crosshair.COLORS.length; i++) {
         int col = Crosshair.COLORS[i];
         int cx = rx + i * (sq + 2);
         boolean sel = !c.rainbow && (c.color | 0xFF000000) == col;
         Ui.rect(g, cx, y, sq, sq, sel ? th.accent : -13752510);
         Ui.rect(g, cx + 1, y + 1, sq - 2, sq - 2, col);
         this.zones.add(cx, y, sq, sq, () -> {
            c.color = col;
            c.rainbow = false;
         });
      }

      y += sq + 4;
      y = this.toggle(g, mx, my, th, rx, y, rw, Ui.tr("maneszn_styles.crosshair.rainbow"), c.rainbow, () -> c.rainbow = !c.rainbow);
      y = this.toggle(g, mx, my, th, rx, y, rw, Ui.tr("maneszn_styles.crosshair.outline"), c.outline, () -> c.outline = !c.outline);
      if (!"pixel".equals(c.shape)) {
         y = this.toggle(g, mx, my, th, rx, y, rw, Ui.tr("maneszn_styles.crosshair.center_dot"), c.centerDot, () -> c.centerDot = !c.centerDot);
      }

      int dy = py + ph - 22;
      boolean hov = Ui.hover(mx, my, rx, dy, rw, 16);
      Ui.button(g, rx, dy, rw, 16, Ui.tr("maneszn_styles.hud.done"), hov, true, th);
      this.zones.add(rx, dy, rw, 16, this::onClose);
   }

   private int toggle(GuiGraphics g, int mx, int my, Theme th, int x, int y, int w, String label, boolean on, Runnable r) {
      boolean hov = Ui.hover(mx, my, x, y, w, 14);
      if (hov) {
         Ui.rect(g, x, y, w, 14, -14015424);
      }

      Ui.text(g, Ui.trim(label, w - 30), x + 3, y + 3, -1185282);
      Ui.rect(g, x + w - 22, y + 3, 18, 8, on ? th.accent : -13752510);
      Ui.rect(g, on ? x + w - 12 : x + w - 22, y + 3, 8, 8, -1);
      this.zones.add(x, y, w, 14, () -> {
         r.run();
         ModConfig.save();
      });
      return y + 15;
   }

   private int stepper(GuiGraphics g, int mx, int my, Theme th, int x, int y, int w, String key, int value, int min, int max, IntConsumer set) {
      Ui.text(g, Ui.tr("maneszn_styles.crosshair." + key), x + 3, y + 3, -1185282);
      int bw = 14;
      int plusX = x + w - bw;
      int minusX = plusX - bw - 26;
      boolean hm = Ui.hover(mx, my, minusX, y, bw, 13);
      boolean hp = Ui.hover(mx, my, plusX, y, bw, 13);
      Ui.button(g, minusX, y, bw, 13, "−", hm, value > min, th);
      Ui.textCentered(g, String.valueOf(value), minusX + bw + 13, y + 3, th.accent);
      Ui.button(g, plusX, y, bw, 13, "+", hp, value < max, th);
      this.zones.add(minusX, y, bw, 13, () -> {
         set.accept(Math.max(min, value - 1));
         ModConfig.save();
      });
      this.zones.add(plusX, y, bw, 13, () -> {
         set.accept(Math.min(max, value + 1));
         ModConfig.save();
      });
      return y + 15;
   }

   private static List<String> emptyPixels() {
      List<String> rows = new ArrayList<>();

      for (int i = 0; i < 15; i++) {
         rows.add(".".repeat(15));
      }

      return rows;
   }

   private static List<String> fromShape(ModConfig.CrosshairCfg c) {
      boolean[][] g = Crosshair.grid(c);
      int n = g.length;
      int off = (n - 15) / 2;
      List<String> rows = new ArrayList<>();

      for (int y = 0; y < 15; y++) {
         StringBuilder b = new StringBuilder();

         for (int x = 0; x < 15; x++) {
            int gx = x + off;
            int gy = y + off;
            b.append((char)(gx >= 0 && gy >= 0 && gx < n && gy < n && g[gy][gx] ? '#' : '.'));
         }

         rows.add(b.toString());
      }

      return rows;
   }

   private boolean paintAt(double mx, double my, boolean on) {
      ModConfig.CrosshairCfg c = cfg();
      if ("pixel".equals(c.shape) && this.cell > 0) {
         int x = (int)Math.floor((mx - this.gridX) / this.cell);
         int y = (int)Math.floor((my - this.gridY) / this.cell);
         if (x >= 0 && y >= 0 && x < 15 && y < 15) {
            if (c.pixels == null || c.pixels.size() != 15) {
               c.pixels = emptyPixels();
            }

            StringBuilder row = new StringBuilder(c.pixels.get(y));

            while (row.length() < 15) {
               row.append('.');
            }

            row.setCharAt(x, (char)(on ? '#' : '.'));
            c.pixels.set(y, row.toString());
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (event.button() == 0 && this.zones.click(event.x(), event.y(), 0)) {
         return true;
      } else if ((event.button() == 0 || event.button() == 1) && this.paintAt(event.x(), event.y(), event.button() == 0)) {
         this.painting = event.button() == 0;
         return true;
      } else {
         return super.mouseClicked(event, doubleClick);
      }
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
      if (this.painting != null) {
         this.paintAt(event.x(), event.y(), this.painting);
         return true;
      } else {
         return super.mouseDragged(event, dx, dy);
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      if (this.painting != null) {
         this.painting = null;
         ModConfig.save();
         return true;
      } else {
         return super.mouseReleased(event);
      }
   }

   @Override
   public void onClose() {
      ModConfig.save();
      this.minecraft.setScreen(this.parent);
   }
}
