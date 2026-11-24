package com.maneszn.styles.tools;

import com.maneszn.styles.gui.Theme;
import com.maneszn.styles.gui.Ui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public abstract class ToolScreen extends Screen {
   protected final Screen parent;
   protected final Ui.Zones zones = new Ui.Zones();
   protected int px;
   protected int py;
   protected int pw;
   protected int ph;

   protected ToolScreen(String titleKey, Screen parent) {
      super(Component.translatable(titleKey));
      this.parent = parent;
   }

   protected int maxWidth() {
      return 360;
   }

   @Override
   protected void init() {
      this.pw = Math.min(this.width - 20, this.maxWidth());
      this.ph = this.height - 20;
      this.px = (this.width - this.pw) / 2;
      this.py = 10;
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
   public void render(GuiGraphics g, int mx, int my, float pt) {
      this.zones.clear();
      Theme th = Theme.current();
      Ui.rect(g, this.px, this.py, this.pw, this.ph, -15462116);
      Ui.border(g, this.px, this.py, this.pw, this.ph, th.accent);
      Ui.textShadow(g, this.getTitle().getString(), this.px + 10, this.py + 8, th.accent);
      boolean hc = Ui.hover(mx, my, this.px + this.pw - 18, this.py + 4, 14, 14);
      Ui.text(g, "✕", this.px + this.pw - 14, this.py + 7, hc ? -1185282 : -6646866);
      this.zones.add(this.px + this.pw - 18, this.py + 4, 14, 14, this::onClose);
      super.render(g, mx, my, pt);
      this.draw(g, mx, my, th);
   }

   protected abstract void draw(GuiGraphics var1, int var2, int var3, Theme var4);

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      return super.mouseClicked(event, doubleClick) ? true : this.zones.click(event.x(), event.y(), event.button());
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   protected boolean de() {
      return Ui.langShort().equals("de");
   }
}
