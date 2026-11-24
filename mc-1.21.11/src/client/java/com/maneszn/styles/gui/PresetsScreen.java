package com.maneszn.styles.gui;

import com.maneszn.styles.ManesznStyles;
import com.maneszn.styles.pack.StylePackBuilder;
import com.maneszn.styles.preset.PresetManager;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class PresetsScreen extends Screen {
   private final StylesScreen parent;
   private final Ui.Zones zones = new Ui.Zones();
   private EditBox nameBox;
   private String message;
   private int messageColor = -6646866;
   private String armedDelete;
   private double scroll;
   private int maxScroll;

   public PresetsScreen(StylesScreen parent) {
      super(Component.translatable("maneszn_styles.presets.title"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      PresetManager.get().reload();
      String old = this.nameBox == null ? "" : this.nameBox.getValue();
      int w = this.panelW();
      int x = (this.width - w) / 2;
      this.nameBox = new EditBox(this.font, x + 8, 44, w - 16 - 90, 16, Component.translatable("maneszn_styles.presets.name"));
      this.nameBox.setMaxLength(32);
      this.nameBox.setHint(Component.translatable("maneszn_styles.presets.name_hint"));
      this.nameBox.setValue(old);
      this.addRenderableWidget(this.nameBox);
   }

   private int panelW() {
      return Math.min(420, this.width - 20);
   }

   @Override
   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      Ui.background(g, this.width, this.height);
   }

   @Override
   public void render(GuiGraphics g, int mx, int my, float pt) {
      this.zones.clear();
      Theme th = Theme.current();
      Ui.rect(g, 0, 0, this.width, 24, -15462116);
      Ui.rect(g, 0, 23, this.width, 1, -13752510);
      Ui.textShadow(g, Ui.tr("maneszn_styles.presets.title"), 8, 8, th.accent);
      int w = this.panelW();
      int x = (this.width - w) / 2;
      Ui.rect(g, x, 30, w, this.height - 34, -15462116);
      int sbx = x + w - 8 - 84;
      boolean canSave = !this.nameBox.getValue().isBlank();
      boolean hovS = Ui.hover(mx, my, sbx, 44, 84, 16);
      Ui.button(g, sbx, 44, 84, 16, Ui.tr("maneszn_styles.presets.save"), hovS && canSave, canSave, th);
      if (canSave) {
         this.zones.add(sbx, 44, 84, 16, this::saveCurrent);
      }

      Ui.text(g, Ui.tr("maneszn_styles.presets.save_hint"), x + 8, 33, -9739136);
      boolean hovP = Ui.hover(mx, my, x + 8, 66, w - 16, 16);
      Ui.button(g, x + 8, 66, w - 16, 16, Ui.tr("maneszn_styles.presets.paste"), hovP, false, th);
      this.zones.add(x + 8, 66, w - 16, 16, this::pasteCode);
      int listY = 90;
      int listH = this.height - listY - 40;
      List<PresetManager.Preset> list = PresetManager.get().presets();
      if (list.isEmpty()) {
         Ui.textCentered(g, Ui.tr("maneszn_styles.presets.empty"), x + w / 2, listY + 20, -9739136);
      }

      g.enableScissor(x, listY, x + w, listY + listH);
      int ry = listY - (int)this.scroll;

      for (PresetManager.Preset p : list) {
         boolean vis = ry + 22 > listY && ry < listY + listH;
         if (vis) {
            Ui.rect(g, x + 8, ry, w - 16, 22, -14607314);
            Ui.text(g, Ui.trim(p.name, w - 200), x + 14, ry + 3, -1185282);
            Ui.text(g, Ui.tr("maneszn_styles.presets.info", p.styles.size(), p.created == null ? "" : p.created), x + 14, ry + 12, -9739136);
            int bx = x + w - 12;
            boolean armed = p.name.equals(this.armedDelete);
            String[] labels = new String[]{
               Ui.tr(armed ? "maneszn_styles.presets.delete_confirm" : "maneszn_styles.presets.delete"),
               Ui.tr("maneszn_styles.presets.share"),
               Ui.tr("maneszn_styles.presets.load")
            };
            Runnable[] acts = new Runnable[]{() -> this.delete(p), () -> this.share(p), () -> this.load(p)};

            for (int i = 0; i < labels.length; i++) {
               int bw = Ui.width(labels[i]) + 10;
               bx -= bw;
               boolean hov = my >= listY && my < listY + listH && Ui.hover(mx, my, bx, ry + 3, bw, 16);
               Ui.button(g, bx, ry + 3, bw, 16, labels[i], hov, i == 2, th);
               this.zones.addClipped(bx, ry + 3, bw, 16, listY, listY + listH, acts[i], null);
               bx -= 4;
            }
         }

         ry += 25;
      }

      g.disableScissor();
      this.maxScroll = Math.max(0, list.size() * 25 - listH);
      if (this.message != null) {
         Ui.textCentered(g, Ui.trim(this.message, w - 16), x + w / 2, this.height - 34, this.messageColor);
      }

      boolean hovB = Ui.hover(mx, my, x + 8, this.height - 22, 80, 16);
      Ui.button(g, x + 8, this.height - 22, 80, 16, Ui.tr("maneszn_styles.ui.back"), hovB, false, th);
      this.zones.add(x + 8, this.height - 22, 80, 16, this::onClose);
      super.render(g, mx, my, pt);
   }

   private void saveCurrent() {
      try {
         PresetManager.Preset p = PresetManager.get().saveCurrent(this.nameBox.getValue());
         this.info(Ui.tr("maneszn_styles.presets.saved", p.name), -14498466);
         this.nameBox.setValue("");
      } catch (Exception var2) {
         ManesznStyles.LOGGER.error("Preset speichern fehlgeschlagen", var2);
         this.info(var2.getMessage(), -1096636);
      }
   }

   private void load(PresetManager.Preset p) {
      int skipped = PresetManager.get().apply(p);
      StylePackBuilder.applyAndReload();
      this.info(skipped == 0 ? Ui.tr("maneszn_styles.presets.loaded", p.name) : Ui.tr("maneszn_styles.presets.loaded_skipped", p.name, skipped), -14498466);
   }

   private void share(PresetManager.Preset p) {
      String code = PresetManager.toCode(p);
      this.minecraft.keyboardHandler.setClipboard(code);
      this.info(Ui.tr("maneszn_styles.presets.copied", code.length()), -14498466);
   }

   private void delete(PresetManager.Preset p) {
      if (!p.name.equals(this.armedDelete)) {
         this.armedDelete = p.name;
      } else {
         this.armedDelete = null;

         try {
            PresetManager.get().delete(p);
            this.info(Ui.tr("maneszn_styles.presets.deleted", p.name), -6646866);
         } catch (Exception var3) {
            this.info(var3.getMessage(), -1096636);
         }
      }
   }

   private void pasteCode() {
      String clip = this.minecraft.keyboardHandler.getClipboard();

      try {
         PresetManager.Preset p = PresetManager.fromCode(clip == null ? "" : clip);
         int skipped = PresetManager.get().apply(p);
         PresetManager.get().saveCurrent(p.name);
         StylePackBuilder.applyAndReload();
         this.info(skipped == 0 ? Ui.tr("maneszn_styles.presets.loaded", p.name) : Ui.tr("maneszn_styles.presets.loaded_skipped", p.name, skipped), -14498466);
      } catch (Exception var4) {
         this.info(Ui.tr("maneszn_styles.presets.bad_code"), -1096636);
      }
   }

   private void info(String msg, int color) {
      this.message = msg;
      this.messageColor = color;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (this.nameBox.isMouseOver(event.x(), event.y())) {
         return super.mouseClicked(event, doubleClick);
      } else {
         if (this.nameBox.isFocused()) {
            this.nameBox.setFocused(false);
            this.setFocused(null);
         }

         return this.zones.click(event.x(), event.y(), event.button()) ? true : super.mouseClicked(event, doubleClick);
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      this.scroll = Math.max(0.0, Math.min((double)this.maxScroll, this.scroll - scrollY * 20.0));
      return true;
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (event.key() == 257 && this.nameBox.isFocused() && !this.nameBox.getValue().isBlank()) {
         this.saveCurrent();
         return true;
      } else {
         return super.keyPressed(event);
      }
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }
}
