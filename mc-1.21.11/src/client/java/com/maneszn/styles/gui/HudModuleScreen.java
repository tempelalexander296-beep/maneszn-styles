package com.maneszn.styles.gui;

import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.hud.HudManager;
import java.util.function.IntConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class HudModuleScreen extends Screen {
   private static final int[] TEXT_COLORS = new int[]{0, 16777215, 11032063, 16736210, 16726843, 16752410, 16769357, 5111658, 3399935, 3898367, 65793};
   private static final int[] BG_COLORS = new int[]{0, 1773616, 2756160, 926272, 932394, 4198424, 3815994, 16777215};
   private final Screen parent;
   private final String id;
   private final Ui.Zones zones = new Ui.Zones();

   public HudModuleScreen(Screen parent, String moduleId) {
      super(Component.translatable("maneszn_styles.hud.module." + moduleId));
      this.parent = parent;
      this.id = moduleId;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
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
      ModConfig.HudPos p = HudManager.pos(this.id);
      int pw = Math.min(this.width - 20, 300);
      int ph = Math.min(this.height - 16, 300);
      int px = (this.width - pw) / 2;
      int py = (this.height - ph) / 2;
      Ui.rect(g, px, py, pw, ph, -15462116);
      Ui.border(g, px, py, pw, ph, th.accent);
      Ui.textShadow(g, Ui.trim(Ui.tr("maneszn_styles.hudmod.title", Ui.tr("maneszn_styles.hud.module." + this.id)), pw - 16), px + 8, py + 6, -1185282);
      int prevH = 44;
      Ui.rect(g, px + 8, py + 18, pw - 16, prevH, -12957094);
      HudManager.Module m = null;

      for (HudManager.Module mod : HudManager.modules()) {
         if (mod.id().equals(this.id)) {
            m = mod;
         }
      }

      if (m != null) {
         HudManager.editing = true;

         try {
            int[] sz = HudManager.scaledSize(this.minecraft, m, p);
            if (sz != null) {
               int cx = px + 8 + (pw - 16 - sz[0]) / 2;
               int cy = py + 18 + Math.max(2, (prevH - sz[1]) / 2);
               g.enableScissor(px + 8, py + 18, px + pw - 8, py + 18 + prevH);
               HudManager.drawModule(g, this.minecraft, m, new int[]{cx, cy}, p.scale);
               g.disableScissor();
            }
         } finally {
            HudManager.editing = false;
         }
      }

      int x = px + 8;
      int w = pw - 16;
      int y = py + 18 + prevH + 6;
      y = this.stepper(
         g,
         mx,
         my,
         th,
         x,
         y,
         w,
         Ui.tr("maneszn_styles.hudmod.size"),
         Math.round(p.scale * 100.0) + "%",
         () -> p.scale = Math.max(0.5, Math.round((p.scale - 0.1) * 10.0) / 10.0),
         () -> p.scale = Math.min(3.0, Math.round((p.scale + 0.1) * 10.0) / 10.0)
      );
      Ui.text(g, Ui.tr("maneszn_styles.hudmod.text_color"), x + 2, y + 1, -9739136);
      y = this.swatches(g, x, y + 10, w, TEXT_COLORS, p.textColor, true, c -> p.textColor = c);
      y = this.toggle(g, mx, my, th, x, y, w, Ui.tr("maneszn_styles.hudmod.background"), p.background, () -> p.background = !p.background);
      if (p.background) {
         y = this.stepper(
            g,
            mx,
            my,
            th,
            x,
            y,
            w,
            Ui.tr("maneszn_styles.hudmod.opacity"),
            p.bgOpacity + "%",
            () -> p.bgOpacity = Math.max(0, p.bgOpacity - 10),
            () -> p.bgOpacity = Math.min(100, p.bgOpacity + 10)
         );
         Ui.text(g, Ui.tr("maneszn_styles.hudmod.bg_color"), x + 2, y + 1, -9739136);
         y = this.swatches(g, x, y + 10, w, BG_COLORS, p.bgColor, false, c -> p.bgColor = c);
      }

      y = this.toggle(g, mx, my, th, x, y, w, Ui.tr("maneszn_styles.hudmod.accent"), p.accentBar, () -> p.accentBar = !p.accentBar);
      y = this.toggle(g, mx, my, th, x, y, w, Ui.tr("maneszn_styles.hudmod.shadow"), p.shadow, () -> p.shadow = !p.shadow);

      for (String[] o : HudManager.options(this.id)) {
         String key = o[0];
         boolean def = Boolean.parseBoolean(o[1]);
         boolean on = HudManager.opt(this.id, key, def);
         y = this.toggle(
            g, mx, my, th, x, y, w, Ui.tr("maneszn_styles.hudmod.opt." + this.id + "." + key), on, () -> p.opts.put(key, !HudManager.opt(this.id, key, def))
         );
      }

      int by = py + ph - 22;
      int bw = (w - 4) / 3;
      String[] labels = new String[]{Ui.tr("maneszn_styles.hudmod.reset"), Ui.tr("maneszn_styles.hudmod.remove"), Ui.tr("maneszn_styles.hud.done")};
      Runnable[] actions = new Runnable[]{() -> {
         HudManager.resetStyle(this.id);
         p.scale = 1.0;
         ModConfig.save();
      }, () -> {
         p.on = false;
         ModConfig.save();
         this.onClose();
      }, this::onClose};

      for (int i = 0; i < 3; i++) {
         int bx = x + i * (bw + 2);
         Ui.button(g, bx, by, bw, 16, Ui.trim(labels[i], bw - 4), Ui.hover(mx, my, bx, by, bw, 16), i == 2, th);
         this.zones.add(bx, by, bw, 16, actions[i]);
      }
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

   private int stepper(GuiGraphics g, int mx, int my, Theme th, int x, int y, int w, String label, String value, Runnable minus, Runnable plus) {
      Ui.text(g, Ui.trim(label, w - 80), x + 3, y + 3, -1185282);
      int bw = 14;
      int plusX = x + w - bw;
      int minusX = plusX - bw - 36;
      Ui.button(g, minusX, y, bw, 13, "−", Ui.hover(mx, my, minusX, y, bw, 13), false, th);
      Ui.textCentered(g, value, minusX + bw + 18, y + 3, th.accent);
      Ui.button(g, plusX, y, bw, 13, "+", Ui.hover(mx, my, plusX, y, bw, 13), false, th);
      this.zones.add(minusX, y, bw, 13, () -> {
         minus.run();
         ModConfig.save();
      });
      this.zones.add(plusX, y, bw, 13, () -> {
         plus.run();
         ModConfig.save();
      });
      return y + 15;
   }

   private int swatches(GuiGraphics g, int x, int y, int w, int[] colors, int value, boolean withDefault, IntConsumer set) {
      int sx = x + 2;

      for (int c : colors) {
         boolean isDefault = withDefault && c == 0;
         boolean sel = isDefault ? value == 0 : (value & 16777215) == (c & 16777215) && (value != 0 || !withDefault);
         Ui.rect(g, sx, y, 12, 12, sel ? -1 : -13752510);
         if (isDefault) {
            Ui.rect(g, sx + 1, y + 1, 10, 10, -15001818);
            Ui.textCentered(g, "S", sx + 6, y + 2, -6646866);
         } else {
            Ui.rect(g, sx + 1, y + 1, 10, 10, 0xFF000000 | c);
         }

         int col = isDefault ? 0 : c;
         this.zones.add(sx, y, 12, 12, () -> {
            set.accept(col);
            ModConfig.save();
         });
         sx += 15;
         if (sx + 12 > x + w) {
            break;
         }
      }

      return y + 16;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      return event.button() == 0 && this.zones.click(event.x(), event.y(), 0) ? true : super.mouseClicked(event, doubleClick);
   }

   @Override
   public void onClose() {
      ModConfig.save();
      this.minecraft.setScreen(this.parent);
   }
}
