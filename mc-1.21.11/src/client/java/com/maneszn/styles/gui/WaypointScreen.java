package com.maneszn.styles.gui;

import com.maneszn.styles.waypoint.Waypoints;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public class WaypointScreen extends Screen {
   private final Screen parent;
   private final Ui.Zones zones = new Ui.Zones();
   private EditBox nameBox;
   private int color = Waypoints.COLORS[0];
   private double scroll;
   private int maxScroll;
   private int listY;
   private int listH;
   private final boolean quickAdd;

   public WaypointScreen(Screen parent) {
      this(parent, false);
   }

   public WaypointScreen(Screen parent, boolean quickAdd) {
      super(Component.translatable("maneszn_styles.waypoint.title"));
      this.parent = parent;
      this.quickAdd = quickAdd;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private int pw() {
      return Math.min(this.width - 20, 340);
   }

   @Override
   protected void init() {
      int pw = this.pw();
      int px = (this.width - pw) / 2;
      String old = this.nameBox == null ? Waypoints.nextName() : this.nameBox.getValue();
      this.nameBox = new EditBox(this.font, px + 8, 34, pw - 16 - 70, 14, Component.translatable("maneszn_styles.waypoint.name"));
      this.nameBox.setMaxLength(32);
      this.nameBox.setValue(old);
      this.addRenderableWidget(this.nameBox);
      if (this.quickAdd) {
         this.setFocused(this.nameBox);
         this.nameBox.setFocused(true);
      }
   }

   @Override
   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      g.fill(0, 0, this.width, this.height, -1442512118);
   }

   @Override
   public void render(GuiGraphics g, int mx, int my, float partialTick) {
      this.zones.clear();
      Theme th = Theme.current();
      int pw = this.pw();
      int px = (this.width - pw) / 2;
      int py = 10;
      int ph = this.height - 20;
      Ui.rect(g, px, py, pw, ph, -15462116);
      Ui.border(g, px, py, pw, ph, th.accent);
      Ui.textShadow(g, Ui.tr("maneszn_styles.waypoint.title"), px + 8, py + 6, -1185282);
      Ui.textRight(g, Ui.trim(Waypoints.currentWorld() + " · " + Waypoints.currentDim(), pw / 2), px + pw - 8, py + 6, -9739136);
      super.render(g, mx, my, partialTick);
      int bx = px + pw - 8 - 66;
      boolean hovAdd = Ui.hover(mx, my, bx, 33, 66, 16);
      Ui.button(g, bx, 33, 66, 16, Ui.tr("maneszn_styles.waypoint.add"), hovAdd, true, th);
      this.zones.add(bx, 33, 66, 16, this::add);
      int cy = 53;

      for (int i = 0; i < Waypoints.COLORS.length; i++) {
         int c = Waypoints.COLORS[i];
         int cx = px + 8 + i * 15;
         Ui.rect(g, cx, cy, 13, 13, c == this.color ? -1 : -13752510);
         Ui.rect(g, cx + 1, cy + 1, 11, 11, c);
         this.zones.add(cx, cy, 13, 13, () -> this.color = c);
      }

      Ui.text(
         g, Ui.trim(Ui.tr("maneszn_styles.waypoint.hint"), pw - 16 - Waypoints.COLORS.length * 15), px + 12 + Waypoints.COLORS.length * 15, cy + 3, -9739136
      );
      this.listY = cy + 20;
      this.listH = py + ph - 26 - this.listY;
      List<Waypoints.Waypoint> list = Waypoints.forWorld();
      LocalPlayer player = this.minecraft.player;
      g.enableScissor(px + 2, this.listY, px + pw - 2, this.listY + this.listH);
      int y = this.listY - (int)this.scroll;
      if (list.isEmpty()) {
         Ui.textCentered(g, Ui.tr("maneszn_styles.waypoint.empty"), px + pw / 2, this.listY + 10, -6646866);
      }

      for (Waypoints.Waypoint w : list) {
         if (y + 22 >= this.listY && y <= this.listY + this.listH) {
            boolean hov = my >= this.listY && my < this.listY + this.listH && Ui.hover(mx, my, px + 6, y, pw - 12, 22);
            Ui.rect(g, px + 6, y, pw - 12, 22, hov ? -14015424 : -14607314);
            Ui.rect(g, px + 10, y + 6, 10, 10, w.color | 0xFF000000);
            this.zones.addClipped(px + 10, y + 6, 10, 10, this.listY, this.listY + this.listH, () -> w.color = nextColor(w.color), null);
            Ui.text(g, Ui.trim(w.name, pw - 150), px + 26, y + 3, w.visible ? -1185282 : -9739136);
            String dist = "";
            if (player != null && w.dim.equals(Waypoints.currentDim())) {
               double dx = w.x + 0.5 - player.getX();
               double dz = w.z + 0.5 - player.getZ();
               double dy = w.y - player.getY();
               dist = " · " + (int)Math.sqrt(dx * dx + dy * dy + dz * dz) + " m";
            }

            Ui.text(g, Ui.trim(w.x + " " + w.y + " " + w.z + " · " + w.dim + dist, pw - 110), px + 26, y + 12, -6646866);
            int ex = px + pw - 12 - 18;
            int vx = ex - 46;
            boolean hv = Ui.hover(mx, my, vx, y + 4, 44, 14);
            boolean hx = Ui.hover(mx, my, ex, y + 4, 16, 14);
            Ui.button(g, vx, y + 4, 44, 14, Ui.tr(w.visible ? "maneszn_styles.waypoint.shown" : "maneszn_styles.waypoint.hidden"), hv, w.visible, th);
            Ui.button(g, ex, y + 4, 16, 14, "✕", hx, false, th);
            this.zones.addClipped(vx, y + 4, 44, 14, this.listY, this.listY + this.listH, () -> {
               w.visible = !w.visible;
               Waypoints.save();
            }, null);
            this.zones.addClipped(ex, y + 4, 16, 14, this.listY, this.listY + this.listH, () -> Waypoints.remove(w), null);
         }

         y += 24;
      }

      g.disableScissor();
      int content = list.size() * 24;
      this.maxScroll = Math.max(0, content - this.listH);
      this.scroll = Math.max(0.0, Math.min(this.scroll, (double)this.maxScroll));
      int dy = py + ph - 22;
      boolean hd = Ui.hover(mx, my, px + 8, dy, pw - 16, 16);
      Ui.button(g, px + 8, dy, pw - 16, 16, Ui.tr("maneszn_styles.hud.done"), hd, false, th);
      this.zones.add(px + 8, dy, pw - 16, 16, this::onClose);
   }

   private static int nextColor(int c) {
      for (int i = 0; i < Waypoints.COLORS.length; i++) {
         if (Waypoints.COLORS[i] == (c | 0xFF000000)) {
            return Waypoints.COLORS[(i + 1) % Waypoints.COLORS.length];
         }
      }

      return Waypoints.COLORS[0];
   }

   private void add() {
      if (this.minecraft.player != null) {
         Waypoints.addHere(this.nameBox.getValue(), this.color);
         this.nameBox.setValue(Waypoints.nextName());
         if (this.quickAdd) {
            this.onClose();
         }
      }
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

         Waypoints.save();
         return this.zones.click(event.x(), event.y(), event.button()) ? true : super.mouseClicked(event, doubleClick);
      }
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (mouseY >= this.listY && mouseY < this.listY + this.listH) {
         this.scroll = Math.max(0.0, Math.min((double)this.maxScroll, this.scroll - scrollY * 20.0));
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      }
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      if (!this.nameBox.isFocused() || event.key() != 257 && event.key() != 335) {
         return super.keyPressed(event);
      } else {
         this.add();
         return true;
      }
   }

   @Override
   public void onClose() {
      Waypoints.save();
      this.minecraft.setScreen(this.parent);
   }
}
