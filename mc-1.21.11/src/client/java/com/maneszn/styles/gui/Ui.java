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

   /**
    * v24 design: theme surfaces (panels, cards, selections) are rendered as raised 3D tiles –
    * vertical gradient, light top edge, dark bottom edge, soft drop shadow and an accent glow.
    * Every screen picks this up automatically because they all draw their surfaces through here.
    */
   public static void rect(GuiGraphics g, int x, int y, int w, int h, int color) {
      if (w >= 10 && h >= 8) {
         Theme th = Theme.current();
         if (color == Theme.PANEL) { surface(g, x, y, w, h, 0xFF1C1830, 0xFF110E1D, 0x24FFFFFF, 0xFF07060C, 3, 0); return; }
         if (color == Theme.PANEL_2) { surface(g, x, y, w, h, 0xFF24203A, 0xFF181429, 0x22FFFFFF, 0xFF0A0812, 2, 0); return; }
         if (color == Theme.CARD) { surface(g, x, y, w, h, 0xFF2A2542, 0xFF1B172C, 0x2AFFFFFF, 0xFF0B0914, 2, 0); return; }
         if (color == Theme.CARD_HOVER) { surface(g, x, y, w, h, Theme.mix(th.accent, 0xFF2E2848, 0.82F), 0xFF201B33, 0x40FFFFFF, 0xFF0B0914, 2, th.accentAlpha(0x70)); return; }
         if (color == th.accentDim()) { surface(g, x, y, w, h, Theme.mix(th.accent, 0xFF2A2542, 0.55F), Theme.mix(th.accent, 0xFF120F1F, 0.78F), 0x50FFFFFF, 0xFF07060C, 2, th.accentAlpha(0xC0)); return; }
      }
      g.fill(x, y, x + w, y + h, color);
   }

   /** Raised tile: shadow, gradient body, bevel edges, optional glow outline. */
   public static void surface(GuiGraphics g, int x, int y, int w, int h, int top, int bottom, int light, int dark, int depth, int glow) {
      for (int i = depth; i >= 1; i--) g.fill(x + i, y + i, x + w + i, y + h + i, (0x30 + 0x18 * (depth - i)) << 24);
      g.fillGradient(x, y, x + w, y + h, top, bottom);
      g.fill(x + 1, y, x + w - 1, y + 1, light);
      g.fill(x, y + 1, x + 1, y + h - 1, light & 0x60FFFFFF);
      g.fill(x + 1, y + h - 1, x + w - 1, y + h, dark);
      g.fill(x + w - 1, y + 1, x + w, y + h - 1, dark & 0x90FFFFFF | 0x40000000);
      if (glow != 0) {
         border(g, x - 1, y - 1, w + 2, h + 2, glow);
         g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, glow);
      }
   }

   /** Screen background: deep night gradient tinted by the theme accent, soft top glow and vignette. */
   public static void background(GuiGraphics g, int w, int h) {
      Theme th = Theme.current();
      g.fillGradient(0, 0, w, h, Theme.mix(th.accent, 0xF00B0916, 0.88F) | 0xF0000000, 0xF407060D);
      g.fillGradient(0, 0, w, h / 3, th.accentAlpha(0x22), 0);
      g.fillGradient(0, h - h / 4, w, h, 0, 0x60000000);
      if (th.holiday()) g.fillGradient(0, h - h / 5, w, h, 0, (th.accent2 & 0xFFFFFF) | 0x18000000);
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

   /** 3D push button: extruded base, gradient face, highlight; lifts on hover. */
   public static void button(GuiGraphics g, int x, int y, int w, int h, String label, boolean hovered, boolean primary, Theme th) {
      int lift = hovered ? 1 : 0;
      int base = primary ? Theme.mix(th.accent, 0xFF000000, 0.55F) : 0xFF07060C;
      g.fill(x, y + 2, x + w, y + h + 1, base);
      int top = primary ? Theme.mix(th.accent, 0xFFFFFFFF, hovered ? 0.35F : 0.22F) : (hovered ? 0xFF3A3358 : 0xFF2C2744);
      int bottom = primary ? th.accent : (hovered ? 0xFF241F38 : 0xFF1C1830);
      g.fillGradient(x, y - lift, x + w, y + h - lift, top, bottom);
      g.fill(x + 1, y - lift, x + w - 1, y + 1 - lift, primary ? 0x90FFFFFF : 0x30FFFFFF);
      if (hovered) border(g, x - 1, y - 1 - lift, w + 2, h + 2, th.accentAlpha(0x90));
      int color = primary ? 0xFF140E1F : (hovered ? -1185282 : -6646866);
      textCentered(g, trim(label, w - 6), x + w / 2, y + (h - 8) / 2 - lift, color);
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
