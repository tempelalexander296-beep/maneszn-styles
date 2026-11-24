package com.maneszn.styles.gui;

import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.hud.HudManager;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HudEditScreen extends Screen {
   private static final double MIN_SCALE = 0.5;
   private static final double MAX_SCALE = 3.0;
   private final Screen parent;
   private final Ui.Zones zones = new Ui.Zones();
   private String dragging;
   private double dragDx;
   private double dragDy;
   private boolean panelHidden;
   private String scaleInfoId;
   private long scaleInfoUntil;
   private int panelX;
   private int panelY;
   private int panelW;
   private int panelH;

   public HudEditScreen(Screen parent) {
      super(Component.translatable("maneszn_styles.hud.title"));
      this.parent = parent;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      g.fill(0, 0, this.width, this.height, 1426063360);
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.render(g, mouseX, mouseY, partialTick);
      this.zones.clear();
      Theme th = Theme.current();
      HudManager.editing = true;

      try {
         for (HudManager.Module m : HudManager.modules()) {
            ModConfig.HudPos p = HudManager.pos(m.id());
            if (p.on) {
               int[] at = this.placeHere(m, p);
               HudManager.drawModule(g, this.minecraft, m, at, p.scale);
               boolean hov = Ui.hover(mouseX, mouseY, at[0], at[1], at[2], at[3]) && !this.overPanel(mouseX, mouseY);
               Ui.border(g, at[0] - 1, at[1] - 1, at[2] + 2, at[3] + 2, !hov && !m.id().equals(this.dragging) ? -1996488705 : th.accent);
               boolean info = m.id().equals(this.scaleInfoId) && System.currentTimeMillis() < this.scaleInfoUntil;
               if (hov || info) {
                  String s = Math.round(p.scale * 100.0) + "%";
                  int ty = at[1] > 12 ? at[1] - 11 : at[1] + at[3] + 3;
                  Ui.rect(g, at[0] - 1, ty - 1, Ui.width(s) + 4, 10, -872415232);
                  Ui.text(g, s, at[0] + 1, ty, th.accent);
               }
            }
         }
      } finally {
         HudManager.editing = false;
      }

      if (!this.panelHidden) {
         this.renderPanel(g, mouseX, mouseY, th);
      } else {
         this.panelW = 0;
      }

      Ui.textCentered(g, Ui.tr("maneszn_styles.hud.hint"), this.width / 2, this.height - 14, -6646866);
   }

   private void renderPanel(GuiGraphics g, int mouseX, int mouseY, Theme th) {
      ModConfig cfg = ModConfig.get();
      List<HudManager.Module> mods = HudManager.modules();
      int colW = 160;
      this.panelW = colW * 2 + 18;
      int modRows = (mods.size() + 1) / 2;
      this.panelH = 22 + modRows * 15 + 8 + 45 + 8 + 20 + 6;
      this.panelX = (this.width - this.panelW) / 2;
      this.panelY = Math.max(6, (this.height - this.panelH) / 2);
      Ui.rect(g, this.panelX, this.panelY, this.panelW, this.panelH, -15462116);
      Ui.border(g, this.panelX, this.panelY, this.panelW, this.panelH, th.accent);
      Ui.textCentered(g, Ui.tr("maneszn_styles.hud.title"), this.panelX + this.panelW / 2, this.panelY + 7, -1185282);
      int y0 = this.panelY + 22;

      for (int i = 0; i < mods.size(); i++) {
         HudManager.Module m = mods.get(i);
         ModConfig.HudPos p = HudManager.pos(m.id());
         int cx = this.panelX + 6 + i % 2 * (colW + 6);
         int cy = y0 + i / 2 * 15;
         boolean hg = Ui.hover(mouseX, mouseY, cx, cy, 14, 14);
         Ui.button(g, cx, cy, 14, 14, "⚙", hg, false, th);
         String mid = m.id();
         this.zones.add(cx, cy, 14, 14, () -> this.minecraft.setScreen(new HudModuleScreen(this, mid)));
         this.toggle(g, mouseX, mouseY, th, cx + 16, cy, colW - 16, Ui.tr("maneszn_styles.hud.module." + m.id()), p.on, () -> {
            p.on = !p.on;
            ModConfig.save();
         });
      }

      int y = y0 + modRows * 15 + 4;
      Ui.rect(g, this.panelX + 6, y, this.panelW - 12, 1, -13752510);
      y += 4;
      this.toggle(g, mouseX, mouseY, th, this.panelX + 6, y, colW, Ui.tr("maneszn_styles.hud.toggle_sprint"), cfg.toggleSprint, () -> {
         cfg.toggleSprint = !cfg.toggleSprint;
         ModConfig.save();
      });
      this.toggle(g, mouseX, mouseY, th, this.panelX + 12 + colW, y, colW, Ui.tr("maneszn_styles.hud.fullbright"), cfg.fullbright, () -> {
         cfg.fullbright = !cfg.fullbright;
         ModConfig.save();
      });
      y += 15;
      this.toggle(g, mouseX, mouseY, th, this.panelX + 6, y, colW, Ui.tr("maneszn_styles.hud.freelook_toggle"), cfg.freelookToggle, () -> {
         cfg.freelookToggle = !cfg.freelookToggle;
         ModConfig.save();
      });
      this.toggle(g, mouseX, mouseY, th, this.panelX + 12 + colW, y, colW, Ui.tr("maneszn_styles.hud.zoom_smooth"), cfg.zoomSmooth, () -> {
         cfg.zoomSmooth = !cfg.zoomSmooth;
         ModConfig.save();
      });
      y += 15;
      this.toggle(g, mouseX, mouseY, th, this.panelX + 6, y, colW, Ui.tr("maneszn_styles.hud.custom_crosshair"), cfg.crosshair.on, () -> {
         cfg.crosshair.on = !cfg.crosshair.on;
         ModConfig.save();
      });
      y += 19;
      int bw = (this.panelW - 12 - 6) / 3;
      String[] labels = new String[]{Ui.tr("maneszn_styles.hud.crosshair_edit"), Ui.tr("maneszn_styles.hud.reset"), Ui.tr("maneszn_styles.hud.done")};
      Runnable[] actions = new Runnable[]{() -> this.minecraft.setScreen(new CrosshairScreen(this)), () -> {
         cfg.hud.clear();
         ModConfig.save();
      }, this::onClose};

      for (int i = 0; i < 3; i++) {
         int bx = this.panelX + 6 + i * (bw + 2);
         boolean hov = Ui.hover(mouseX, mouseY, bx, y, bw, 16);
         Ui.button(g, bx, y, bw, 16, Ui.trim(labels[i], bw - 4), hov, i == 2, th);
         this.zones.add(bx, y, bw, 16, actions[i]);
      }
   }

   private void toggle(GuiGraphics g, int mx, int my, Theme th, int x, int y, int w, String label, boolean on, Runnable r) {
      boolean hov = Ui.hover(mx, my, x, y, w, 14);
      if (hov) {
         Ui.rect(g, x, y, w, 14, -14015424);
      }

      Ui.text(g, Ui.trim(label, w - 30), x + 4, y + 3, -1185282);
      Ui.rect(g, x + w - 24, y + 3, 18, 8, on ? th.accent : -13752510);
      Ui.rect(g, on ? x + w - 14 : x + w - 24, y + 3, 8, 8, -1);
      this.zones.add(x, y, w, 14, r);
   }

   private boolean overPanel(double mx, double my) {
      return !this.panelHidden && this.panelW > 0 && mx >= this.panelX && mx < this.panelX + this.panelW && my >= this.panelY && my < this.panelY + this.panelH;
   }

   private HudManager.Module moduleAt(double mx, double my) {
      if (this.overPanel(mx, my)) {
         return null;
      } else {
         List<HudManager.Module> mods = HudManager.modules();

         for (int i = mods.size() - 1; i >= 0; i--) {
            HudManager.Module m = mods.get(i);
            ModConfig.HudPos p = HudManager.pos(m.id());
            if (p.on) {
               int[] at = this.placeHere(m, p);
               if (Ui.hover((int)mx, (int)my, at[0], at[1], at[2], at[3])) {
                  return m;
               }
            }
         }

         return null;
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (event.button() == 0 && this.zones.click(event.x(), event.y(), 0)) {
         return true;
      } else {
         HudManager.Module m = this.moduleAt(event.x(), event.y());
         if (m != null && event.button() == 0 && doubleClick) {
            this.dragging = null;
            this.minecraft.setScreen(new HudModuleScreen(this, m.id()));
            return true;
         } else if (m != null && event.button() == 0) {
            int[] at = this.placeHere(m, HudManager.pos(m.id()));
            this.dragging = m.id();
            this.dragDx = event.x() - at[0];
            this.dragDy = event.y() - at[1];
            return true;
         } else if (m != null && event.button() == 1) {
            HudManager.pos(m.id()).on = false;
            ModConfig.save();
            return true;
         } else {
            return super.mouseClicked(event, doubleClick);
         }
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      HudManager.Module m = this.moduleAt(mouseX, mouseY);
      if (m != null && scrollY != 0.0) {
         ModConfig.HudPos p = HudManager.pos(m.id());
         double step = scrollY > 0.0 ? 0.1 : -0.1;
         p.scale = Math.round(Math.max(0.5, Math.min(3.0, p.scale + step)) * 10.0) / 10.0;
         this.scaleInfoId = m.id();
         this.scaleInfoUntil = System.currentTimeMillis() + 1200L;
         ModConfig.save();
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (event.key() == 258) {
         this.panelHidden = !this.panelHidden;
         return true;
      } else {
         return super.keyPressed(event);
      }
   }

   private int[] placeHere(HudManager.Module m, ModConfig.HudPos p) {
      int[] sz = HudManager.scaledSize(this.minecraft, m, p);
      if (sz == null) {
         sz = new int[]{(int)(60.0 * p.scale), (int)(13.0 * p.scale)};
      }

      int x = (int)Math.round(p.x * this.width);
      int y = (int)Math.round(p.y * this.height);
      x = Math.max(0, Math.min(this.width - sz[0], x));
      y = Math.max(0, Math.min(this.height - sz[1], y));
      return new int[]{x, y, sz[0], sz[1]};
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
      if (this.dragging != null) {
         ModConfig.HudPos p = HudManager.pos(this.dragging);
         p.x = Math.max(0.0, Math.min(1.0, (event.x() - this.dragDx) / this.width));
         p.y = Math.max(0.0, Math.min(1.0, (event.y() - this.dragDy) / this.height));
         return true;
      } else {
         return super.mouseDragged(event, dx, dy);
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      if (this.dragging != null) {
         this.dragging = null;
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
