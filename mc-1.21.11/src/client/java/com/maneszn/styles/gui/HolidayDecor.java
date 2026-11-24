package com.maneszn.styles.gui;

import net.minecraft.client.gui.GuiGraphics;

final class HolidayDecor {
   private HolidayDecor() {
   }

   static void draw(GuiGraphics g, Theme th, int width, int height) {
      if (th.holiday()) {
         double t = System.currentTimeMillis() / 1000.0;
         switch (th) {
            case WEIHNACHTEN:
               snow(g, t, width, height);
               break;
            case HALLOWEEN:
               halloween(g, t, width);
               break;
            case OSTERN:
               easter(g, t, width, height);
         }
      }
   }

   private static void snow(GuiGraphics g, double t, int w, int h) {
      for (int i = 0; i < 46; i++) {
         double speed = 8 + i * 37 % 13;
         int x = (int)(i * 97 % Math.max(1, w) + Math.sin(t * 0.8 + i) * 6.0);
         int y = (int)((t * speed + i * 53) % (h + 10)) - 5;
         int a = 80 + i * 23 % 80;
         int s = i % 5 == 0 ? 2 : 1;
         Ui.rect(g, x, y, s, s, a << 24 | 16777215);
      }

      int[] cols = new int[]{-1689274, -13779348, -10163, -11688193};
      int x = 6;

      for (int i = 0; x < w; i++) {
         boolean on = ((int)(t * 2.0) + i) % 3 != 0;
         int c = cols[i % cols.length];
         Ui.rect(g, x, 22, 3, 3, on ? c : 1610612736 | c & 16777215);
         x += 18;
      }
   }

   private static void halloween(GuiGraphics g, double t, int w) {
      for (int i = 0; i < 4; i++) {
         int x = (int)((t * (14 + i * 5) + i * 140) % (w + 40)) - 20;
         int y = 6 + (int)(Math.sin(t * 2.0 + i) * 3.0) + i % 2 * 6;
         boolean up = ((int)(t * 6.0) + i) % 2 == 0;
         int c = -1340863970;
         Ui.rect(g, x + 3, y, 2, 3, c);
         Ui.rect(g, x, y + (up ? 0 : 1), 3, 1, c);
         Ui.rect(g, x + 5, y + (up ? 0 : 1), 3, 1, c);
         Ui.rect(g, x + 1, y + (up ? -1 : 2), 1, 1, c);
         Ui.rect(g, x + 6, y + (up ? -1 : 2), 1, 1, c);
      }

      int c = 1090519039;

      for (int i = 0; i < 14; i++) {
         Ui.rect(g, w - 1 - i, i, 1, 1, c);
         Ui.rect(g, w - 1 - i / 2, i, 1, 1, c);
      }
   }

   private static void easter(GuiGraphics g, double t, int w, int h) {
      int[] cols = new int[]{-546600, -7347280, -8054, -5715713};

      for (int i = 0; i < 4; i++) {
         int bx = i < 2 ? 6 + i * 10 : w - 26 + (i - 2) * 10;
         int by = h - 12 - (int)Math.abs(Math.sin(t * 3.0 + i) * 3.0);
         int c = cols[i];
         Ui.rect(g, bx + 1, by, 3, 1, c);
         Ui.rect(g, bx, by + 1, 5, 5, c);
         Ui.rect(g, bx + 1, by + 6, 3, 1, c);
         Ui.rect(g, bx, by + 3, 5, 1, -1);
      }

      for (int i = 0; i < 14; i++) {
         int x = (int)(i * 131 % Math.max(1, w) + Math.sin(t + i) * 8.0);
         int y = (int)((t * (6 + i % 5) + i * 71) % (h + 10)) - 5;
         Ui.rect(g, x, y, 2, 1, 1879048192 | cols[i % 4] & 16777215);
      }
   }
}
