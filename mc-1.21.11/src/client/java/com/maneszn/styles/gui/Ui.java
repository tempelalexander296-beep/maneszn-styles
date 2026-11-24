package com.maneszn.styles.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public final class Ui {
   private Ui() {
   }

   public static boolean hover(int mx, int my, int x, int y, int w, int h) {
      return mx >= x && mx < x + w && my >= y && my < y + h;
   }

   public static void rect(GuiGraphics g, int x, int y, int w, int h, int color) {
      g.fill(x, y, x + w, y + h, color);
   }

   public static void border(GuiGraphics g, int x, int y, int w, int h, int color) {
      g.fill(x, y, x + w, y + 1, color);
      g.fill(x, y + h - 1, x + w, y + h, color);
      g.fill(x, y + 1, x + 1, y + h - 1, color);
      g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
   }

   public static void text(GuiGraphics g, String s, int x, int y, int color) {
      g.drawString(font(), s, x, y, color, false);
   }

   public static void textShadow(GuiGraphics g, String s, int x, int y, int color) {
      g.drawString(font(), s, x, y, color, true);
   }

   public static void component(GuiGraphics g, Component c, int x, int y, int color) {
      g.drawString(font(), c, x, y, color, true);
   }

   public static void textCentered(GuiGraphics g, String s, int cx, int y, int color) {
      g.drawString(font(), s, cx - font().width(s) / 2, y, color, false);
   }

   public static void textRight(GuiGraphics g, String s, int right, int y, int color) {
      g.drawString(font(), s, right - font().width(s), y, color, false);
   }

   public static String trim(String s, int maxW) {
      Font f = font();
      if (f.width(s) <= maxW) {
         return s;
      } else {
         String ell = "…";
         return f.plainSubstrByWidth(s, Math.max(0, maxW - f.width(ell))) + ell;
      }
   }

   public static int width(String s) {
      return font().width(s);
   }

   public static Font font() {
      return Minecraft.getInstance().font;
   }

   public static void button(GuiGraphics g, int x, int y, int w, int h, String label, boolean hovered, boolean primary, Theme th) {
      int bg = primary ? (hovered ? Theme.mix(th.accent, -1, 0.15F) : th.accent) : (hovered ? -14015424 : -14607314);
      rect(g, x, y, w, h, bg);
      border(g, x, y, w, h, primary ? Theme.mix(th.accent, -16777216, 0.3F) : -13752510);
      int color = primary ? -16054254 : (hovered ? -1185282 : -6646866);
      textCentered(g, trim(label, w - 6), x + w / 2, y + (h - 8) / 2, color);
   }

   public static void texture(GuiGraphics g, TextureCache.Tex tex, int x, int y, int w, int h) {
      int tw = tex.width();
      int th = tex.frameHeight();
      float scale = Math.min((float)w / tw, (float)h / th);
      if (scale >= 1.0F) {
         scale = (float)Math.floor(scale);
      }

      int dw = Math.max(1, Math.round(tw * scale));
      int dh = Math.max(1, Math.round(th * scale));
      int dx = x + (w - dw) / 2;
      int dy = y + (h - dh) / 2;
      g.blit(RenderPipelines.GUI_TEXTURED, tex.id(), dx, dy, 0.0F, 0.0F, dw, dh, tw, th, tex.width(), tex.height());
   }

   public static String tr(String key, Object... args) {
      return Component.translatable(key, args).getString();
   }

   public static String lang() {
      Options options = Minecraft.getInstance() == null ? null : Minecraft.getInstance().options;
      String l = options == null ? null : options.languageCode;
      return l == null ? "en_us" : l;
   }

   public static String langShort() {
      return lang().startsWith("de") ? "de" : "en";
   }

   public static void clickSound() {
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   public record Zone(int x1, int y1, int x2, int y2, Runnable left, Runnable right) {
      public boolean contains(double x, double y) {
         return x >= this.x1 && x < this.x2 && y >= this.y1 && y < this.y2;
      }
   }

   public static final class Zones {
      private final List<Ui.Zone> list = new ArrayList<>();

      public void clear() {
         this.list.clear();
      }

      public void add(int x, int y, int w, int h, Runnable left) {
         this.add(x, y, w, h, left, null);
      }

      public void add(int x, int y, int w, int h, Runnable left, Runnable right) {
         if (w > 0 && h > 0) {
            this.list.add(new Ui.Zone(x, y, x + w, y + h, left, right));
         }
      }

      public void addClipped(int x, int y, int w, int h, int clipY1, int clipY2, Runnable left, Runnable right) {
         int y1 = Math.max(y, clipY1);
         int y2 = Math.min(y + h, clipY2);
         if (y2 > y1) {
            this.list.add(new Ui.Zone(x, y1, x + w, y2, left, right));
         }
      }

      public boolean click(double x, double y, int button) {
         for (int i = this.list.size() - 1; i >= 0; i--) {
            Ui.Zone z = this.list.get(i);
            if (z.contains(x, y)) {
               Runnable r = button == 0 ? z.left() : (button == 1 ? z.right() : null);
               if (r == null) {
                  return button == 0;
               }

               Ui.clickSound();
               r.run();
               return true;
            }
         }

         return false;
      }
   }
}
