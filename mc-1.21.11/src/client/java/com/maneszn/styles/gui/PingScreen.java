package com.maneszn.styles.gui;

import com.maneszn.styles.config.ModConfig;
import com.maneszn.styles.perf.PingOptimizer;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerData.Type;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

public class PingScreen extends Screen {
   private final Screen parent;
   private final Ui.Zones zones = new Ui.Zones();
   private EditBox box;

   public PingScreen(Screen parent) {
      super(Component.translatable("maneszn_styles.ping.title"));
      this.parent = parent;
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
      String old = this.box != null ? this.box.getValue() : null;
      if (old == null) {
         ServerData cur = this.minecraft.getCurrentServer();
         old = cur != null && !cur.isLan() ? cur.ip : ModConfig.get().pingServer;
      }

      this.box = new EditBox(this.font, px + 10, 46, pw - 90, 18, Component.translatable("maneszn_styles.ping.address"));
      this.box.setMaxLength(128);
      this.box.setValue(old == null ? "" : old);
      this.addRenderableWidget(this.box);
      this.setFocused(this.box);
      this.box.setFocused(true);
   }

   @Override
   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      g.fill(0, 0, this.width, this.height, -1442512118);
   }

   @Override
   public void render(GuiGraphics g, int mx, int my, float pt) {
      this.zones.clear();
      Theme th = Theme.current();
      int pw = this.pw();
      int px = (this.width - pw) / 2;
      int py = 10;
      int ph = this.height - 20;
      Ui.rect(g, px, py, pw, ph, -15462116);
      Ui.border(g, px, py, pw, ph, th.accent);
      Ui.textShadow(g, "\ud83d\udce1 " + Ui.tr("maneszn_styles.ping.title"), px + 10, py + 8, th.accent);
      Ui.text(g, Ui.trim(Ui.tr("maneszn_styles.ping.hint"), pw - 20), px + 10, py + 22, -6646866);
      super.render(g, mx, my, pt);
      boolean busy = PingOptimizer.running();
      int bx = px + pw - 76;
      int by = 46;
      boolean hb = Ui.hover(mx, my, bx, by, 66, 18);
      Ui.button(g, bx, by, 66, 18, Ui.tr(busy ? "maneszn_styles.ping.busy" : "maneszn_styles.ping.test"), hb && !busy, !busy, th);
      if (!busy) {
         this.zones.add(bx, by, 66, 18, this::test);
      }

      int y = 72;
      String st = PingOptimizer.status();
      if (!st.isBlank()) {
         Ui.text(g, Ui.trim(st, pw - 20), px + 10, y, PingOptimizer.best() != null ? -14498466 : -6646866);
         y += 14;
      }

      List<PingOptimizer.Result> res = PingOptimizer.results();
      PingOptimizer.Result best = PingOptimizer.best();
      int maxY = py + ph - 48;

      for (PingOptimizer.Result r : res) {
         if (y + 18 > maxY) {
            break;
         }

         boolean isBest = r == best;
         boolean hov = Ui.hover(mx, my, px + 10, y, pw - 20, 18);
         Ui.rect(g, px + 10, y, pw - 20, 18, hov ? -14015424 : -14607314);
         if (isBest) {
            Ui.rect(g, px + 10, y, 2, 18, -14498466);
         }

         Ui.text(g, (isBest ? "★ " : "") + r.region(), px + 16, y + 5, isBest ? -14498466 : -1185282);
         Ui.text(g, Ui.trim(r.address(), pw - 200), px + 110, y + 5, -9739136);
         Ui.textRight(g, r.ping() + " ms", px + pw - 64, y + 5, pingColor(r.ping()));
         Ui.textRight(g, Ui.tr("maneszn_styles.ping.join"), px + pw - 16, y + 5, hov ? th.accent : -6646866);
         this.zones.add(px + 10, y, pw - 20, 18, () -> this.connect(r.address()));
         y += 20;
      }

      int dy = py + ph - 44;
      boolean can = best != null && !busy;
      boolean hj = Ui.hover(mx, my, px + 10, dy, pw - 20, 18);
      Ui.button(
         g,
         px + 10,
         dy,
         pw - 20,
         18,
         can ? Ui.tr("maneszn_styles.ping.join_best", best.region(), best.ping()) : Ui.tr("maneszn_styles.ping.join_best_none"),
         hj && can,
         can,
         th
      );
      if (can) {
         this.zones.add(px + 10, dy, pw - 20, 18, () -> this.connect(best.address()));
      }

      int cy = py + ph - 22;
      boolean hc = Ui.hover(mx, my, px + 10, cy, pw - 20, 16);
      Ui.button(g, px + 10, cy, pw - 20, 16, Ui.tr("maneszn_styles.hud.done"), hc, false, th);
      this.zones.add(px + 10, cy, pw - 20, 16, this::onClose);
   }

   private static int pingColor(long ms) {
      return ms < 60L ? -14498466 : (ms < 120L ? -7859 : (ms < 200L ? -24806 : -1096636));
   }

   private void test() {
      String v = this.box.getValue().trim();
      if (!v.isEmpty()) {
         ModConfig.get().pingServer = v;
         ModConfig.save();
         PingOptimizer.start(v, Ui.lang().startsWith("de"));
      }
   }

   private void connect(String address) {
      if (ServerAddress.isValidAddress(address)) {
         ServerData data = new ServerData(address, address, Type.OTHER);
         Screen back = new JoinMultiplayerScreen(new TitleScreen());
         if (this.minecraft.level != null) {
            this.minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
         }

         ConnectScreen.startConnecting(back, this.minecraft, ServerAddress.parseString(address), data, false, null);
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (this.box.isMouseOver(event.x(), event.y())) {
         return super.mouseClicked(event, doubleClick);
      } else {
         return this.zones.click(event.x(), event.y(), event.button()) ? true : super.mouseClicked(event, doubleClick);
      }
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }
}
